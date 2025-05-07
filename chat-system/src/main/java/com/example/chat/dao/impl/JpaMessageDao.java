package com.example.chat.dao.impl;
import com.example.chat.dao.MessageDao;

import com.example.chat.model.ChatMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JpaMessageDao implements MessageDao {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ChatMessage save(ChatMessage message) {
        // 移除手动设置ID的逻辑
        if (message.getId() == null) {
            entityManager.persist(message);  // 新增对象使用persist
            return message;
        } else {
            return entityManager.merge(message);  // 已存在对象使用merge
        }
    }

    @Override
    public ChatMessage findById(String id) {
        return entityManager.find(ChatMessage.class, id);
    }

    @Override
    public List<ChatMessage> findByRoomId(String roomId) {
        return entityManager.createQuery(
                        "SELECT m FROM ChatMessage m WHERE m.roomId = :roomId ORDER BY m.timestamp ASC",
                        ChatMessage.class)
                .setParameter("roomId", roomId)
                .getResultList();
    }

    @Override
    public List<ChatMessage> findBetweenUsers(String userId1, String userId2, int limit, long offset) {
        return entityManager.createQuery(
                        "SELECT m FROM ChatMessage m WHERE m.roomId IS NULL " +
                                "AND ((m.senderId = :user1 AND m.receiverId = :user2) " +
                                "OR (m.senderId = :user2 AND m.receiverId = :user1)) " +
                                "ORDER BY m.timestamp ASC",
                        ChatMessage.class)
                .setParameter("user1", userId1)
                .setParameter("user2", userId2)
                .setFirstResult((int) offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public boolean delete(String id) {
        ChatMessage message = findById(id);
        if (message != null) {
            entityManager.remove(message);
            return true;
        }
        return false;
    }
}

