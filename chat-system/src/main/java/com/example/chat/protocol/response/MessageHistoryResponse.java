package com.example.chat.protocol.response;

import com.example.chat.model.ChatMessage;
import com.example.chat.protocol.ProtocolMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageHistoryResponse extends ProtocolMessage {
    private String roomId;
    private List<ChatMessage> messages;
    private boolean hasMore;
}





