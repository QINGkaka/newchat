package com.example.chat.dao.impl;

import com.example.chat.dao.MessageDao;
import com.example.chat.model.ChatMessage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryMessageDao implements MessageDao {
    
    private final Map<String, ChatMessage> messages = new ConcurrentHashMap<>();
    
    @Override
    public ChatMessage save(ChatMessage message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        messages.put(message.getId(), message);
        return message;
    }
    
    @Override
    public ChatMessage findById(String id) {
        return messages.get(id);
    }
    
    @Override
    public List<ChatMessage> findByRoomId(String roomId) {
        return messages.values().stream()
                .filter(message -> roomId.equals(message.getRoomId()))
                .sorted((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ChatMessage> findBetweenUsers(String userId1, String userId2, int limit, long offset) {
        return messages.values().stream()
                .filter(message -> message.getRoomId() == null) // 只查找私聊消息
                .filter(message -> 
                    (userId1.equals(message.getSenderId()) && userId2.equals(message.getReceiverId())) ||
                    (userId2.equals(message.getSenderId()) && userId1.equals(message.getReceiverId()))
                )
                .sorted((m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean delete(String id) {
        return messages.remove(id) != null;
    }
}