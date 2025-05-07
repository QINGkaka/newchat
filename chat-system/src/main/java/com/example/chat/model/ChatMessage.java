package com.example.chat.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_message")
public class ChatMessage {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;  // 移除手动ID设置

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "receiver_id")
    private String receiverId;

    @Column(name = "room_id")
    private String roomId;

    @Lob
    private String content;
    private String image;
    private long timestamp;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    @Convert(converter = StringSetConverter.class)
    @Column(name = "read_by")
    @Builder.Default
    private Set<String> readBy = new HashSet<>();
    
    public enum MessageType {
        TEXT,
        IMAGE,
        FILE,
        SYSTEM
    }
    
    /**
     * 判断消息是否为私聊消息
     */
    public boolean isPrivate() {
        return receiverId != null && !receiverId.isEmpty();
    }
    
    /**
     * 标记消息为指定用户已读
     */
    public void markAsRead(String userId) {
        if (readBy == null) {
            readBy = new HashSet<>();
        }
        readBy.add(userId);
    }
    
    /**
     * 检查消息是否被指定用户已读
     */
    public boolean isRead(String userId) {
        return readBy != null && readBy.contains(userId);
    }
}
@Converter
class StringSetConverter implements AttributeConverter<Set<String>, String> {
    private static final String SPLIT_CHAR = ",";

    @Override
    public String convertToDatabaseColumn(Set<String> stringSet) {
        return stringSet != null ? String.join(SPLIT_CHAR, stringSet) : "";
    }

    @Override
    public Set<String> convertToEntityAttribute(String string) {
        return string != null ? new HashSet<>(Arrays.asList(string.split(SPLIT_CHAR))) : new HashSet<>();
    }
}