package com.example.chat.protocol;

public enum MessageType {
    CHAT_REQUEST,
    CHAT_RESPONSE,
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    LOGOUT_REQUEST,
    LOGOUT_RESPONSE,
    ROOM_CREATE,
    ROOM_JOIN,
    ROOM_LEAVE,
    ROOM_LIST,
    ROOM_REQUEST,
    ROOM_RESPONSE,
    HEARTBEAT_REQUEST,
    HEARTBEAT_RESPONSE,
    MESSAGE_HISTORY_REQUEST,
    MESSAGE_HISTORY_RESPONSE,
    ERROR,
    SYSTEM,
    NOTIFICATION;
    
    public byte getCode() {
        return (byte) ordinal();
    }
    
    public static MessageType fromCode(byte code) {
        if (code >= 0 && code < values().length) {
            return values()[code];
        }
        return null;
    }
}
