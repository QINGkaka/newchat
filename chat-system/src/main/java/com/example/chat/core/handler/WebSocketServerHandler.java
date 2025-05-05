package com.example.chat.core.handler;

import com.example.chat.model.ChatMessage;
import com.example.chat.model.User;
import com.example.chat.service.MessageService;
import com.example.chat.service.RoomService;
import com.example.chat.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final UserService userService;
    private final RoomService roomService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final Map<String, ChannelHandlerContext> userChannels;
    
    private String userId;
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            handleTextMessage(ctx, text);
        } else {
            log.warn("Unsupported frame type: {}", frame.getClass().getName());
        }
    }
    
    @SuppressWarnings("unchecked")
    private void handleTextMessage(ChannelHandlerContext ctx, String text) {
        try {
            Map<String, Object> message = objectMapper.readValue(text, Map.class);
            String type = (String) message.get("type");
            
            switch (type) {
                case "auth":
                    handleAuth(ctx, message);
                    break;
                case "message":
                    handleChatMessage(ctx, message);
                    break;
                case "join":
                    handleJoinRoom(ctx, message);
                    break;
                case "leave":
                    handleLeaveRoom(ctx, message);
                    break;
                case "getUsers":
                    handleGetUsers(ctx);
                    break;
                case "getRoomUsers":
                    handleGetRoomUsers(ctx, message);
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error processing message", e);
            sendError(ctx, "Invalid message format");
        }
    }
    
    private void handleAuth(ChannelHandlerContext ctx, Map<String, Object> message) {
        String token = (String) message.get("token");
        
        // 验证token
        String userId = userService.validateToken(token);
        if (userId != null) {
            this.userId = userId;
            userChannels.put(userId, ctx);
            
            // 更新用户在线状态
            User user = userService.getUserById(userId);
            if (user != null) {
                user.setOnline(true);
                userService.updateUser(user);
            }
            
            // 发送认证成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("type", "auth");
            response.put("success", true);
            response.put("userId", userId);
            
            sendMessage(ctx, response);
            log.info("User authenticated: {}", userId);
        } else {
            sendError(ctx, "Authentication failed");
            ctx.close();
        }
    }
    
    private void handleChatMessage(ChannelHandlerContext ctx, Map<String, Object> message) {
        if (userId == null) {
            sendError(ctx, "Not authenticated");
            return;
        }
        
        String content = (String) message.get("content");
        String type = (String) message.get("type");
        String roomId = (String) message.get("roomId");
        String receiverId = (String) message.get("receiverId");
        
        // 创建消息对象
        ChatMessage chatMessage = ChatMessage.builder()
                .senderId(userId)
                .content(content)
                .roomId(roomId)
                .receiverId(receiverId)
                .timestamp(System.currentTimeMillis())
                .type("image".equalsIgnoreCase(type) ? ChatMessage.MessageType.IMAGE : ChatMessage.MessageType.TEXT)
                .build();
        
        // 保存并发送消息
        messageService.sendMessage(chatMessage);
        
        // 发送确认
        Map<String, Object> response = new HashMap<>();
        response.put("type", "messageAck");
        response.put("messageId", chatMessage.getId());
        response.put("success", true);
        
        sendMessage(ctx, response);
    }
    
    private void handleJoinRoom(ChannelHandlerContext ctx, Map<String, Object> message) {
        if (userId == null) {
            sendError(ctx, "Not authenticated");
            return;
        }
        
        String roomId = (String) message.get("roomId");
        
        // 加入房间
        roomService.addUserToRoom(roomId, userId);
        
        // 发送系统消息
        ChatMessage systemMessage = ChatMessage.builder()
                .senderId("system")
                .content(userService.getUserById(userId).getUsername() + " joined the room")
                .roomId(roomId)
                .timestamp(System.currentTimeMillis())
                .type(ChatMessage.MessageType.SYSTEM)
                .build();
        
        messageService.sendMessage(systemMessage);
        
        // 发送确认
        Map<String, Object> response = new HashMap<>();
        response.put("type", "joinAck");
        response.put("roomId", roomId);
        response.put("success", true);
        
        sendMessage(ctx, response);
    }
    
    private void handleLeaveRoom(ChannelHandlerContext ctx, Map<String, Object> message) {
        if (userId == null) {
            sendError(ctx, "Not authenticated");
            return;
        }
        
        String roomId = (String) message.get("roomId");
        
        // 离开房间
        roomService.removeUserFromRoom(roomId, userId);
        
        // 发送系统消息
        ChatMessage systemMessage = ChatMessage.builder()
                .senderId("system")
                .content(userService.getUserById(userId).getUsername() + " left the room")
                .roomId(roomId)
                .timestamp(System.currentTimeMillis())
                .type(ChatMessage.MessageType.SYSTEM)
                .build();
        
        messageService.sendMessage(systemMessage);
        
        // 发送确认
        Map<String, Object> response = new HashMap<>();
        response.put("type", "leaveAck");
        response.put("roomId", roomId);
        response.put("success", true);
        
        sendMessage(ctx, response);
    }
    
    private void handleGetUsers(ChannelHandlerContext ctx) {
        if (userId == null) {
            sendError(ctx, "Not authenticated");
            return;
        }
        
        List<User> users = userService.getAllUsers();
        List<Map<String, Object>> userList = users.stream()
                .map(this::convertUserToMap)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "users");
        response.put("users", userList);
        
        sendMessage(ctx, response);
    }
    
    private void handleGetRoomUsers(ChannelHandlerContext ctx, Map<String, Object> message) {
        if (userId == null) {
            sendError(ctx, "Not authenticated");
            return;
        }
        
        String roomId = (String) message.get("roomId");
        List<String> userIds = roomService.getRoomMembers(roomId);
        
        List<Map<String, Object>> userList = userIds.stream()
                .map(userService::getUserById)
                .filter(Objects::nonNull)
                .map(this::convertUserToMap)
                .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "roomUsers");
        response.put("roomId", roomId);
        response.put("users", userList);
        
        sendMessage(ctx, response);
    }
    
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("online", user.isOnline());
        userMap.put("fullName", user.getFullName());
        userMap.put("profilePicture", user.getProfilePicture());
        return userMap;
    }
    
    private void sendMessage(ChannelHandlerContext ctx, Map<String, Object> response) {
        try {
            String jsonResponse = objectMapper.writeValueAsString(response);
            ctx.writeAndFlush(new TextWebSocketFrame(jsonResponse));
        } catch (Exception e) {
            log.error("Error sending message", e);
        }
    }
    
    private void sendError(ChannelHandlerContext ctx, String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", "error");
        errorResponse.put("message", errorMessage);
        sendMessage(ctx, errorResponse);
    }
}



