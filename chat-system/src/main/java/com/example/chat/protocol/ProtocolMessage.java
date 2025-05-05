package com.example.chat.protocol;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class ProtocolMessage {
    private MessageType type;
    private String requestId;
    private short statusCode;
    
    protected ProtocolMessage(MessageType type) {
        this.type = type;
    }
}





