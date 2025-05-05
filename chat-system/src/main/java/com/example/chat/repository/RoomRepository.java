package com.example.chat.repository;

import com.example.chat.model.Room;
import java.util.List;

/**
 * 房间数据访问接口
 */
public interface RoomRepository {
    
    /**
     * 保存房间信息
     */
    Room save(Room room);
    
    /**
     * 根据ID查找房间
     */
    Room findById(String roomId);
    
    /**
     * 获取所有房间
     */
    List<Room> findAll();
    
    /**
     * 根据成员ID查找房间
     */
    List<Room> findByMembersContaining(String userId);
    
    /**
     * 删除房间
     */
    void delete(String roomId);
}