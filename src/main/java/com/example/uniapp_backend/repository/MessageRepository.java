package com.example.uniapp_backend.repository;

import com.example.uniapp_backend.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 根据会话ID分页查询消息
    Page<Message> findByConversationIdOrderByCreatedTimeDesc(String conversationId, Pageable pageable);

    // 查询用户未读消息
    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);

    // 查询会话未读消息数量
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.receiverId = :receiverId AND m.isRead = false")
    Long countUnreadMessages(@Param("conversationId") String conversationId, @Param("receiverId") Long receiverId);

    // 标记消息为已读
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversationId = :conversationId AND m.receiverId = :receiverId AND m.isRead = false")
    void markMessagesAsRead(@Param("conversationId") String conversationId, @Param("receiverId") Long receiverId);

    // 撤回消息
    @Modifying
    @Query("UPDATE Message m SET m.isRecalled = true, m.recallTime = :recallTime WHERE m.id = :messageId AND m.senderId = :senderId")
    int recallMessage(@Param("messageId") Long messageId, @Param("senderId") Long senderId, @Param("recallTime") LocalDateTime recallTime);

    // 查询群组消息
    Page<Message> findByGroupIdOrderByCreatedTimeDesc(Long groupId, Pageable pageable);
}