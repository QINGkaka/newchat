package com.example.chat.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 自定义协议类，用于Socket.IO通信
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomProtocol<T> implements Serializable {
    
    private Header header;
    private T body;
    
    /**
     * 协议头
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header implements Serializable {
        private MessageType type;
        private String requestId;
        private int statusCode;
        private long timestamp;
    }
    
    /**
     * 创建响应协议
     */
    public static <T> CustomProtocol<T> response(MessageType type, String requestId, T body) {
        return CustomProtocol.<T>builder()
                .header(Header.builder()
                        .type(type)
                        .requestId(requestId)
                        .statusCode(StatusCode.OK.getCode())
                        .timestamp(System.currentTimeMillis())
                        .build())
                .body(body)
                .build();
    }
    
    /**
     * 创建错误响应协议
     */
    public static <T> CustomProtocol<T> error(StatusCode statusCode, String requestId, T body) {
        return CustomProtocol.<T>builder()
                .header(Header.builder()
                        .type(MessageType.ERROR)
                        .requestId(requestId)
                        .statusCode(statusCode.getCode())
                        .timestamp(System.currentTimeMillis())
                        .build())
                .body(body)
                .build();
    }
}















