package com.example.chat.dao.impl;

import com.example.chat.dao.UserDao;
import com.example.chat.model.User;
import com.example.chat.util.JsonUtil;
import com.example.chat.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@Primary
public class RedisUserDao implements UserDao {
    
    private static final String USER_KEY_PREFIX = "chat:user:";
    private static final String USER_EMAIL_KEY_PREFIX = "chat:user:email:";
    private static final String USER_USERNAME_KEY_PREFIX = "chat:user:username:";
    private static final String ALL_USERS_KEY = "chat:users:all";
    
    @Override
    public User save(User user) {
        if (user == null) {
            log.error("Cannot save null user");
            return null;
        }
        
        if (user.getId() == null) {
            log.error("Cannot save user without ID");
            return null;
        }
        
        try (Jedis jedis = RedisUtil.getJedis()) {
            if (jedis == null) {
                log.error("Failed to get Redis connection");
                return null;
            }
            
            // 检查用户名是否已存在
            String existingUserId = jedis.get(USER_USERNAME_KEY_PREFIX + user.getUsername());
            if (existingUserId != null && !existingUserId.equals(user.getId())) {
                log.error("Username already exists: {}", user.getUsername());
                return null;
            }
            
            // 转换用户对象为JSON
            String userJson = JsonUtil.toJson(user);
            if (userJson == null) {
                log.error("Failed to convert user to JSON: {}", user);
                return null;
            }
            
            log.debug("Saving user to Redis: {}", userJson);
            
            // 保存用户信息
            String key = USER_KEY_PREFIX + user.getId();
            String result = jedis.set(key, userJson);
            if (!"OK".equals(result)) {
                log.error("Failed to save user data to Redis, result: {}", result);
                return null;
            }
            
            // 保存邮箱索引
            if (user.getEmail() != null) {
                String emailKey = USER_EMAIL_KEY_PREFIX + user.getEmail();
                result = jedis.set(emailKey, user.getId());
                if (!"OK".equals(result)) {
                    log.error("Failed to save email index to Redis, result: {}", result);
                    // 回滚用户数据
                    jedis.del(key);
                    return null;
                }
            }
            
            // 保存用户名索引
            if (user.getUsername() != null) {
                String usernameKey = USER_USERNAME_KEY_PREFIX + user.getUsername();
                result = jedis.set(usernameKey, user.getId());
                if (!"OK".equals(result)) {
                    log.error("Failed to save username index to Redis, result: {}", result);
                    // 回滚之前的操作
                    jedis.del(key);
                    if (user.getEmail() != null) {
                        jedis.del(USER_EMAIL_KEY_PREFIX + user.getEmail());
                    }
                    return null;
                }
            }
            
            // 添加到所有用户集合
            Long addResult = jedis.sadd(ALL_USERS_KEY, user.getId());
            if (addResult == null || addResult != 1) {
                log.error("Failed to add user to all users set, result: {}", addResult);
                // 回滚之前的操作
                jedis.del(key);
                if (user.getEmail() != null) {
                    jedis.del(USER_EMAIL_KEY_PREFIX + user.getEmail());
                }
                if (user.getUsername() != null) {
                    jedis.del(USER_USERNAME_KEY_PREFIX + user.getUsername());
                }
                return null;
            }
            
            log.info("Successfully saved user: {}", user.getUsername());
            return user;
        } catch (Exception e) {
            log.error("Error saving user: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public User findById(String userId) {
        if (userId == null) {
            return null;
        }
        
        try (Jedis jedis = RedisUtil.getJedis()) {
            if (jedis == null) {
                log.error("Failed to get Redis connection");
                return null;
            }
            
            String key = USER_KEY_PREFIX + userId;
            String json = jedis.get(key);
            if (json == null) {
                return null;
            }
            
            return JsonUtil.fromJson(json, User.class);
        } catch (Exception e) {
            log.error("Error finding user by ID: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public User findByEmail(String email) {
        if (email == null) {
            return null;
        }
        
        try (Jedis jedis = RedisUtil.getJedis()) {
            if (jedis == null) {
                log.error("Failed to get Redis connection");
                return null;
            }
            
            String emailKey = USER_EMAIL_KEY_PREFIX + email;
            String userId = jedis.get(emailKey);
            return userId != null ? findById(userId) : null;
        } catch (Exception e) {
            log.error("Error finding user by email: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public User findByUsername(String username) {
        if (username == null) {
            return null;
        }
        
        try (Jedis jedis = RedisUtil.getJedis()) {
            if (jedis == null) {
                log.error("Failed to get Redis connection");
                return null;
            }
            
            String usernameKey = USER_USERNAME_KEY_PREFIX + username;
            String userId = jedis.get(usernameKey);
            return userId != null ? findById(userId) : null;
        } catch (Exception e) {
            log.error("Error finding user by username: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public User update(User user) {
        if (user == null || user.getId() == null) {
            return null;
        }
        
        try (Jedis jedis = RedisUtil.getJedis()) {
            if (jedis == null) {
                log.error("Failed to get Redis connection");
                return null;
            }
            
            String key = USER_KEY_PREFIX + user.getId();
            String userJson = JsonUtil.toJson(user);
            if (userJson == null) {
                log.error("Failed to convert user to JSON");
                return null;
            }
            
            jedis.set(key, userJson);
            log.info("Successfully updated user: {}", user.getUsername());
            return user;
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public void delete(String userId) {
        if (userId == null) {
            return;
        }
        
        try (Jedis jedis = RedisUtil.getJedis()) {
            if (jedis == null) {
                log.error("Failed to get Redis connection");
                return;
            }
            
            // 获取用户信息
            User user = findById(userId);
            if (user == null) {
                return;
            }
            
            // 删除用户信息
            String key = USER_KEY_PREFIX + userId;
            jedis.del(key);
            
            // 删除邮箱索引
            if (user.getEmail() != null) {
                String emailKey = USER_EMAIL_KEY_PREFIX + user.getEmail();
                jedis.del(emailKey);
            }
            
            // 删除用户名索引
            if (user.getUsername() != null) {
                String usernameKey = USER_USERNAME_KEY_PREFIX + user.getUsername();
                jedis.del(usernameKey);
            }
            
            // 从所有用户集合中移除
            jedis.srem(ALL_USERS_KEY, userId);
            
            log.info("Successfully deleted user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public List<User> findAll() {
        try (Jedis jedis = RedisUtil.getJedis()) {
            if (jedis == null) {
                log.error("Failed to get Redis connection");
                return new ArrayList<>();
            }
            
            Set<String> userIds = jedis.smembers(ALL_USERS_KEY);
            List<User> users = new ArrayList<>();
            
            for (String userId : userIds) {
                User user = findById(userId);
                if (user != null) {
                    users.add(user);
                }
            }
            
            return users;
        } catch (Exception e) {
            log.error("Error finding all users: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}
