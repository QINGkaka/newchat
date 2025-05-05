package com.example.chat.protocol.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int code;
    private String message;
    
    public static ErrorResponse create(StatusCode statusCode, String message) {
        return ErrorResponse.builder()
                .code(statusCode.getCode())
                .message(message)
                .build();
    }
}
