package com.example.uniapp_backend.service;

import com.example.uniapp_backend.entity.Conversation;
import com.example.uniapp_backend.repository.ConversationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    // 获取用户对话列表（返回实体列表）
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findByUserIdOrderByLastMessageTimeDesc(userId);
    }

    // 获取用户对话列表（返回 Map 列表，用于 WebSocket）
    public List<Map<String, Object>> getUserConversationList(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByLastMessageTimeDesc(userId);

        // 将 Conversation 实体转换为 Map
        return conversations.stream().map(conversation -> {
            Map<String, Object> conversationMap = new HashMap<>();
            conversationMap.put("id", conversation.getId());
            conversationMap.put("conversationId", conversation.getConversationId());
            conversationMap.put("userId", conversation.getUserId());
            conversationMap.put("type", conversation.getType());
            conversationMap.put("targetId", conversation.getTargetId());
            conversationMap.put("name", conversation.getName());
            conversationMap.put("avatar", conversation.getAvatar());
            conversationMap.put("lastMessage", conversation.getLastMessage());
            conversationMap.put("unreadCount", conversation.getUnreadCount());
            conversationMap.put("isTop", conversation.getIsTop());
            conversationMap.put("lastMessageTime", conversation.getLastMessageTime());
            conversationMap.put("memberCount", conversation.getMemberCount());
            conversationMap.put("createTime", conversation.getCreateTime());
            conversationMap.put("updateTime", conversation.getUpdateTime());
            return conversationMap;
        }).collect(Collectors.toList());
    }

    // 标记对话为已读
    @Transactional
    public void markAsRead(Long userId, String conversationId) {
        Conversation conversation = conversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));

        conversation.setUnreadCount(0);
        conversationRepository.save(conversation);
        log.info("用户 {} 标记对话 {} 为已读", userId, conversationId);
    }

    // WebSocket 专用的标记已读方法
    @Transactional
    public void markConversationAsRead(String conversationId, Long userId) {
        markAsRead(userId, conversationId);
    }

    // 删除对话
    @Transactional
    public void deleteConversation(Long userId, String conversationId) {
        Conversation conversation = conversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));

        conversationRepository.delete(conversation);
        log.info("用户 {} 删除对话 {}", userId, conversationId);
    }

    // 设置对话置顶
    @Transactional
    public void setTop(Long userId, String conversationId, Boolean isTop) {
        Conversation conversation = conversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));

        conversation.setIsTop(isTop);
        conversationRepository.save(conversation);
        log.info("用户 {} {} 对话 {}", userId, isTop ? "置顶" : "取消置顶", conversationId);
    }

    // WebSocket 专用的设置置顶方法
    @Transactional
    public void setConversationTop(String conversationId, Long userId, Boolean isTop) {
        setTop(userId, conversationId, isTop);
    }

    // 获取或创建私聊对话
    public String getOrCreatePrivateConversation(Long user1Id, Long user2Id) {
        String conversationId = "conversation_" + Math.min(user1Id, user2Id) + "_" + Math.max(user1Id, user2Id);

        // 检查对话是否已存在
        List<Conversation> existingConversations = conversationRepository.findByConversationId(conversationId);
        if (existingConversations.isEmpty()) {
            // 创建新的对话
            createPrivateConversation(user1Id, user2Id, conversationId);
        }

        return conversationId;
    }

    // 创建私聊对话
    private void createPrivateConversation(Long user1Id, Long user2Id, String conversationId) {
        // 为用户1创建对话
        Conversation conversation1 = new Conversation();
        conversation1.setUserId(user1Id);
        conversation1.setConversationId(conversationId);
        conversation1.setType("FRIEND");
        conversation1.setTargetId(user2Id.toString());
        conversation1.setName(getUserName(user2Id)); // 需要实现获取用户名的方法
        conversation1.setUnreadCount(0);
        conversation1.setIsTop(false);
        conversation1.setLastMessageTime(LocalDateTime.now());
        conversationRepository.save(conversation1);

        // 为用户2创建对话
        Conversation conversation2 = new Conversation();
        conversation2.setUserId(user2Id);
        conversation2.setConversationId(conversationId);
        conversation2.setType("FRIEND");
        conversation2.setTargetId(user1Id.toString());
        conversation2.setName(getUserName(user1Id)); // 需要实现获取用户名的方法
        conversation2.setUnreadCount(0);
        conversation2.setIsTop(false);
        conversation2.setLastMessageTime(LocalDateTime.now());
        conversationRepository.save(conversation2);

        log.info("创建私聊对话: {} (用户 {} 和 {})", conversationId, user1Id, user2Id);
    }

    // 获取用户名（需要根据您的用户服务实现）
    private String getUserName(Long userId) {
        // 这里需要调用用户服务来获取用户名
        // 暂时返回用户ID作为名称
        return "用户" + userId;
    }

    // 更新对话的最后消息
    @Transactional
    public void updateLastMessage(String conversationId, String lastMessage, Long senderId) {
        List<Conversation> conversations = conversationRepository.findByConversationId(conversationId);
        for (Conversation conversation : conversations) {
            conversation.setLastMessage(lastMessage);
            conversation.setLastMessageTime(LocalDateTime.now());
            // 如果发送者不是当前用户，增加未读计数
            if (!conversation.getUserId().equals(senderId)) {
                conversation.setUnreadCount(conversation.getUnreadCount() + 1);
            }
            conversationRepository.save(conversation);
        }
        log.info("更新对话 {} 的最后消息: {}", conversationId, lastMessage);
    }

    // 获取对话详情
    public Conversation getConversationDetail(Long userId, String conversationId) {
        return conversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
    }

    // 获取用户的未读消息总数
    public Integer getTotalUnreadCount(Long userId) {
        Integer totalUnread = conversationRepository.sumUnreadCountByUserId(userId);
        return totalUnread != null ? totalUnread : 0;
    }

    // 获取置顶对话列表
    public List<Conversation> getTopConversations(Long userId) {
        return conversationRepository.findByUserIdAndIsTopTrueOrderByLastMessageTimeDesc(userId);
    }

    // 根据类型获取对话
    public List<Conversation> getConversationsByType(Long userId, String type) {
        return conversationRepository.findByUserIdAndType(userId, type);
    }

    // 创建群组对话
    @Transactional
    public void createGroupConversation(Long groupId, String groupName, List<Long> memberIds) {
        String conversationId = "group_" + groupId;

        for (Long memberId : memberIds) {
            Conversation conversation = new Conversation();
            conversation.setUserId(memberId);
            conversation.setConversationId(conversationId);
            conversation.setType("GROUP");
            conversation.setTargetId(groupId.toString());
            conversation.setName(groupName);
            conversation.setUnreadCount(0);
            conversation.setIsTop(false);
            conversation.setLastMessageTime(LocalDateTime.now());
            conversation.setMemberCount(memberIds.size());
            conversationRepository.save(conversation);
        }

        log.info("创建群组对话: {} (群组ID: {})", conversationId, groupId);
    }

    // 添加用户到群组对话
    @Transactional
    public void addUserToGroupConversation(Long groupId, Long userId, String groupName) {
        String conversationId = "group_" + groupId;

        // 检查是否已经存在
        boolean exists = conversationRepository.findByUserIdAndConversationId(userId, conversationId).isPresent();
        if (!exists) {
            Conversation conversation = new Conversation();
            conversation.setUserId(userId);
            conversation.setConversationId(conversationId);
            conversation.setType("GROUP");
            conversation.setTargetId(groupId.toString());
            conversation.setName(groupName);
            conversation.setUnreadCount(0);
            conversation.setIsTop(false);
            conversation.setLastMessageTime(LocalDateTime.now());

            // 更新成员数量
            Long memberCount = conversationRepository.countByConversationId(conversationId);
            conversation.setMemberCount(memberCount != null ? memberCount.intValue() + 1 : 1);

            conversationRepository.save(conversation);
            log.info("添加用户 {} 到群组对话: {}", userId, conversationId);
        }
    }

    // 从群组对话中移除用户
    @Transactional
    public void removeUserFromGroupConversation(Long groupId, Long userId) {
        String conversationId = "group_" + groupId;

        Conversation conversation = conversationRepository.findByUserIdAndConversationId(userId, conversationId)
                .orElse(null);

        if (conversation != null) {
            conversationRepository.delete(conversation);
            log.info("从群组对话 {} 中移除用户 {}", conversationId, userId);

            // 更新剩余成员的成员数量
            List<Conversation> remainingConversations = conversationRepository.findByConversationId(conversationId);
            for (Conversation conv : remainingConversations) {
                conv.setMemberCount(remainingConversations.size());
                conversationRepository.save(conv);
            }
        }
    }

    // 更新群组对话信息
    @Transactional
    public void updateGroupConversation(Long groupId, String groupName, String avatar) {
        String conversationId = "group_" + groupId;
        List<Conversation> conversations = conversationRepository.findByConversationId(conversationId);

        for (Conversation conversation : conversations) {
            if (groupName != null) {
                conversation.setName(groupName);
            }
            if (avatar != null) {
                conversation.setAvatar(avatar);
            }
            conversationRepository.save(conversation);
        }

        log.info("更新群组对话 {} 的信息", conversationId);
    }
}