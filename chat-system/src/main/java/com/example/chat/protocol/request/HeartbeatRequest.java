package com.example.chat.protocol.request;

import com.example.chat.protocol.ProtocolMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HeartbeatRequest extends ProtocolMessage {
    // 不需要额外字段
}
