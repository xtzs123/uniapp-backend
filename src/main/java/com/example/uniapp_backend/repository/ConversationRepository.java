package com.example.uniapp_backend.repository;

import com.example.uniapp_backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // 根据用户ID查找对话
    List<Conversation> findByUserId(Long userId);

    // 根据用户ID按最后消息时间降序排列
    List<Conversation> findByUserIdOrderByLastMessageTimeDesc(Long userId);

    // 根据用户ID和对话ID查找对话
    Optional<Conversation> findByUserIdAndConversationId(Long userId, String conversationId);

    // 根据对话ID查找所有相关对话
    List<Conversation> findByConversationId(String conversationId);

    // 查找置顶的对话
    List<Conversation> findByUserIdAndIsTopTrueOrderByLastMessageTimeDesc(Long userId);

    // 统计用户的未读消息总数
    @Query("SELECT SUM(c.unreadCount) FROM Conversation c WHERE c.userId = :userId")
    Integer sumUnreadCountByUserId(@Param("userId") Long userId);

    // 根据用户ID和类型查找对话
    List<Conversation> findByUserIdAndType(Long userId, String type);

    // 统计对话的成员数量
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.conversationId = :conversationId")
    Long countByConversationId(@Param("conversationId") String conversationId);

    // 根据对话ID和类型查找对话
    List<Conversation> findByConversationIdAndType(String conversationId, String type);

    // 删除用户的所有对话
    void deleteByUserId(Long userId);

    // 根据对话ID删除所有相关对话
    void deleteByConversationId(String conversationId);
}