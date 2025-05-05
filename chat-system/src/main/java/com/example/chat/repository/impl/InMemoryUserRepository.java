package com.example.chat.repository.impl;

import com.example.chat.model.User;
import com.example.chat.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryUserRepository implements UserRepository {
    
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    @Override
    public User findById(String userId) {
        return users.get(userId);
    }
    
    @Override
    public User findByEmail(String email) {
        return users.values().stream()
                .filter(user -> email.equals(user.getEmail()))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public User save(User user) {
        users.put(user.getId(), user);
        return user;
    }
    
    @Override
    public void delete(String userId) {
        users.remove(userId);
    }
    
    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}