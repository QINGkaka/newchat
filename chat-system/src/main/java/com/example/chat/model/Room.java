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
public class Room {
    private String id;
    private String name;
    private String creatorId;
    private long createTime;
    
    @Builder.Default
    private Set<String> members = new HashSet<>();
    
    public boolean addMember(String userId) {
        return members.add(userId);
    }
    
    public boolean removeMember(String userId) {
        return members.remove(userId);
    }
    
    public boolean hasMember(String userId) {
        return members.contains(userId);
    }
}

