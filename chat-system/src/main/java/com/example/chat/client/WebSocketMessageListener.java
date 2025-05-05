package com.example.chat.client;

/**
 * WebSocket消息监听器接口
 */
public interface WebSocketMessageListener {
    /**
     * 当收到消息时调用
     * @param message 收到的消息
     */
    void onMessage(String message);
}