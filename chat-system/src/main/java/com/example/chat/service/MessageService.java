package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import java.util.List;

public interface MessageService {
    
    /**
     * 保存消息
     */
    void saveMessage(ChatMessage message);
    
    /**
     * 发送消息
     */
    ChatMessage sendMessage(ChatMessage message);
    
    /**
     * 根据ID获取消息
     */
    ChatMessage getMessageById(String messageId);
    
    /**
     * 获取用户的所有消息
     */
    List<ChatMessage> getUserMessages(String userId);
    
    /**
     * 获取用户与特定用户之间的消息，带限制
     */
    List<ChatMessage> getUserMessages(String userId, String otherUserId, int limit);
    
    /**
     * 获取聊天室的消息
     */
    List<ChatMessage> getRoomMessages(String roomId);
    
    /**
     * 获取聊天室的消息，带限制
     */
    List<ChatMessage> getRoomMessages(String roomId, int limit);
    
    /**
     * 获取两个用户之间的消息
     */
    List<ChatMessage> getMessagesBetweenUsers(String userId1, String userId2);
    
    /**
     * 获取两个用户之间的消息，带限制
     */
    List<ChatMessage> getMessagesBetweenUsers(String userId1, String userId2, int limit);
    
    /**
     * 删除消息
     */
    boolean deleteMessage(String messageId);
    
    /**
     * 标记消息为已读
     */
    boolean markAsRead(String messageId, String userId);
    
    /**
     * 获取用户的未读消息数
     */
    int getUnreadCount(String userId);
    
    /**
     * 获取消息（兼容旧方法）
     */
    default ChatMessage getMessage(String messageId) {
        return getMessageById(messageId);
    }
}




