package com.example.chat.protocol.response;

import com.example.chat.protocol.MessageType;
import com.example.chat.protocol.ProtocolMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RoomResponse extends ProtocolMessage {
    private String roomId;
    private String roomName;
    private String description;
    private boolean isPrivate;
    private boolean success;
    private String message;
    
    public RoomResponse() {
        super(MessageType.ROOM_RESPONSE);
    }
}
