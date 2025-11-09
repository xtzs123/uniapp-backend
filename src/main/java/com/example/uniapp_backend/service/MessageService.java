package com.example.uniapp_backend.service;

import com.example.uniapp_backend.entity.Message;
import com.example.uniapp_backend.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    // 保存消息
    @Transactional
    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    // 获取会话消息历史
    public Page<Message> getConversationMessages(String conversationId, Pageable pageable) {
        return messageRepository.findByConversationIdOrderByCreatedTimeDesc(conversationId, pageable);
    }

    // 获取用户未读消息
    public List<Message> getUnreadMessages(Long receiverId) {
        return messageRepository.findByReceiverIdAndIsReadFalse(receiverId);
    }

    // 获取会话未读消息数量
    public Long getUnreadCount(String conversationId, Long receiverId) {
        return messageRepository.countUnreadMessages(conversationId, receiverId);
    }

    // 标记会话消息为已读
    @Transactional
    public void markConversationAsRead(String conversationId, Long receiverId) {
        messageRepository.markMessagesAsRead(conversationId, receiverId);
    }

    // 撤回消息
    @Transactional
    public boolean recallMessage(Long messageId, Long senderId) {
        int updated = messageRepository.recallMessage(messageId, senderId, LocalDateTime.now());
        return updated > 0;
    }

    // 根据ID获取消息
    public Optional<Message> getMessageById(Long messageId) {
        return messageRepository.findById(messageId);
    }

    // 获取群组消息
    public Page<Message> getGroupMessages(Long groupId, Pageable pageable) {
        return messageRepository.findByGroupIdOrderByCreatedTimeDesc(groupId, pageable);
    }
}