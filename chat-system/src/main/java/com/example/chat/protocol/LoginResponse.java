package com.example.chat.protocol;

import com.example.chat.model.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 登录响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class LoginResponse extends AbstractProtocolMessage {
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 用户信息
     */
    private User user;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 默认构造函数
     */
    public LoginResponse() {
        super(MessageType.LOGIN_RESPONSE);
    }
}