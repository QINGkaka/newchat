package com.example.chat.core.server;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SocketIOAdapter {
    
    private final SocketIOServer server;
    private final UserService userService;
    private final MessageService messageService;
    
    // 存储用户ID与客户端的映射关系
    private final Map<String, Set<SocketIOClient>> userClients = new ConcurrentHashMap<>();
    
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
        this.server.addEventListener("ping", Object.class, (client, data, ack) -> onPing(client));
        
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
            // 配置 Socket.IO 服务器
            com.corundumstudio.socketio.Configuration config = server.getConfiguration();
            config.setPingTimeout(60000);  // 60秒 ping 超时
            config.setPingInterval(25000); // 25秒发送一次 ping
            config.setAllowCustomRequests(true);
            config.setUpgradeTimeout(10000);
            config.setMaxFramePayloadLength(1024 * 1024); // 1MB
            config.setAllowHeaders("*");
            
            // 启动服务器
            server.start();
            log.info("Socket.IO server started on {}:{}", 
                    config.getHostname(), 
                    config.getPort());
                    
            // 启动心跳超时检查定时任务
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(this::checkHeartbeatTimeout, 30, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to start Socket.IO server", e);
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
    
    private void onPing(SocketIOClient client) {
        String userId = client.get("userId");
        if (userId != null) {
            log.debug("Ping received from user: {}", userId);
            // 更新用户最后心跳时间
            client.set("lastPingTime", System.currentTimeMillis());
            
            // 发送 pong 响应
            Map<String, Object> pongData = new HashMap<>();
            pongData.put("timestamp", System.currentTimeMillis());
            client.sendEvent("pong", pongData);
            
            // 更新用户在线状态
            User user = userService.getUserById(userId);
            if (user != null) {
                boolean wasOnline = user.isOnline();
                user.setOnline(true);
                userService.updateUser(user);
                
                // 只有在状态发生变化时才广播
                if (!wasOnline) {
                    broadcastUserStatus(userId, true);
                    log.info("User {} marked as online after receiving ping", userId);
                }
            }
        }
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
                client.set("lastPingTime", System.currentTimeMillis());
                client.set("connectTime", System.currentTimeMillis());
                
                synchronized (userClients) {
                    Set<SocketIOClient> clients = userClients.computeIfAbsent(userId, k -> new HashSet<>());
                    
                    // 检查是否已有连接
                    if (!clients.isEmpty()) {
                        // 如果已有连接，检查最后心跳时间
                        boolean hasActiveConnection = false;
                        for (SocketIOClient existingClient : new HashSet<>(clients)) {
                            Long lastPingTime = existingClient.get("lastPingTime");
                            if (lastPingTime != null && System.currentTimeMillis() - lastPingTime < 30000) {
                                hasActiveConnection = true;
                                break;
                            }
                        }
                        
                        if (hasActiveConnection) {
                            // 如果有活跃连接，发送错误消息给客户端
                            Map<String, Object> errorData = new HashMap<>();
                            errorData.put("error", "ALREADY_CONNECTED");
                            errorData.put("message", "You already have an active connection");
                            client.sendEvent("error", errorData);
                            log.warn("User {} already has active connection, rejecting new connection", userId);
                            client.disconnect();
                            return;
                        } else {
                            // 如果没有活跃连接，清理旧连接
                            for (SocketIOClient oldClient : new HashSet<>(clients)) {
                                try {
                                    oldClient.disconnect();
                                    log.info("Disconnected inactive client: {} for user: {}", oldClient.getSessionId(), userId);
                                } catch (Exception e) {
                                    log.error("Error disconnecting old client: {}", oldClient.getSessionId(), e);
                                }
                            }
                            clients.clear();
                        }
                    }
                    
                    // 添加新连接
                    clients.add(client);
                    
                    // 更新用户在线状态
                    User user = userService.getUserById(userId);
                    if (user != null) {
                        user.setOnline(true);
                        userService.updateUser(user);
                        broadcastUserStatus(userId, true);
                        log.info("User {} marked as online with new connection: {}", userId, client.getSessionId());
                    }
                }
                
                // 发送当前在线用户列表给新连接的客户端
                sendOnlineUsersToClient(client);
                
                // 发送连接成功消息
                Map<String, Object> successData = new HashMap<>();
                successData.put("status", "CONNECTED");
                successData.put("message", "Successfully connected to server");
                client.sendEvent("connectionStatus", successData);
                
                log.info("Client connected: {}, userId: {}, total connections: {}", 
                    client.getSessionId(), userId, userClients.get(userId).size());
            } else {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", "AUTH_FAILED");
                errorData.put("message", "Authentication failed");
                client.sendEvent("error", errorData);
                client.disconnect();
                log.warn("Authentication failed for client: {}", client.getSessionId());
            }
        };
    }
    
    // 发送在线用户列表给指定客户端
    private void sendOnlineUsersToClient(SocketIOClient client) {
        String userId = client.get("userId");
        if (userId == null) {
            return;
        }
        
        List<User> allUsers = userService.getAllUsers();
        List<Map<String, Object>> userList = new ArrayList<>();
        
        for (User user : allUsers) {
            if (!user.getId().equals(userId)) {
                Map<String, Object> userData = convertUserToClientFormat(user);
                // 检查用户是否真的在线（有活跃连接且最近有心跳）
                boolean isReallyOnline = false;
                Set<SocketIOClient> userClients = this.userClients.get(user.getId());
                if (userClients != null && !userClients.isEmpty()) {
                    for (SocketIOClient c : userClients) {
                        Long lastPingTime = c.get("lastPingTime");
                        if (lastPingTime != null && System.currentTimeMillis() - lastPingTime < 30000) {
                            isReallyOnline = true;
                            break;
                        }
                    }
                }
                userData.put("online", isReallyOnline);
                userList.add(userData);
            }
        }
        
        try {
            client.sendEvent("onlineUsers", userList);
            log.debug("Sent online users list to client: {}", client.getSessionId());
        } catch (Exception e) {
            log.error("Failed to send online users list to client: {}", client.getSessionId(), e);
        }
    }
    
    private DisconnectListener onDisconnected() {
        return client -> {
            String userId = client.get("userId");
            if (userId != null) {
                Long connectTime = client.get("connectTime");
                long connectionDuration = connectTime != null ? 
                    System.currentTimeMillis() - connectTime : 0;
                
                synchronized (userClients) {
                    Set<SocketIOClient> clients = userClients.get(userId);
                    if (clients != null) {
                        clients.remove(client);
                        log.info("Client disconnected: {}, userId: {}, connection duration: {}ms, remaining connections: {}", 
                            client.getSessionId(), userId, connectionDuration, clients.size());
                            
                        if (clients.isEmpty()) {
                            User user = userService.getUserById(userId);
                            if (user != null) {
                                user.setOnline(false);
                                userService.updateUser(user);
                                broadcastUserStatus(userId, false);
                                log.info("User {} marked as offline after {}ms connection", userId, connectionDuration);
                            }
                        }
                    }
                }
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
        
        // 更新最后活动时间
        client.set("lastPingTime", System.currentTimeMillis());
        
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
        
        // 获取所有用户
        List<User> allUsers = userService.getAllUsers();
        List<Map<String, Object>> userList = new ArrayList<>();
        
        for (User user : allUsers) {
            if (!user.getId().equals(userId)) {  // 排除当前用户
                Map<String, Object> userData = convertUserToClientFormat(user);
                
                // 检查用户是否真的在线（有活跃连接且最近有心跳）
                boolean isReallyOnline = false;
                Set<SocketIOClient> userClients = this.userClients.get(user.getId());
                if (userClients != null && !userClients.isEmpty()) {
                    for (SocketIOClient c : userClients) {
                        Long lastPingTime = c.get("lastPingTime");
                        if (lastPingTime != null && System.currentTimeMillis() - lastPingTime < 30000) {
                            isReallyOnline = true;
                            break;
                        }
                    }
                }
                
                // 更新用户在线状态
                if (isReallyOnline != user.isOnline()) {
                    user.setOnline(isReallyOnline);
                    userService.updateUser(user);
                }
                
                userData.put("online", isReallyOnline);
                userList.add(userData);
            }
        }
        
        // 发送响应
        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData(userList);
            log.debug("Sent online users list to client: {}, count: {}", client.getSessionId(), userList.size());
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
            
            // 广播给所有在线用户
            for (Map.Entry<String, Set<SocketIOClient>> entry : userClients.entrySet()) {
                Set<SocketIOClient> clients = entry.getValue();
                
                // 检查目标用户是否有活跃连接
                boolean hasActiveConnection = false;
                for (SocketIOClient client : clients) {
                    Long lastPingTime = client.get("lastPingTime");
                    if (lastPingTime != null && System.currentTimeMillis() - lastPingTime < 30000) {
                        hasActiveConnection = true;
                        break;
                    }
                }
                
                if (hasActiveConnection) {
                    // 只发送给有活跃连接的用户
                    for (SocketIOClient client : clients) {
                        try {
                            client.sendEvent("userStatus", statusData);
                            log.debug("Sent user status update to client: {}, user: {}, online: {}", 
                                client.getSessionId(), userId, online);
                        } catch (Exception e) {
                            log.error("Failed to send user status to client: {}", client.getSessionId(), e);
                        }
                    }
                }
            }
        }
    }
    
    // 发送消息给指定用户
    public void sendMessageToUser(String userId, ChatMessage message) {
        Set<SocketIOClient> clients = userClients.get(userId);
        boolean messageSent = false;
        
        if (clients != null && !clients.isEmpty()) {
            Map<String, Object> messageData = convertToClientFormat(message);
            log.info("Sending message to user: {}, messageId: {}, clients: {}", userId, message.getId(), clients.size());
            
            for (SocketIOClient client : clients) {
                // 检查客户端是否活跃
                Long lastPingTime = client.get("lastPingTime");
                if (lastPingTime != null && System.currentTimeMillis() - lastPingTime < 30000) {
                    try {
                        client.sendEvent("newMessage", messageData);
                        messageSent = true;
                        log.debug("Message sent to client: {}", client.getSessionId());
                    } catch (Exception e) {
                        log.error("Failed to send message to client: {}", client.getSessionId(), e);
                    }
                }
            }
        }
        
        if (!messageSent) {
            // 如果消息没有发送成功，检查用户状态
            User user = userService.getUserById(userId);
            if (user != null) {
                boolean isReallyOnline = false;
                if (clients != null) {
                    for (SocketIOClient client : clients) {
                        Long lastPingTime = client.get("lastPingTime");
                        if (lastPingTime != null && System.currentTimeMillis() - lastPingTime < 30000) {
                            isReallyOnline = true;
                            break;
                        }
                    }
                }
                
                // 更新用户在线状态
                if (user.isOnline() != isReallyOnline) {
                    user.setOnline(isReallyOnline);
                    userService.updateUser(user);
                    broadcastUserStatus(userId, isReallyOnline);
                    log.info("Updated user {} online status to: {}", userId, isReallyOnline);
                }
                
                if (!isReallyOnline) {
                    log.warn("User {} is offline, message not sent: {}", userId, message.getId());
                }
            } else {
                log.warn("User {} not found, message not sent: {}", userId, message.getId());
            }
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
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                Thread.sleep(RETRY_INTERVAL);
                server.start();
                log.info("Socket.IO server restarted successfully after {} attempts", retryCount + 1);
                return;
            } catch (Exception e) {
                retryCount++;
                log.error("Failed to restart Socket.IO server (attempt {}/{})", retryCount, MAX_RETRY_COUNT, e);
            }
        }
        log.error("Failed to restart Socket.IO server after {} attempts", MAX_RETRY_COUNT);
    }

    // 增加心跳超时检查
    private void checkHeartbeatTimeout() {
        long currentTime = System.currentTimeMillis();
        long timeout = 120000; // 增加到120秒超时
        for (Map.Entry<String, Set<SocketIOClient>> entry : userClients.entrySet()) {
            String userId = entry.getKey();
            Set<SocketIOClient> clients = entry.getValue();
            clients.removeIf(client -> {
                Long lastPingTime = client.get("lastPingTime");
                if (lastPingTime == null || currentTime - lastPingTime > timeout) {
                    log.warn("Client {} heartbeat timeout (last ping: {}ms ago), disconnecting", 
                        client.getSessionId(), 
                        lastPingTime != null ? currentTime - lastPingTime : -1);
                    client.disconnect();
                    return true;
                }
                return false;
            });
            if (clients.isEmpty()) {
                User user = userService.getUserById(userId);
                if (user != null) {
                    user.setOnline(false);
                    userService.updateUser(user);
                    broadcastUserStatus(userId, false);
                    log.info("User {} marked as offline due to no active connections", userId);
                }
            }
        }
    }
}








