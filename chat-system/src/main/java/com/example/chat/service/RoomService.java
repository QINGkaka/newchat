package com.example.chat.service;

import com.example.chat.model.ChatRoom;
import java.util.List;

/**
 * 房间服务接口
 */
public interface RoomService {
    
    // 创建聊天室
    ChatRoom createRoom(String name, String creatorId);
    
    // 获取聊天室
    ChatRoom getRoomById(String roomId);
    
    // 获取用户的所有聊天室
    List<ChatRoom> getUserRooms(String userId);
    
    // 获取所有聊天室
    List<ChatRoom> getAllRooms();
    
    // 添加用户到聊天室
    boolean addUserToRoom(String roomId, String userId);
    
    // 从聊天室移除用户
    boolean removeUserFromRoom(String roomId, String userId);
    
    // 检查用户是否在聊天室中
    boolean isUserInRoom(String roomId, String userId);
    
    // 获取聊天室中的所有用户
    List<String> getRoomUsers(String roomId);
    
    // 获取聊天室成员（兼容旧方法名）
    default List<String> getRoomMembers(String roomId) {
        return getRoomUsers(roomId);
    }
    
    // 获取聊天室用户（兼容旧方法名）
    default List<String> getUsersInRoom(String roomId) {
        return getRoomUsers(roomId);
    }
    
    // 更新聊天室信息
    void updateRoom(ChatRoom room);
    
    // 删除聊天室
    boolean deleteRoom(String roomId);
}
