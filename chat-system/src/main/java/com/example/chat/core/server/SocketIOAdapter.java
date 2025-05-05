package com.example.chat.core.server;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.PingListener;
import com.example.chat.model.ChatMessage;
import com.example.chat.model.User;
import com.example.chat.service.MessageService;
import com.example.chat.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SocketIOAdapter {
    
    private final SocketIOServer server;
    private final UserService userService;
    private final MessageService messageService;
    
    // 存储用户ID与客户端的映射关系
    private final Map<String, SocketIOClient> userClients = new ConcurrentHashMap<>();
    
    // 重试次数和间隔
    private static final int MAX_RETRY_COUNT = 3;
    private static final long RETRY_INTERVAL = 5000; // 5秒
    
    @Autowired
    public SocketIOAdapter(SocketIOServer server, UserService userService, MessageService messageService) {
        this.server = server;
        this.userService = userService;
        this.messageService = messageService;
        
        // 注册事件监听器
        this.server.addConnectListener(onConnected());
        this.server.addDisconnectListener(onDisconnected());
        this.server.addPingListener(onPing());
        
        // 注册消息处理事件
        this.server.addEventListener("sendMessage", Map.class, 
            (client, data, ack) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> messageData = (Map<String, Object>) data;
                handleChatMessage(client, messageData, ack);
            });
            
        this.server.addEventListener("getOnlineUsers", Object.class, 
            (client, data, ack) -> handleGetOnlineUsers(client, data, ack));
        
        // 注册房间相关事件
        this.server.addEventListener("joinRoom", Map.class, 
            (client, data, ack) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> roomData = (Map<String, Object>) data;
                handleJoinRoom(client, roomData, ack);
            });
        this.server.addEventListener("leaveRoom", Map.class, 
            (client, data, ack) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> roomData = (Map<String, Object>) data;
                handleLeaveRoom(client, roomData, ack);
            });
    }
    
    @PostConstruct
    public void start() {
        try {
            // 启动服务器
            server.start();
            log.info("Socket.IO server started on {}:{}", 
                    server.getConfiguration().getHostname(), 
                    server.getConfiguration().getPort());
        } catch (Exception e) {
            log.error("Failed to start Socket.IO server", e);
            // 尝试重新启动
            scheduleRestart();
        }
    }
    
    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop();
            log.info("Socket.IO server stopped");
        }
    }
    
    private PingListener onPing() {
        return client -> {
            String userId = client.get("userId");
            if (userId != null) {
                log.debug("Ping received from user: {}", userId);
            }
        };
    }
    
    private ConnectListener onConnected() {
        return client -> {
            // 获取认证信息
            String token = null;
            String authHeader = client.getHandshakeData().getHttpHeaders().get("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            } else {
                token = client.getHandshakeData().getSingleUrlParam("token");
            }
            
            if (token == null) {
                log.warn("No token provided for client: {}", client.getSessionId());
                client.disconnect();
                return;
            }
            
            // 验证token并获取用户ID
            String userId = userService.validateToken(token);
            if (userId != null) {
                // 将用户ID与客户端关联
                client.set("userId", userId);
                userClients.put(userId, client);
                
                // 更新用户在线状态
                User user = userService.getUserById(userId);
                if (user != null) {
                    user.setOnline(true);
                    userService.updateUser(user);
                }
                
                // 广播用户上线状态
                broadcastUserStatus(userId, true);
                
                log.info("Client connected: {}, userId: {}", client.getSessionId(), userId);
            } else {
                // 认证失败，断开连接
                client.disconnect();
                log.warn("Authentication failed for client: {}", client.getSessionId());
            }
        };
    }
    
    private DisconnectListener onDisconnected() {
        return client -> {
            String userId = client.get("userId");
            if (userId != null) {
                // 移除客户端映射
                userClients.remove(userId);
                
                // 更新用户在线状态
                User user = userService.getUserById(userId);
                if (user != null) {
                    user.setOnline(false);
                    userService.updateUser(user);
                }
                
                // 广播用户下线状态
                broadcastUserStatus(userId, false);
                
                log.info("Client disconnected: {}, userId: {}", client.getSessionId(), userId);
            }
        };
    }
    
    // 处理聊天消息
    private void handleChatMessage(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String userId = client.get("userId");
        if (userId == null) {
            log.warn("Received message from unauthenticated client");
            return;
        }
        
        log.info("Received message from user {}: {}", userId, data);
        
        String content = (String) data.get("content");
        String type = (String) data.get("type");
        String receiverId = (String) data.get("receiverId");
        String roomId = (String) data.get("roomId");
        
        // 创建消息对象
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .senderId(userId)
                .content(content)
                .receiverId(receiverId)
                .roomId(roomId)
                .timestamp(System.currentTimeMillis())
                .type("image".equalsIgnoreCase(type) ? ChatMessage.MessageType.IMAGE : ChatMessage.MessageType.TEXT)
                .build();
        
        // 保存消息
        messageService.saveMessage(message);
        log.info("Message saved: {}", message.getId());
        
        // 发送消息给接收者或房间
        if (roomId != null) {
            log.info("Sending message to room: {}", roomId);
            sendMessageToRoom(roomId, message);
        } else if (receiverId != null) {
            log.info("Sending message to user: {}", receiverId);
            sendMessageToUser(receiverId, message);
        }
        
        // 确认消息已收到
        if (ackRequest.isAckRequested()) {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "messageAck");
            response.put("messageId", message.getId());
            response.put("success", true);
            ackRequest.sendAckData(response);
            log.info("Sent ack response for message: {}", message.getId());
        }
    }
    
    // 处理获取在线用户请求
    private void handleGetOnlineUsers(SocketIOClient client, Object data, AckRequest ackRequest) {
        String userId = client.get("userId");
        if (userId == null) {
            return;
        }
        
        log.info("User {} requested online users", userId);
        
        // 获取所有在线用户，并过滤掉当前用户
        List<User> onlineUsers = userService.getOnlineUsers().stream()
                .filter(user -> !user.getId().equals(userId))
                .collect(Collectors.toList());
        
        List<Map<String, Object>> userList = new ArrayList<>();
        for (User user : onlineUsers) {
            userList.add(convertUserToClientFormat(user));
        }
        
        // 发送响应
        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData(userList);
        }
    }
    
    // 广播用户状态变更
    private void broadcastUserStatus(String userId, boolean online) {
        User user = userService.getUserById(userId);
        if (user != null) {
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("userId", userId);
            statusData.put("username", user.getUsername());
            statusData.put("online", online);
            statusData.put("timestamp", System.currentTimeMillis());
            
            server.getBroadcastOperations().sendEvent("userStatus", statusData);
        }
    }
    
    // 发送消息给指定用户
    public void sendMessageToUser(String userId, ChatMessage message) {
        SocketIOClient client = userClients.get(userId);
        if (client != null) {
            // 转换为客户端期望的格式
            Map<String, Object> messageData = convertToClientFormat(message);
            
            // 发送新消息事件
            client.sendEvent("newMessage", messageData);
        }
    }
    
    // 发送消息给房间内所有用户
    public void sendMessageToRoom(String roomId, ChatMessage message) {
        // 转换为客户端期望的格式
        Map<String, Object> messageData = convertToClientFormat(message);
        
        // 发送给房间内所有客户端
        server.getRoomOperations(roomId).sendEvent("newMessage", messageData);
    }
    
    // 将消息转换为客户端期望的格式
    private Map<String, Object> convertToClientFormat(ChatMessage message) {
        Map<String, Object> result = new HashMap<>();
        
        // 基本字段
        result.put("_id", message.getId());  // chat-app使用_id作为主键
        result.put("senderId", message.getSenderId());
        result.put("createdAt", new java.util.Date(message.getTimestamp())); // chat-app使用createdAt
        
        // 根据消息类型设置不同的字段
        if (message.getType() == ChatMessage.MessageType.TEXT) {
            result.put("text", message.getContent());
        } else if (message.getType() == ChatMessage.MessageType.IMAGE) {
            result.put("image", message.getContent());
        } else {
            result.put("text", message.getContent());
        }
        
        // 可选字段
        if (message.getReceiverId() != null) {
            result.put("receiverId", message.getReceiverId());
        }
        
        if (message.getRoomId() != null) {
            result.put("roomId", message.getRoomId());
        }
        
        // 添加发送者信息
        if (!"system".equals(message.getSenderId())) {
            User sender = userService.getUserById(message.getSenderId());
            if (sender != null) {
                Map<String, Object> senderInfo = new HashMap<>();
                senderInfo.put("_id", sender.getId());
                senderInfo.put("username", sender.getUsername());
                senderInfo.put("profilePic", sender.getProfilePicture());
                result.put("sender", senderInfo);
            }
        } else {
            Map<String, Object> systemSender = new HashMap<>();
            systemSender.put("_id", "system");
            systemSender.put("username", "System");
            result.put("sender", systemSender);
        }
        
        return result;
    }
    
    // 将用户对象转换为客户端期望的格式
    private Map<String, Object> convertUserToClientFormat(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("_id", user.getId());
        result.put("fullName", user.getFullName());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("profilePic", user.getProfilePicture());
        result.put("online", user.isOnline());
        return result;
    }

    // 处理加入房间请求
    private void handleJoinRoom(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String userId = client.get("userId");
        if (userId == null) {
            return;
        }
        
        String roomId = (String) data.get("roomId");
        if (roomId == null) {
            return;
        }
        
        log.info("User {} joining room {}", userId, roomId);
        
        // 将客户端加入房间
        client.joinRoom(roomId);
        
        // 发送系统消息通知房间内其他用户
        User user = userService.getUserById(userId);
        if (user != null) {
            ChatMessage joinMessage = ChatMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .senderId("system")
                    .content(user.getUsername() + " joined the room")
                    .roomId(roomId)
                    .timestamp(System.currentTimeMillis())
                    .type(ChatMessage.MessageType.SYSTEM)
                    .build();
            
            // 发送给房间内所有客户端（除了刚加入的客户端）
            for (SocketIOClient c : server.getRoomOperations(roomId).getClients()) {
                if (!c.getSessionId().equals(client.getSessionId())) {
                    c.sendEvent("newMessage", convertToClientFormat(joinMessage));
                }
            }
        }
        
        // 发送确认
        if (ackRequest.isAckRequested()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roomId", roomId);
            ackRequest.sendAckData(response);
        }
    }

    // 处理离开房间请求
    private void handleLeaveRoom(SocketIOClient client, Map<String, Object> data, AckRequest ackRequest) {
        String userId = client.get("userId");
        if (userId == null) {
            return;
        }
        
        String roomId = (String) data.get("roomId");
        if (roomId == null) {
            return;
        }
        
        log.info("User {} leaving room {}", userId, roomId);
        
        // 将客户端从房间移除
        client.leaveRoom(roomId);
        
        // 发送系统消息通知房间内其他用户
        User user = userService.getUserById(userId);
        if (user != null) {
            ChatMessage leaveMessage = ChatMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .senderId("system")
                    .content(user.getUsername() + " left the room")
                    .roomId(roomId)
                    .timestamp(System.currentTimeMillis())
                    .type(ChatMessage.MessageType.SYSTEM)
                    .build();
            
            sendMessageToRoom(roomId, leaveMessage);
        }
        
        // 发送确认
        if (ackRequest.isAckRequested()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roomId", roomId);
            ackRequest.sendAckData(response);
        }
    }

    private void scheduleRestart() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.schedule(() -> {
            log.info("Attempting to restart Socket.IO server...");
            try {
                if (server != null) {
                    server.stop();
                }
                server.start();
                log.info("Socket.IO server restarted successfully");
            } catch (Exception e) {
                log.error("Failed to restart Socket.IO server", e);
                // 继续尝试重启
                scheduleRestart();
            }
        }, 10, TimeUnit.SECONDS);
    }
}








