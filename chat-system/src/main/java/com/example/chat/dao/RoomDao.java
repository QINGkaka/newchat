package com.example.chat.dao;

import com.example.chat.model.Room;
import java.util.List;
import java.util.Set;

public interface RoomDao {
    Room save(Room room);
    Room findById(String roomId);
    List<Room> findAll();
    void delete(String roomId);
    void deleteRoom(String roomId);
    void updateRoom(Room room);
    void addUserToRoom(String roomId, String userId);
    void removeUserFromRoom(String roomId, String userId);
    Set<String> getRoomMembers(String roomId);
    List<String> getUserRooms(String userId);
    List<Room> findByUserId(String userId);
}
