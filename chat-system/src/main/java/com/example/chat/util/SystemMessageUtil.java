package com.example.chat.util;

import com.example.chat.protocol.MessageType;
import com.example.chat.protocol.response.SystemMessage;

/**
 * 系统消息工具类
 */
public class SystemMessageUtil {
    
    /**
     * 创建系统消息
     */
    public static SystemMessage createSystemMessage(String title, String message, String level) {
        return SystemMessage.builder()
                .type(MessageType.SYSTEM)
                .title(title)
                .message(message)
                .level(level)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static SystemMessage createErrorSystemMessage(String title, String message) {
        return createSystemMessage(title, message, "ERROR");
    }

    public static SystemMessage createInfoSystemMessage(String title, String message) {
        return createSystemMessage(title, message, "INFO");
    }

    public static SystemMessage createWarningSystemMessage(String title, String message) {
        return createSystemMessage(title, message, "WARNING");
    }
}




