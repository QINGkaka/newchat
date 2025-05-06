package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String id;
    private String senderId;
    private String receiverId;
    private String roomId;
    private String content;
    private String image;
    private long timestamp;
    private MessageType type;
    
    // 已读用户集合
    @Builder.Default
    private Set<String> readBy = new HashSet<>();
    
    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        SYSTEM
    }
    
    /**
     * 判断消息是否为私聊消息
     */
    public boolean isPrivate() {
        return receiverId != null && !receiverId.isEmpty();
    }
    
    /**
     * 标记消息为指定用户已读
     */
    public void markAsRead(String userId) {
        if (readBy == null) {
            readBy = new HashSet<>();
        }
        readBy.add(userId);
    }
    
    /**
     * 检查消息是否被指定用户已读
     */
    public boolean isRead(String userId) {
        return readBy != null && readBy.contains(userId);
    }
}
