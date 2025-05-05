package com.example.chat.dao.impl;

import com.example.chat.dao.UserDao;
import com.example.chat.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserDao implements UserDao {
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, String> usernameIndex = new ConcurrentHashMap<>();
    private final Map<String, String> emailIndex = new ConcurrentHashMap<>();
    
    @Override
    public User save(User user) {
        if (user.getId() == null || user.getUsername() == null) {
            return null;
        }
        
        users.put(user.getId(), user);
        usernameIndex.put(user.getUsername(), user.getId());
        if (user.getEmail() != null) {
            emailIndex.put(user.getEmail(), user.getId());
        }
        return user;
    }
    
    @Override
    public User findById(String userId) {
        return users.get(userId);
    }
    
    @Override
    public User findByEmail(String email) {
        String userId = emailIndex.get(email);
        return userId != null ? users.get(userId) : null;
    }
    
    @Override
    public User findByUsername(String username) {
        String userId = usernameIndex.get(username);
        return userId != null ? users.get(userId) : null;
    }
    
    @Override
    public User update(User user) {
        if (user.getId() == null) {
            return null;
        }
        users.put(user.getId(), user);
        return user;
    }
    
    @Override
    public void delete(String userId) {
        User user = users.remove(userId);
        if (user != null) {
            usernameIndex.remove(user.getUsername());
            if (user.getEmail() != null) {
                emailIndex.remove(user.getEmail());
            }
        }
    }
    
    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
} 