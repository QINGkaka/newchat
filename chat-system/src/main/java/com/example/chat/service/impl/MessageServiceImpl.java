package com.example.chat.service.impl;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {
    
    // 模拟消息存储
    private final Map<String, ChatMessage> messages = new ConcurrentHashMap<>();
    private final Map<String, List<String>> roomMessages = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userMessages = new ConcurrentHashMap<>();
    private final Map<String, List<String>> privateMessages = new ConcurrentHashMap<>();
    
    @Override
    public void saveMessage(ChatMessage message) {
        // 生成消息ID
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        
        // 设置时间戳
        if (message.getTimestamp() == 0) {
            message.setTimestamp(System.currentTimeMillis());
        }
        
        // 存储消息
        messages.put(message.getId(), message);
        
        // 更新索引
        String senderId = message.getSenderId();
        userMessages.computeIfAbsent(senderId, k -> new ArrayList<>()).add(message.getId());
        
        if (message.isPrivate()) {
            // 私聊消息
            String receiverId = message.getReceiverId();
            userMessages.computeIfAbsent(receiverId, k -> new ArrayList<>()).add(message.getId());
            
            String chatKey = getChatKey(senderId, receiverId);
            privateMessages.computeIfAbsent(chatKey, k -> new ArrayList<>()).add(message.getId());
        } else if (message.getRoomId() != null) {
            // 群聊消息
            roomMessages.computeIfAbsent(message.getRoomId(), k -> new ArrayList<>()).add(message.getId());
        }
    }
    
    @Override
    public ChatMessage sendMessage(ChatMessage message) {
        saveMessage(message);
        return message;
    }
    
    @Override
    public ChatMessage getMessageById(String messageId) {
        return messages.get(messageId);
    }
    
    @Override
    public List<ChatMessage> getUserMessages(String userId) {
        List<String> messageIds = userMessages.getOrDefault(userId, new ArrayList<>());
        
        return messageIds.stream()
                .map(messages::get)
                .filter(msg -> msg != null)
                .sorted(Comparator.comparingLong(ChatMessage::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ChatMessage> getUserMessages(String userId, String otherUserId, int limit) {
        return getMessagesBetweenUsers(userId, otherUserId, limit);
    }
    
    @Override
    public List<ChatMessage> getRoomMessages(String roomId) {
        return getRoomMessages(roomId, Integer.MAX_VALUE);
    }
    
    @Override
    public List<ChatMessage> getRoomMessages(String roomId, int limit) {
        List<String> messageIds = roomMessages.getOrDefault(roomId, new ArrayList<>());
        
        return messageIds.stream()
                .map(messages::get)
                .filter(msg -> msg != null)
                .sorted(Comparator.comparingLong(ChatMessage::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ChatMessage> getMessagesBetweenUsers(String userId1, String userId2) {
        return getMessagesBetweenUsers(userId1, userId2, Integer.MAX_VALUE);
    }
    
    @Override
    public List<ChatMessage> getMessagesBetweenUsers(String userId1, String userId2, int limit) {
        String chatKey = getChatKey(userId1, userId2);
        List<String> messageIds = privateMessages.getOrDefault(chatKey, new ArrayList<>());
        
        return messageIds.stream()
                .map(messages::get)
                .filter(msg -> msg != null)
                .sorted(Comparator.comparingLong(ChatMessage::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteMessage(String messageId) {
        ChatMessage message = messages.remove(messageId);
        if (message != null) {
            // 从索引中移除
            if (message.getSenderId() != null) {
                List<String> senderMsgs = userMessages.get(message.getSenderId());
                if (senderMsgs != null) {
                    senderMsgs.remove(messageId);
                }
            }
            
            if (message.getReceiverId() != null) {
                List<String> receiverMsgs = userMessages.get(message.getReceiverId());
                if (receiverMsgs != null) {
                    receiverMsgs.remove(messageId);
                }
                
                String chatKey = getChatKey(message.getSenderId(), message.getReceiverId());
                List<String> privateMsgs = privateMessages.get(chatKey);
                if (privateMsgs != null) {
                    privateMsgs.remove(messageId);
                }
            }
            
            if (message.getRoomId() != null) {
                List<String> roomMsgs = roomMessages.get(message.getRoomId());
                if (roomMsgs != null) {
                    roomMsgs.remove(messageId);
                }
            }
            
            return true;
        }
        return false;
    }
    
    @Override
    public boolean markAsRead(String messageId, String userId) {
        ChatMessage message = messages.get(messageId);
        if (message != null) {
            message.markAsRead(userId);
            return true;
        }
        return false;
    }
    
    @Override
    public int getUnreadCount(String userId) {
        return (int) messages.values().stream()
                .filter(msg -> !msg.isRead(userId) && 
                        (msg.getReceiverId() != null && msg.getReceiverId().equals(userId)))
                .count();
    }
    
    /**
     * 获取私聊的唯一键
     */
    private String getChatKey(String userId1, String userId2) {
        // 确保键的一致性，无论用户ID的顺序如何
        return userId1.compareTo(userId2) < 0 
                ? userId1 + ":" + userId2 
                : userId2 + ":" + userId1;
    }
}


