package com.example.chat.protocol;

import com.example.chat.protocol.request.*;

/**
 * 消息验证器
 */
public class MessageValidator {
    
    /**
     * 验证消息
     */
    public static boolean validate(ProtocolMessage message) {
        if (message == null) {
            return false;
        }
        
        // 验证基本字段
        if (message.getType() == null) {
            return false;
        }
        
        // 根据消息类型进行特定验证
        switch (message.getType()) {
            case CHAT_REQUEST:
                return validateChatRequest((ChatRequest) message);
            case LOGIN_REQUEST:
                return validateLoginRequest((LoginRequest) message);
            case ROOM_REQUEST:
                return validateRoomRequest((RoomRequest) message);
            case MESSAGE_HISTORY_REQUEST:
                return validateMessageHistoryRequest((MessageHistoryRequest) message);
            default:
                return true;
        }
    }
    
    private static boolean validateChatRequest(ChatRequest request) {
        return request.getSender() != null && !request.getSender().isEmpty() &&
               request.getContent() != null && !request.getContent().isEmpty();
    }
    
    private static boolean validateLoginRequest(LoginRequest request) {
        return request.getUsername() != null && !request.getUsername().isEmpty() &&
               request.getPassword() != null && !request.getPassword().isEmpty();
        }

    private static boolean validateRoomRequest(RoomRequest request) {
        return request.getRoomId() != null && !request.getRoomId().isEmpty();
    }

    private static boolean validateMessageHistoryRequest(MessageHistoryRequest request) {
        return request.getRoomId() != null && !request.getRoomId().isEmpty() &&
               request.getStartTime() >= 0 && request.getEndTime() >= request.getStartTime() &&
               request.getLimit() > 0;
        }
    }







