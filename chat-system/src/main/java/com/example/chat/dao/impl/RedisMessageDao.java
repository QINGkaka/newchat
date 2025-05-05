package com.example.chat.dao.impl;

import com.example.chat.dao.MessageDao;
import com.example.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisMessageDao implements MessageDao {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String MESSAGE_KEY = "message:";
    private static final String ROOM_MESSAGES_KEY = "room:messages:";
    private static final String USER_MESSAGES_KEY = "user:messages:";
    private static final String PRIVATE_MESSAGES_KEY = "private:messages:";
    
    @Override
    public ChatMessage save(ChatMessage message) {
        try {
            // 将消息对象转换为JSON字符串
            String messageJson = objectMapper.writeValueAsString(message);
            
            // 保存消息
            String messageKey = MESSAGE_KEY + message.getId();
            redisTemplate.opsForValue().set(messageKey, messageJson);
            
            // 如果是房间消息，添加到房间消息集合
            if (message.getRoomId() != null && !message.getRoomId().isEmpty()) {
                String roomMessagesKey = ROOM_MESSAGES_KEY + message.getRoomId();
                redisTemplate.opsForZSet().add(roomMessagesKey, message.getId(), message.getTimestamp());
            }
            
            // 添加到发送者的消息集合
            String senderMessagesKey = USER_MESSAGES_KEY + message.getSenderId();
            redisTemplate.opsForZSet().add(senderMessagesKey, message.getId(), message.getTimestamp());
            
            // 如果是私聊消息，添加到接收者的消息集合
            if (message.getReceiverId() != null && !message.getReceiverId().isEmpty()) {
                String receiverMessagesKey = USER_MESSAGES_KEY + message.getReceiverId();
                redisTemplate.opsForZSet().add(receiverMessagesKey, message.getId(), message.getTimestamp());
                
                // 添加到私聊消息集合
                String privateMessagesKey = getPrivateMessagesKey(message.getSenderId(), message.getReceiverId());
                redisTemplate.opsForZSet().add(privateMessagesKey, message.getId(), message.getTimestamp());
            }
            
            return message;
        } catch (Exception e) {
            log.error("Error saving message", e);
            return null;
        }
    }
    
    @Override
    public ChatMessage findById(String messageId) {
        try {
            String messageKey = MESSAGE_KEY + messageId;
            String messageJson = redisTemplate.opsForValue().get(messageKey);
            
            if (messageJson != null) {
                return objectMapper.readValue(messageJson, ChatMessage.class);
            }
            
            return null;
        } catch (Exception e) {
            log.error("Error getting message by ID", e);
            return null;
        }
    }
    
    @Override
    public boolean delete(String messageId) {
        try {
            // 获取消息
            ChatMessage message = findById(messageId);
            if (message == null) {
                return false;
            }
            
            // 删除消息
            String messageKey = MESSAGE_KEY + messageId;
            redisTemplate.delete(messageKey);
            
            // 从房间消息集合中删除
            if (message.getRoomId() != null && !message.getRoomId().isEmpty()) {
                String roomMessagesKey = ROOM_MESSAGES_KEY + message.getRoomId();
                redisTemplate.opsForZSet().remove(roomMessagesKey, messageId);
            }
            
            // 从发送者的消息集合中删除
            String senderMessagesKey = USER_MESSAGES_KEY + message.getSenderId();
            redisTemplate.opsForZSet().remove(senderMessagesKey, messageId);
            
            // 从接收者的消息集合中删除
            if (message.getReceiverId() != null && !message.getReceiverId().isEmpty()) {
                String receiverMessagesKey = USER_MESSAGES_KEY + message.getReceiverId();
                redisTemplate.opsForZSet().remove(receiverMessagesKey, messageId);
                
                // 从私聊消息集合中删除
                String privateMessagesKey = getPrivateMessagesKey(message.getSenderId(), message.getReceiverId());
                redisTemplate.opsForZSet().remove(privateMessagesKey, messageId);
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error deleting message", e);
            return false;
        }
    }
    
    @Override
    public List<ChatMessage> findByRoomId(String roomId) {
        return findByRoomId(roomId, 100, 0);
    }
    
    // 辅助方法，用于分页获取房间消息
    public List<ChatMessage> findByRoomId(String roomId, int limit, long before) {
        try {
            String roomMessagesKey = ROOM_MESSAGES_KEY + roomId;
            
            // 获取指定时间戳之前的消息ID
            Set<String> messageIds;
            if (before > 0) {
                messageIds = redisTemplate.opsForZSet().reverseRangeByScore(
                        roomMessagesKey, 0, before, 0, limit);
            } else {
                messageIds = redisTemplate.opsForZSet().reverseRange(roomMessagesKey, 0, limit - 1);
            }
            
            if (messageIds == null || messageIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 获取消息详情
            List<ChatMessage> messages = new ArrayList<>();
            for (String messageId : messageIds) {
                ChatMessage message = findById(messageId);
                if (message != null) {
                    messages.add(message);
                }
            }
            
            return messages;
        } catch (Exception e) {
            log.error("Error getting messages by room ID", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<ChatMessage> findBetweenUsers(String userId1, String userId2, int limit, long before) {
        try {
            String privateMessagesKey = getPrivateMessagesKey(userId1, userId2);
            
            // 获取指定时间戳之前的消息ID
            Set<String> messageIds;
            if (before > 0) {
                messageIds = redisTemplate.opsForZSet().reverseRangeByScore(
                        privateMessagesKey, 0, before, 0, limit);
            } else {
                messageIds = redisTemplate.opsForZSet().reverseRange(privateMessagesKey, 0, limit - 1);
            }
            
            if (messageIds == null || messageIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 获取消息详情
            List<ChatMessage> messages = new ArrayList<>();
            for (String messageId : messageIds) {
                ChatMessage message = findById(messageId);
                if (message != null) {
                    messages.add(message);
                }
            }
            
            return messages;
        } catch (Exception e) {
            log.error("Error getting messages between users", e);
            return Collections.emptyList();
        }
    }
    
    // 辅助方法，不是接口的一部分
    public List<ChatMessage> findByUserId(String userId) {
        try {
            String userMessagesKey = USER_MESSAGES_KEY + userId;
            
            // 获取用户的所有消息ID
            Set<String> messageIds = redisTemplate.opsForZSet().reverseRange(userMessagesKey, 0, -1);
            
            if (messageIds == null || messageIds.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 获取消息详情
            List<ChatMessage> messages = new ArrayList<>();
            for (String messageId : messageIds) {
                ChatMessage message = findById(messageId);
                if (message != null) {
                    messages.add(message);
                }
            }
            
            return messages;
        } catch (Exception e) {
            log.error("Error getting messages by user ID", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取私聊消息的键
     */
    private String getPrivateMessagesKey(String userId1, String userId2) {
        // 确保用户ID按字典序排序，保证两个用户之间的私聊消息使用相同的键
        if (userId1.compareTo(userId2) < 0) {
            return PRIVATE_MESSAGES_KEY + userId1 + ":" + userId2;
        } else {
            return PRIVATE_MESSAGES_KEY + userId2 + ":" + userId1;
        }
    }
}
