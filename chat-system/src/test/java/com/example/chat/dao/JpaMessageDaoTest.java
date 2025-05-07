package com.example.chat.dao;

import com.example.chat.dao.impl.JpaMessageDao;
import com.example.chat.model.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class JpaMessageDaoTest {

    @Autowired
    private JpaMessageDao messageDao;

    @Test
    void testSaveAndFindById() {
        ChatMessage message = ChatMessage.builder()
                .senderId("user1")
                .content("Hello")
                .timestamp(System.currentTimeMillis())
                .type(ChatMessage.MessageType.TEXT)
                .build();

        ChatMessage saved = messageDao.save(message);
        ChatMessage found = messageDao.findById(saved.getId());

        assertNotNull(found);
        assertEquals("user1", found.getSenderId());
    }

    @Test
    void testFindBetweenUsers() {
        // 创建测试数据
        messageDao.save(createPrivateMessage("user1", "user2", "Hi"));
        messageDao.save(createPrivateMessage("user2", "user1", "Hello"));

        List<ChatMessage> messages = messageDao.findBetweenUsers("user1", "user2", 10, 0);
        assertEquals(2, messages.size());
        assertEquals("Hi", messages.get(0).getContent());
    }

    private ChatMessage createPrivateMessage(String sender, String receiver, String content) {
        return ChatMessage.builder()
                .senderId(sender)
                .receiverId(receiver)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .type(ChatMessage.MessageType.TEXT)
                .build();
    }
}


