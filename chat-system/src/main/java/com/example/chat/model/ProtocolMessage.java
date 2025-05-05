package com.example.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolMessage {
    
    private String type;
    private String userId;
    private String roomId;
    private String content;
    private long timestamp;
    private Object data;
    
    // 消息类型常量
    public static final String TYPE_CHAT = "CHAT";
    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_LEAVE = "LEAVE";
    public static final String TYPE_USER_LIST = "USER_LIST";
    public static final String TYPE_ERROR = "ERROR";
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";
}


