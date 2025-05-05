package com.example.chat.service.impl;

import com.example.chat.model.ChatRoom;
import com.example.chat.service.RoomService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {
    
    // 模拟聊天室存储
    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    
    @Override
    public ChatRoom createRoom(String name, String creatorId) {
        String roomId = UUID.randomUUID().toString();
        
        ChatRoom room = ChatRoom.builder()
                .id(roomId)
                .name(name)
                .creatorId(creatorId)
                .createdAt(System.currentTimeMillis())
                .build();
        
        // 创建者自动加入聊天室
        room.addUser(creatorId);
        
        rooms.put(roomId, room);
        return room;
    }
    
    @Override
    public ChatRoom getRoomById(String roomId) {
        return rooms.get(roomId);
    }
    
    @Override
    public List<ChatRoom> getUserRooms(String userId) {
        return rooms.values().stream()
                .filter(room -> room.getUserIds().contains(userId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ChatRoom> getAllRooms() {
        return new ArrayList<>(rooms.values());
    }
    
    @Override
    public boolean addUserToRoom(String roomId, String userId) {
        ChatRoom room = rooms.get(roomId);
        if (room != null) {
            boolean added = room.addUser(userId);
            if (added) {
                rooms.put(roomId, room);
            }
            return added;
        }
        return false;
    }
    
    @Override
    public boolean removeUserFromRoom(String roomId, String userId) {
        ChatRoom room = rooms.get(roomId);
        if (room != null) {
            boolean removed = room.removeUser(userId);
            if (removed) {
                rooms.put(roomId, room);
            }
            return removed;
        }
        return false;
    }
    
    @Override
    public boolean isUserInRoom(String roomId, String userId) {
        ChatRoom room = rooms.get(roomId);
        return room != null && room.hasUser(userId);
    }
    
    @Override
    public List<String> getRoomUsers(String roomId) {
        ChatRoom room = rooms.get(roomId);
        if (room != null) {
            return new ArrayList<>(room.getUserIds());
        }
        return new ArrayList<>();
    }
    
    @Override
    public void updateRoom(ChatRoom room) {
        if (room != null && room.getId() != null) {
            rooms.put(room.getId(), room);
        }
    }
    
    @Override
    public boolean deleteRoom(String roomId) {
        return rooms.remove(roomId) != null;
    }
    
    // 初始化一些测试聊天室
    public void initTestRooms() {
        // 创建一个公共聊天室
        ChatRoom publicRoom = ChatRoom.builder()
                .id("public")
                .name("Public Chat")
                .creatorId("system")
                .createdAt(System.currentTimeMillis())
                .isPrivate(false)
                .description("Public chat room for all users")
                .build();
        
        rooms.put(publicRoom.getId(), publicRoom);
    }
}















