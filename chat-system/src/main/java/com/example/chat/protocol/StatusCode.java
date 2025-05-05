package com.example.chat.protocol;

import lombok.Getter;

/**
 * 状态码枚举
 */
@Getter
public enum StatusCode {
    
    // 成功
    OK(200, "Success"),
    
    // 客户端错误
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    
    // 服务器错误
    INTERNAL_ERROR(500, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    
    // 业务错误
    USER_NOT_EXIST(1001, "User does not exist"),
    USER_ALREADY_EXIST(1002, "User already exists"),
    ROOM_NOT_EXIST(1003, "Room does not exist"),
    ROOM_ALREADY_EXIST(1004, "Room already exists"),
    MESSAGE_NOT_EXIST(1005, "Message does not exist"),
    ALREADY_LOGGED_IN(1006, "User already logged in"),
    INVALID_CREDENTIALS(1007, "Invalid credentials");
    
    private final int code;
    private final String message;
    
    StatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    /**
     * 根据状态码获取枚举
     */
    public static StatusCode fromCode(int code) {
        for (StatusCode statusCode : values()) {
            if (statusCode.getCode() == code) {
                return statusCode;
            }
        }
        return INTERNAL_ERROR;
    }
}


