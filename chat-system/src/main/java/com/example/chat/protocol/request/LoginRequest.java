package com.example.chat.protocol.request;

import com.example.chat.protocol.MessageType;
import com.example.chat.protocol.ProtocolMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class LoginRequest extends ProtocolMessage {
    private String username;
    private String password;
    private String deviceId;
    
    public LoginRequest() {
        setType(MessageType.LOGIN_REQUEST);
    }
}
