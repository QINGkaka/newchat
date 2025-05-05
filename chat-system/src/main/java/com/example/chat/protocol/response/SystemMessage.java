package com.example.chat.protocol.response;

import com.example.chat.protocol.ProtocolMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 系统消息
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemMessage extends ProtocolMessage {
    /**
     * 消息标题
     */
    private String title;
    
    /**
     * 消息内容
     */
    private String message;
    
    /**
     * 消息级别
     */
    private String level;
    
    /**
     * 消息时间戳
     */
    private long timestamp;
}



