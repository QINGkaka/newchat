package com.example.chat.protocol.message;

import com.example.chat.protocol.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String messageId;
    private MessageType type;
    private String content;
    private long timestamp;
    private String targetId; // 可以是用户ID或房间ID
}
