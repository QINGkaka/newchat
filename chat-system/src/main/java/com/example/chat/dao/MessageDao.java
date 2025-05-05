package com.example.chat.dao;

import com.example.chat.model.ChatMessage;
import java.util.List;

public interface MessageDao {
    
    /**
     * 保存消息
     * @param message 要保存的消息
     * @return 保存后的消息（可能包含生成的ID等）
     */
    ChatMessage save(ChatMessage message);
    
    /**
     * 根据ID查找消息
     * @param id 消息ID
     * @return 找到的消息，如果不存在则返回null
     */
    ChatMessage findById(String id);
    
    /**
     * 查找指定房间的消息
     * @param roomId 房间ID
     * @return 该房间的所有消息
     */
    List<ChatMessage> findByRoomId(String roomId);
    
    /**
     * 查找两个用户之间的私聊消息
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @param limit 最大消息数量
     * @param offset 偏移量（用于分页）
     * @return 两个用户之间的消息列表
     */
    List<ChatMessage> findBetweenUsers(String userId1, String userId2, int limit, long offset);
    
    /**
     * 删除消息
     * @param id 要删除的消息ID
     * @return 是否删除成功
     */
    boolean delete(String id);
}
