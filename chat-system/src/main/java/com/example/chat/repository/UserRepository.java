package com.example.chat.repository;

import com.example.chat.model.User;
import java.util.List;

public interface UserRepository {
    User findById(String userId);
    User findByEmail(String email);
    User save(User user);
    void delete(String userId);
    List<User> findAll();
}