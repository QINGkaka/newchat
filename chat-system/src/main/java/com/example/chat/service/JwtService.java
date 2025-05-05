package com.example.chat.service;

import java.util.Date;
import java.util.Map;

public interface JwtService {
    
    /**
     * 生成JWT令牌
     * 
     * @param userId 用户ID
     * @return JWT令牌
     */
    String generateToken(String userId);
    
    /**
     * 生成带有额外声明的JWT令牌
     * 
     * @param userId 用户ID
     * @param claims 额外声明
     * @return JWT令牌
     */
    String generateToken(String userId, Map<String, Object> claims);
    
    /**
     * 验证JWT令牌
     * 
     * @param token JWT令牌
     * @return 如果有效，返回用户ID；否则返回null
     */
    String validateToken(String token);
    
    /**
     * 从令牌中提取用户ID
     * 
     * @param token JWT令牌
     * @return 用户ID
     */
    String getUserIdFromToken(String token);
    
    /**
     * 获取令牌的过期时间
     * 
     * @param token JWT令牌
     * @return 过期时间
     */
    Date getExpirationDateFromToken(String token);
}
