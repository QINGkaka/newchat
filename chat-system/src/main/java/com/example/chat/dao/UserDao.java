package com.example.chat.dao;

import com.example.chat.model.User;
import java.util.List;

public interface UserDao {
    User save(User user);
    User findById(String userId);
    User findByEmail(String email);
    User findByUsername(String username);
    User update(User user); // 返回更新后的User对象
    void delete(String userId);
    List<User> findAll();
}
