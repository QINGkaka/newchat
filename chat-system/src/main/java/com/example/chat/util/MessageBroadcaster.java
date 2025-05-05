package com.example.chat.util;

import com.example.chat.protocol.ProtocolMessage;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MessageBroadcaster {
    private static final ChannelGroup GLOBAL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Map<String, ChannelGroup> ROOM_GROUPS = new ConcurrentHashMap<>();

    public static void addChannel(Channel channel) {
        GLOBAL_GROUP.add(channel);
    }

    public static void removeChannel(Channel channel) {
        GLOBAL_GROUP.remove(channel);
        String userId = ChannelUtil.getUserId(channel);
        if (userId != null) {
            ROOM_GROUPS.values().forEach(group -> group.remove(channel));
        }
    }

    public static void addToRoom(String roomId, Channel channel) {
        ROOM_GROUPS.computeIfAbsent(roomId, 
            k -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)).add(channel);
    }

    public static void removeFromRoom(String roomId, Channel channel) {
        ChannelGroup group = ROOM_GROUPS.get(roomId);
        if (group != null) {
            group.remove(channel);
            if (group.isEmpty()) {
                ROOM_GROUPS.remove(roomId);
            }
        }
    }

    public static void broadcastToRoom(String roomId, ProtocolMessage ProtocolMessage) {
        ChannelGroup group = ROOM_GROUPS.get(roomId);
        if (group != null) {
            group.writeAndFlush(ProtocolMessage);
            log.debug("Broadcasted ProtocolMessage to room {}: {}", roomId, ProtocolMessage);
        }
    }

    public static void broadcastToAll(ProtocolMessage ProtocolMessage) {
        GLOBAL_GROUP.writeAndFlush(ProtocolMessage);
        log.debug("Broadcasted ProtocolMessage to all users: {}", ProtocolMessage);
    }
}
