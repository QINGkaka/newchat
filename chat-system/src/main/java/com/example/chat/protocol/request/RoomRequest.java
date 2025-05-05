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
public class RoomRequest extends ProtocolMessage {
    private String roomId;
    private String roomName;
    private String description;
    private boolean isPrivate;
    
    public RoomRequest(MessageType type, String roomId) {
        super(type);
        this.roomId = roomId;
    }
}
