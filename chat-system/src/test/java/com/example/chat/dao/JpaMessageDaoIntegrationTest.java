package com.example.chat.dao;

import com.example.chat.dao.impl.JpaMessageDao;
import com.example.chat.model.ChatMessage;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// 完整示例测试类
@SpringBootTest
@Transactional
public class JpaMessageDaoIntegrationTest {
    @Autowired
    private JpaMessageDao messageDao;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Rollback(false) // 关闭回滚
    @Sql(statements = "DELETE FROM chat_message WHERE sender_id = 'testUser'") // 清理数据
    void testRealPersistence() {
        ChatMessage message = ChatMessage.builder()
                .senderId("testUser")
                .content("Persistence Test")
                .timestamp(System.currentTimeMillis())
                .type(ChatMessage.MessageType.TEXT)
                .build();

        // 保存操作
        ChatMessage saved = messageDao.save(message);

        // 强制刷新持久化上下文
        entityManager.flush();
        entityManager.clear();

        // 直接通过EntityManager查询
        ChatMessage dbMessage = entityManager.find(ChatMessage.class, saved.getId());
        assertNotNull(dbMessage);
        assertEquals("Persistence Test", dbMessage.getContent());
    }
}

