package com.example.chat.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int statusCode;
    private String message;
    private String requestId;
    
    public static ErrorResponse create(StatusCode statusCode, String message) {
        return ErrorResponse.builder()
                .statusCode(statusCode.getCode())
                .message(message)
                .build();
    }
    
    public static ErrorResponse create(StatusCode statusCode, String message, String requestId) {
        return ErrorResponse.builder()
                .statusCode(statusCode.getCode())
                .message(message)
                .requestId(requestId)
                .build();
    }
}
