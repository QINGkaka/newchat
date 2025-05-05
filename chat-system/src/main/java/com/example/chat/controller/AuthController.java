package com.example.chat.controller;

import com.example.chat.model.User;
import com.example.chat.service.JwtService;
import com.example.chat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:19096"}, allowCredentials = "true")
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
        log.debug("Login attempt with credentials: {}", credentials);
        String email = credentials.get("email");
        String password = credentials.get("password");
        
        if (email == null || password == null) {
            log.debug("Missing email or password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "邮箱和密码不能为空"));
        }
        
        // 使用email登录
        User user = userService.login(email, password);
        if (user == null) {
            log.debug("Invalid credentials for email: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "邮箱或密码错误"));
        }
        
        String token = jwtService.generateToken(user.getId());
        log.debug("Login successful for user: {}", user.getId());
        
        return ResponseEntity.ok(Map.of(
                "user", user,
                "token", token
        ));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            // 获取token
            String token = extractToken(request);
            if (token != null) {
                // 从token中获取用户ID
                String userId = userService.validateToken(token);
                if (userId != null) {
                    // 更新用户在线状态
                    User user = userService.getUserById(userId);
                    if (user != null) {
                        user.setOnline(false);
                        userService.updateUser(user);
                        log.info("User {} logged out successfully", userId);
                    }
                }
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "登出失败");
                    }});
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}





