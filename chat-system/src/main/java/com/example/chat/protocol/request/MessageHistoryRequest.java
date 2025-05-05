package com.example.chat.protocol.request;

import com.example.chat.protocol.MessageType;
import com.example.chat.protocol.ProtocolMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MessageHistoryRequest extends ProtocolMessage {
    private String roomId;
    private long startTime;
    private long endTime;
    private int limit;
    private String lastMessageId;
    
    public MessageHistoryRequest() {
        super(MessageType.MESSAGE_HISTORY_REQUEST);
    }
}





