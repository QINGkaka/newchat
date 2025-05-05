package com.example.chat.protocol;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 协议消息的抽象基类
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class AbstractProtocolMessage {
    private String requestId;
    private String sender;
    private String content;
    private long timestamp;
    private MessageType type;
    
    /**
     * 构造函数
     * @param type 消息类型
     */
    protected AbstractProtocolMessage(MessageType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
}




