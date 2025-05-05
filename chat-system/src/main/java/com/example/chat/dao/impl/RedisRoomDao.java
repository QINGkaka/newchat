package com.example.chat.dao.impl;

import com.example.chat.dao.RoomDao;
import com.example.chat.model.Room;
import com.example.chat.util.JsonUtil;
import com.example.chat.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class RedisRoomDao implements RoomDao {
    
    private static final String ROOM_KEY_PREFIX = "chat:room:";
    private static final String ROOM_MEMBERS_KEY_PREFIX = "chat:room:members:";
    private static final String USER_ROOMS_KEY_PREFIX = "chat:user:rooms:";
    private static final String ALL_ROOMS_KEY = "chat:rooms:all";
    
    @Override
    public Room save(Room room) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = ROOM_KEY_PREFIX + room.getId();
            jedis.set(key, JsonUtil.toJson(room));
            
            // 添加到所有房间集合
            jedis.sadd(ALL_ROOMS_KEY, room.getId());
        }
        return room;
    }
    
    @Override
    public Room findById(String roomId) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = ROOM_KEY_PREFIX + roomId;
            String json = jedis.get(key);
            return json != null ? JsonUtil.fromJson(json, Room.class) : null;
        }
    }
    
    @Override
    public List<Room> findAll() {
        try (Jedis jedis = RedisUtil.getJedis()) {
            Set<String> roomIds = jedis.smembers(ALL_ROOMS_KEY);
            List<Room> rooms = new ArrayList<>();
            
            for (String roomId : roomIds) {
                Room room = findById(roomId);
                if (room != null) {
                    rooms.add(room);
                }
            }
            
            return rooms;
        }
    }
    
    @Override
    public void delete(String roomId) {
        deleteRoom(roomId);
    }
    
    @Override
    public void deleteRoom(String roomId) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            // 删除房间信息
            String key = ROOM_KEY_PREFIX + roomId;
            jedis.del(key);
            
            // 从所有房间集合中移除
            jedis.srem(ALL_ROOMS_KEY, roomId);
            
            // 获取房间成员
            String membersKey = ROOM_MEMBERS_KEY_PREFIX + roomId;
            Set<String> members = jedis.smembers(membersKey);
            
            // 从每个成员的房间列表中移除该房间
            for (String userId : members) {
                String userRoomsKey = USER_ROOMS_KEY_PREFIX + userId;
                jedis.srem(userRoomsKey, roomId);
            }
            
            // 删除房间成员列表
            jedis.del(membersKey);
        }
    }
    
    @Override
    public void updateRoom(Room room) {
        save(room);
    }
    
    @Override
    public void addUserToRoom(String roomId, String userId) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            // 添加用户到房间成员列表
            String membersKey = ROOM_MEMBERS_KEY_PREFIX + roomId;
            jedis.sadd(membersKey, userId);
            
            // 添加房间到用户的房间列表
            String userRoomsKey = USER_ROOMS_KEY_PREFIX + userId;
            jedis.sadd(userRoomsKey, roomId);
            
            // 更新房间信息
            Room room = findById(roomId);
            if (room != null) {
                room.getMembers().add(userId);
                save(room);
            }
        }
    }
    
    @Override
    public void removeUserFromRoom(String roomId, String userId) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            // 从房间成员列表中移除用户
            String membersKey = ROOM_MEMBERS_KEY_PREFIX + roomId;
            jedis.srem(membersKey, userId);
            
            // 从用户的房间列表中移除房间
            String userRoomsKey = USER_ROOMS_KEY_PREFIX + userId;
            jedis.srem(userRoomsKey, roomId);
            
            // 更新房间信息
            Room room = findById(roomId);
            if (room != null) {
                room.getMembers().remove(userId);
                save(room);
            }
        }
    }
    
    @Override
    public Set<String> getRoomMembers(String roomId) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String membersKey = ROOM_MEMBERS_KEY_PREFIX + roomId;
            return jedis.smembers(membersKey);
        }
    }
    
    @Override
    public List<String> getUserRooms(String userId) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String userRoomsKey = USER_ROOMS_KEY_PREFIX + userId;
            Set<String> roomIds = jedis.smembers(userRoomsKey);
            return new ArrayList<>(roomIds);
        }
    }
    
    @Override
    public List<Room> findByUserId(String userId) {
        List<String> roomIds = getUserRooms(userId);
        return roomIds.stream()
                .map(this::findById)
                .filter(room -> room != null)
                .collect(Collectors.toList());
    }
}
