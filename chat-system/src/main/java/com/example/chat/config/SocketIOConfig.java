package com.example.chat.config;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import com.corundumstudio.socketio.Transport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.chat.service.UserService;

@Slf4j
@org.springframework.context.annotation.Configuration
public class SocketIOConfig {

    @Value("${socketio.host:0.0.0.0}")
    private String host;
    
    @Value("${socketio.port:19098}")
    private int port;
    
    @Value("${socketio.bossCount:1}")
    private int bossCount;
    
    @Value("${socketio.workCount:100}")
    private int workCount;
    
    @Value("${socketio.allowCustomRequests:true}")
    private boolean allowCustomRequests;
    
    @Value("${socketio.upgradeTimeout:10000}")
    private int upgradeTimeout;
    
    @Value("${socketio.pingTimeout:60000}")
    private int pingTimeout;
    
    @Value("${socketio.pingInterval:25000}")
    private int pingInterval;
    
    @Autowired
    private UserService userService;
    
    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname(host);
        config.setPort(port);
        config.setBossThreads(bossCount);
        config.setWorkerThreads(workCount);
        config.setAllowCustomRequests(allowCustomRequests);
        config.setUpgradeTimeout(upgradeTimeout);
        config.setPingTimeout(pingTimeout);
        config.setPingInterval(pingInterval);
        
        // 增加重试次数和超时设置
        config.setMaxFramePayloadLength(1024 * 1024);
        config.setMaxHttpContentLength(1024 * 1024);
        config.setOrigin("*");
        
        // 允许跨域
        config.setOrigin("http://localhost:5173");
        
        // 设置路径
        config.setContext("/socket.io");
        
        // 允许所有传输方式
        config.setTransports(Transport.WEBSOCKET, Transport.POLLING);
        
        // 禁用WebSocket压缩
        config.setWebsocketCompression(false);
        
        // 添加认证监听器
        config.setAuthorizationListener(data -> {
            try {
                String token = null;
                String authHeader = data.getHttpHeaders().get("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                } else {
                    token = data.getSingleUrlParam("token");
                }
                
                if (token == null) {
                    log.warn("No token provided in Socket.IO connection attempt");
                    return false;
                }
                
                // 验证 token
                String userId = userService.validateToken(token);
                if (userId == null) {
                    log.warn("Invalid token in Socket.IO connection attempt");
                    return false;
                }
                
                log.info("Socket.IO connection authorized for user: {}", userId);
                return true;
            } catch (Exception e) {
                log.error("Error during Socket.IO authorization: {}", e.getMessage());
                return false;
            }
        });
        
        // 添加 CORS 配置
        config.setAllowCustomRequests(true);
        config.setAllowHeaders("Authorization,Content-Type,Accept,Origin,X-Requested-With");
        
        log.info("Configuring Socket.IO server on {}:{}", host, port);
        SocketIOServer server = new SocketIOServer(config);
        
        // 添加连接事件监听器
        server.addConnectListener(client -> {
            log.info("Client connected: {}", client.getSessionId());
        });
        
        // 添加断开连接事件监听器
        server.addDisconnectListener(client -> {
            log.info("Client disconnected: {}", client.getSessionId());
        });
        
        return server;
    }
}

