package com.example.chat.protocol.response;

import com.example.chat.protocol.ProtocolMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationMessage extends ProtocolMessage {
    private String title;
    private String message;
    private String level;
    private long timestamp;
} 