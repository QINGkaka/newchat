package com.example.chat.client;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket客户端适配器，用于连接到chat-app
 */
@Slf4j
@Component
public class WebSocketClientAdapter implements WebSocketMessageListener {

    @Value("${chat-app.websocket.url:ws://localhost:5001/socket.io}")
    private String websocketUrl;
    
    @Value("${chat-app.websocket.enabled:false}")
    private boolean enabled;
    
    @Value("${chat-app.auth.userId:system}")
    private String userId;
    
    @Value("${chat-app.auth.token:}")
    private String token;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MessageService messageService;
    
    private EventLoopGroup group;
    private Channel channel;
    private boolean connected = false;
    private WebSocketClientHandler handler;
    
    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("WebSocket client adapter is disabled");
            return;
        }
        
        connect();
    }
    
    @PreDestroy
    public void destroy() {
        disconnect();
    }
    
    /**
     * 连接到WebSocket服务器
     */
    public void connect() {
        if (!enabled) {
            return;
        }
        
        try {
            URI uri = new URI(websocketUrl);
            String scheme = uri.getScheme();
            final boolean ssl = "wss".equalsIgnoreCase(scheme);
            final String host = uri.getHost();
            final int port = uri.getPort() != -1 ? uri.getPort() : (ssl ? 443 : 80);
            
            group = new NioEventLoopGroup();
            
            // 配置SSL
            final SslContext sslCtx;
            if (ssl) {
                sslCtx = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            } else {
                sslCtx = null;
            }
            
            // 创建WebSocket握手处理器
            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(
                    uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders());
            
            // 创建WebSocket客户端处理器
            handler = new WebSocketClientHandler(handshaker);
            handler.setMessageListener(this);
            
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            if (sslCtx != null) {
                                pipeline.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            pipeline.addLast(new HttpClientCodec());
                            pipeline.addLast(new HttpObjectAggregator(8192));
                            pipeline.addLast(handler);
                        }
                    });
            
            // 连接服务器
            channel = bootstrap.connect(host, port).sync().channel();
            
            // 等待握手完成
            handler.handshakeFuture().sync();
            
            // 发送认证消息
            sendAuthMessage();
            
            connected = true;
            log.info("Connected to WebSocket server: {}", websocketUrl);
            
        } catch (Exception e) {
            log.error("Failed to connect to WebSocket server", e);
            disconnect();
            
            // 尝试重连
            scheduleReconnect();
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (group != null) {
            group.shutdownGracefully();
            group = null;
        }
        connected = false;
    }
    
    /**
     * 发送消息
     */
    public void sendMessage(ChatMessage message) {
        if (!connected || channel == null) {
            log.warn("Not connected to WebSocket server, message not sent");
            return;
        }
        
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "message");
            data.put("senderId", message.getSenderId());
            data.put("roomId", message.getRoomId());
            data.put("receiverId", message.getReceiverId());
            
            if (message.getType() == ChatMessage.MessageType.IMAGE) {
                data.put("image", message.getContent());
            } else {
                data.put("text", message.getContent());
            }
            
            String json = objectMapper.writeValueAsString(data);
            channel.writeAndFlush(new TextWebSocketFrame(json));
            log.debug("Message sent to WebSocket server: {}", json);
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }
    
    /**
     * 发送认证消息
     */
    private void sendAuthMessage() {
        if (channel == null) {
            return;
        }
        
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("type", "auth");
            data.put("userId", userId);
            data.put("token", token);
            
            String json = objectMapper.writeValueAsString(data);
            channel.writeAndFlush(new TextWebSocketFrame(json));
            log.debug("Auth message sent: {}", json);
        } catch (Exception e) {
            log.error("Failed to send auth message", e);
        }
    }
    
    /**
     * 处理从WebSocket服务器接收到的消息
     */
    @Override
    public void onMessage(String message) {
        try {
            log.debug("Received message from WebSocket server: {}", message);
            
            // 解析消息
            Map<String, Object> data = objectMapper.readValue(message, 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            String type = (String) data.get("type");
            
            if ("message".equals(type)) {
                handleChatMessage(data);
            } else if ("auth".equals(type)) {
                handleAuthResponse(data);
            }
        } catch (Exception e) {
            log.error("Failed to process received message", e);
        }
    }
    
    /**
     * 处理聊天消息
     */
    private void handleChatMessage(Map<String, Object> data) {
        try {
            String senderId = (String) data.get("senderId");
            String text = (String) data.get("text");
            String image = (String) data.get("image");
            String roomId = (String) data.get("roomId");
            String receiverId = (String) data.get("receiverId");
            
            // 创建消息对象
            ChatMessage message = ChatMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .senderId(senderId)
                    .content(text != null ? text : image)
                    .roomId(roomId)
                    .receiverId(receiverId)
                    .timestamp(System.currentTimeMillis())
                    .type(image != null ? ChatMessage.MessageType.IMAGE : ChatMessage.MessageType.TEXT)
                    .build();
            
            // 保存消息
            messageService.saveMessage(message);
            
            log.info("Received chat message from {}: {}", senderId, text != null ? text : "[image]");
        } catch (Exception e) {
            log.error("Failed to handle chat message", e);
        }
    }
    
    /**
     * 处理认证响应
     */
    private void handleAuthResponse(Map<String, Object> data) {
        boolean success = (boolean) data.get("success");
        if (success) {
            log.info("Authentication successful");
        } else {
            log.error("Authentication failed: {}", data.get("message"));
            disconnect();
        }
    }
    
    /**
     * 安排重连任务
     */
    private void scheduleReconnect() {
        if (!enabled) {
            return;
        }
        
        // 使用单线程执行器而不是EventLoopGroup
        Thread reconnectThread = new Thread(() -> {
            try {
                // 延迟5秒后执行重连
                Thread.sleep(5000);
                log.info("Attempting to reconnect...");
                connect();
            } catch (Exception e) {
                log.error("Reconnection attempt failed", e);
            }
        });
        
        // 设置为守护线程，这样不会阻止JVM退出
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }
}















