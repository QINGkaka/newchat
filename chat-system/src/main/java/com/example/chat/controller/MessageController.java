package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.model.User;
import com.example.chat.service.MessageService;
import com.example.chat.service.UserService;
import com.example.chat.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MessageController {
    
    private final MessageService messageService;
    private final UserService userService;
    private final JwtService jwtService;
    private static final Logger log = LoggerFactory.getLogger(MessageController.class);
    
    @Autowired
    public MessageController(MessageService messageService, UserService userService, JwtService jwtService) {
        this.messageService = messageService;
        this.userService = userService;
        this.jwtService = jwtService;
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            // 从token中提取用户ID
            String token = authHeader.replace("Bearer ", "");
            String currentUserId = jwtService.validateToken(token);
            
            if (currentUserId == null) {
                return ResponseEntity.status(401).build();
            }
            
            // 获取所有在线用户，并过滤掉当前用户
            List<User> users = userService.getOnlineUsers().stream()
                    .filter(user -> !user.getId().equals(currentUserId))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error getting users", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<List<ChatMessage>> getMessages(
            @PathVariable String userId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String currentUserId = jwtService.validateToken(token);
            
            if (currentUserId == null) {
                return ResponseEntity.status(401).build();
            }
        
        List<ChatMessage> messages = messageService.getMessagesBetweenUsers(currentUserId, userId);
        return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting messages", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    @PostMapping("/send/{userId}")
    public ResponseEntity<ChatMessage> sendMessage(
            @PathVariable String userId,
            @RequestBody Map<String, String> messageData,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String currentUserId = jwtService.validateToken(token);
            
            if (currentUserId == null) {
                return ResponseEntity.status(401).build();
            }
            
            String content = messageData.get("content");
            String type = messageData.get("type");
        
            // 创建消息对象
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                    .senderId(currentUserId)
                    .receiverId(userId)
                    .content(content)
                .timestamp(System.currentTimeMillis())
                    .type("image".equalsIgnoreCase(type) ? ChatMessage.MessageType.IMAGE : ChatMessage.MessageType.TEXT)
                .build();
        
            // 发送消息
            message = messageService.sendMessage(message);
        return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Error sending message", e);
            return ResponseEntity.status(500).build();
        }
    }
}



