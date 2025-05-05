package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import com.example.chat.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MessageServiceTest {

    private MessageService messageService;
    private String senderId;
    private String receiverId;
    private String roomId;

    @BeforeEach
    public void setup() {
        messageService = new MessageServiceImpl();
        senderId = "user1";
        receiverId = "user2";
        roomId = "room1";
        
        // 添加一些测试消息
        for (int i = 0; i < 5; i++) {
            ChatMessage message = ChatMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .content("Test message " + i)
                    .timestamp(System.currentTimeMillis() - i * 1000) // 递减时间戳
                    .type(ChatMessage.MessageType.TEXT)
                    .build();
            messageService.saveMessage(message);
        }
        
        // 添加一些房间消息
        for (int i = 0; i < 3; i++) {
            ChatMessage message = ChatMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .senderId(senderId)
                    .roomId(roomId)
                    .content("Room message " + i)
                    .timestamp(System.currentTimeMillis() - i * 1000) // 递减时间戳
                    .type(ChatMessage.MessageType.TEXT)
                    .build();
            messageService.saveMessage(message);
        }
    }

    @Test
    public void testGetUserMessages() {
        List<ChatMessage> messages = messageService.getUserMessages(senderId);
        
        // 应该有8条消息 (5条直接消息 + 3条房间消息)
        assertEquals(8, messages.size());
        
        // 测试限制条数
        List<ChatMessage> limitedMessages = messageService.getUserMessages(senderId, receiverId, 3);
        assertEquals(3, limitedMessages.size());
    }

    @Test
    public void testGetRoomMessages() {
        List<ChatMessage> messages = messageService.getRoomMessages(roomId);
        
        // 应该有3条房间消息
        assertEquals(3, messages.size());
        
        // 测试限制条数
        List<ChatMessage> limitedMessages = messageService.getRoomMessages(roomId, 2);
        assertEquals(2, limitedMessages.size());
    }

    @Test
    public void testGetMessagesBetweenUsers() {
        List<ChatMessage> messages = messageService.getMessagesBetweenUsers(senderId, receiverId);
        
        // 应该有5条直接消息
        assertEquals(5, messages.size());
    }

    @Test
    public void testSaveAndGetMessage() {
        String messageId = UUID.randomUUID().toString();
        ChatMessage message = ChatMessage.builder()
                .id(messageId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content("Test save and get")
                .timestamp(System.currentTimeMillis())
                .type(ChatMessage.MessageType.TEXT)
                .build();
        
        messageService.saveMessage(message);
        
        ChatMessage retrieved = messageService.getMessageById(messageId);
        assertNotNull(retrieved);
        assertEquals(message.getContent(), retrieved.getContent());
    }
    
    @Test
    public void testSendMessage() {
        ChatMessage message = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .senderId(senderId)
                .receiverId(receiverId)
                .content("Test send message")
                .timestamp(System.currentTimeMillis())
                .type(ChatMessage.MessageType.TEXT)
                .build();
        
        messageService.sendMessage(message);
        
        // 验证消息已保存
        ChatMessage saved = messageService.getMessageById(message.getId());
        assertNotNull(saved);
        assertEquals(message.getContent(), saved.getContent());
    }
}












