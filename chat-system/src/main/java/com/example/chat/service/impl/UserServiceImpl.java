package com.example.chat.service.impl;

import com.example.chat.dao.UserDao;
import com.example.chat.model.User;
import com.example.chat.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    
    @Value("${jwt.secret:yourSecretKey}")
    private String jwtSecret;
    
    private final UserDao userDao;
    
    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }
    
    @Override
    public User getUserById(String userId) {
        return userDao.findById(userId);
    }
    
    @Override
    public User getUserByEmail(String email) {
        return userDao.findByEmail(email);
    }
    
    @Override
    public User getUserByUsername(String username) {
        return userDao.findByUsername(username);
    }
    
    @Override
    public List<User> getOnlineUsers() {
        return userDao.findAll().stream()
                .filter(User::isOnline)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }
    
    @Override
    public void updateUser(User user) {
        if (user != null && user.getId() != null) {
            userDao.update(user);
        }
    }
    
    @Override
    public User createUser(User user) {
        if (user == null) {
            log.error("Cannot create null user");
            return null;
        }
        
        // 生成用户ID
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        
        // 确保必要字段不为空
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            log.error("Username cannot be empty");
            return null;
        }
        
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            log.error("Password cannot be empty");
            return null;
        }
        
        // 设置默认值
        if (user.getFullName() == null) {
            user.setFullName(user.getUsername());
        }
        user.setOnline(false);
        
        log.info("Creating new user with username: {}", user.getUsername());
        User savedUser = userDao.save(user);
        
        if (savedUser == null) {
            log.error("Failed to save user in database");
            return null;
        }
        
        log.info("Successfully created user with ID: {}", savedUser.getId());
        return savedUser;
    }
    
    @Override
    public String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        try {
            // 尝试解析JWT格式的token
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            // chat-app的token中包含userId字段
            return claims.get("userId", String.class);
        } catch (Exception e) {
            // JWT解析失败，尝试其他格式
            try {
                // 尝试解析为JSON格式
                JSONObject jsonToken = new JSONObject(token);
                if (jsonToken.has("userId")) {
                    return jsonToken.getString("userId");
                }
            } catch (Exception jsonEx) {
                // 不是JSON格式
            }
            
            // 如果是简单的测试token，直接返回
            if (token.startsWith("test-")) {
                return token.substring(5);
            }
            
            log.warn("Failed to validate token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public User login(String username, String password) {
        User user = getUserByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            user.setOnline(true);
            updateUser(user);
            return user;
        }
        return null;
    }
    
    // 初始化一些测试用户
    public void initTestUsers() {
        // 创建测试用户
        User user1 = User.builder()
            .id("user1")
            .username("user1")
            .fullName("User One")
            .email("user1@example.com")
            .online(false)
            .build();
        
        User user2 = User.builder()
            .id("user2")
            .username("user2")
            .fullName("User Two")
            .email("user2@example.com")
            .online(false)
            .build();
        
        userDao.save(user1);
        userDao.save(user2);
    }
}


