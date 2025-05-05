package com.example.chat.util;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

/**
 * Channel工具类，用于管理Channel的属性
 */
public class ChannelUtil {
    
    private static final AttributeKey<String> USER_ID_KEY = AttributeKey.valueOf("userId");
    private static final AttributeKey<String> ROOM_ID_KEY = AttributeKey.valueOf("roomId");
    
    /**
     * 设置Channel关联的用户ID
     */
    public static void setUserId(Channel channel, String userId) {
        channel.attr(USER_ID_KEY).set(userId);
    }
    
    /**
     * 获取Channel关联的用户ID
     */
    public static String getUserId(Channel channel) {
        return channel.attr(USER_ID_KEY).get();
    }
    
    /**
     * 设置Channel关联的房间ID
     */
    public static void setRoomId(Channel channel, String roomId) {
        channel.attr(ROOM_ID_KEY).set(roomId);
    }
    
    /**
     * 获取Channel关联的房间ID
     */
    public static String getRoomId(Channel channel) {
        return channel.attr(ROOM_ID_KEY).get();
    }
    
    /**
     * 清除Channel的所有属性
     */
    public static void clearAttributes(Channel channel) {
        channel.attr(USER_ID_KEY).set(null);
        channel.attr(ROOM_ID_KEY).set(null);
    }
}
