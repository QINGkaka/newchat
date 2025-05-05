package com.example.chat.core.server;

import com.example.chat.model.ChatMessage;

/**
 * 聊天服务器接口
 */
public interface ChatServer {
    
    /**
     * 启动服务器
     */
    void start() throws Exception;
    
    /**
     * 停止服务器
     */
    void stop();
    
    /**
     * 发送消息给指定用户
     */
    void sendMessageToUser(String userId, ChatMessage message);
    
    /**
     * 发送消息到聊天室
     */
    void sendMessageToRoom(String roomId, ChatMessage message);
}

