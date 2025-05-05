package com.example.chat.controller;

import com.example.chat.model.User;
import com.example.chat.service.JwtService;
import com.example.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final JwtService jwtService;
    
    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        log.debug("Checking auth status with header: {}", authHeader);
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("No valid Authorization header found");
                return ResponseEntity.ok(Map.of("authenticated", false));
            }
            
            String token = authHeader.substring(7);
            String userId = jwtService.validateToken(token);
            
            if (userId == null) {
                log.debug("Invalid token");
                return ResponseEntity.ok(Map.of("authenticated", false));
            }
            
            User user = userService.getUserById(userId);
            if (user == null) {
                log.debug("User not found for ID: {}", userId);
                return ResponseEntity.ok(Map.of("authenticated", false));
            }
            
            log.debug("User authenticated: {}", userId);
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "user", user
            ));
        } catch (Exception e) {
            log.error("Error checking auth status: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData) {
        try {
            String email = userData.get("email");
            String fullName = userData.get("fullName");
            String password = userData.get("password");
            
            if (email == null || fullName == null || password == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Missing required fields"));
            }
            
            // 检查邮箱是否已存在
            if (userService.getUserByEmail(email) != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Email already exists"));
            }
            
            // 创建新用户
            User user = User.builder()
                    .username(email) // 使用邮箱作为用户名
                    .email(email)
                    .fullName(fullName)
                    .password(password)
                    .build();
            
            User savedUser = userService.createUser(user);
            if (savedUser == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to create user"));
            }
            
            // 生成JWT token
            String token = jwtService.generateToken(savedUser.getId());
            
            // 返回用户信息和token
            return ResponseEntity.ok(Map.of(
                    "user", savedUser,
                    "token", token
            ));
        } catch (Exception e) {
            log.error("Error during registration: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to register user"));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        // 使用login方法获取用户对象
        User user = userService.login(username, password);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
        
        String token = jwtService.generateToken(user.getId());
        
        return ResponseEntity.ok(Map.of(
                "user", user,
                "token", token
        ));
    }
}





