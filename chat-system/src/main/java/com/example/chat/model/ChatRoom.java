package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    private String id;
    private String name;
    private String creatorId;
    private long createdAt;
    
    @Builder.Default
    private List<String> userIds = new ArrayList<>();
    
    private boolean isPrivate;
    private String description;
    
    // 添加用户到聊天室
    public boolean addUser(String userId) {
        if (!userIds.contains(userId)) {
            return userIds.add(userId);
        }
        return false;
    }
    
    // 从聊天室移除用户
    public boolean removeUser(String userId) {
        return userIds.remove(userId);
    }
    
    // 检查用户是否在聊天室中
    public boolean hasUser(String userId) {
        return userIds.contains(userId);
    }
}
