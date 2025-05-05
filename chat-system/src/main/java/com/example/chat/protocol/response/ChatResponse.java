package com.example.chat.protocol.response;

import com.example.chat.protocol.MessageType;
import com.example.chat.protocol.ProtocolMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ChatResponse extends ProtocolMessage {
    private boolean success;
    private String message;
    
    public ChatResponse(boolean success, String message) {
        super(MessageType.CHAT_RESPONSE);
        this.success = success;
        this.message = message;
    }
}
