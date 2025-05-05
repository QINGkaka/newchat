package com.example.chat.protocol.request;

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
public class ChatRequest extends ProtocolMessage {
    private String sender;
    private String content;
    private String roomId;
    
    public ChatRequest(String sender, String content, String roomId) {
        super(MessageType.CHAT_REQUEST);
        this.sender = sender;
        this.content = content;
        this.roomId = roomId;
    }
}
