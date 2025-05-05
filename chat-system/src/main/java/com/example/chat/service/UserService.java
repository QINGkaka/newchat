package com.example.chat.service;

import com.example.chat.model.User;
import java.util.List;

public interface UserService {
    
    /**
     * 根据ID获取用户
     */
    User getUserById(String userId);
    
    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);
    
    /**
     * 根据邮箱获取用户
     */
    User getUserByEmail(String email);
    
    /**
     * 获取在线用户列表
     */
    List<User> getOnlineUsers();
    
    /**
     * 获取所有用户
     */
    List<User> getAllUsers();
    
    /**
     * 更新用户信息
     */
    void updateUser(User user);
    
    /**
     * 创建新用户
     */
    User createUser(User user);
    
    /**
     * 验证用户登录
     */
    User login(String email, String password);
    
    /**
     * 验证令牌
     */
    String validateToken(String token);
}
