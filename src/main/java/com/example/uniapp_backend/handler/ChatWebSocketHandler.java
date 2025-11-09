package com.example.uniapp_backend.handler;

import com.example.uniapp_backend.config.JwtTokenUtil;
import com.example.uniapp_backend.entity.Message;
import com.example.uniapp_backend.entity.GroupMember;
import com.example.uniapp_backend.entity.ChatGroup;
import com.example.uniapp_backend.service.MessageService;
import com.example.uniapp_backend.service.ChatGroupService;
import com.example.uniapp_backend.service.ConversationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 存储用户WebSocket会话
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatGroupService chatGroupService;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired
    public ChatWebSocketHandler(ConversationService conversationService,
                                MessageService messageService,
                                ChatGroupService chatGroupService,
                                JwtTokenUtil jwtTokenUtil) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.chatGroupService = chatGroupService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            System.out.println("用户 " + userId + " WebSocket连接建立，当前在线用户: " + userSessions.size());

            // 发送连接成功消息
            sendSystemMessage(session, "连接成功", "success");

            // 连接建立后立即发送对话列表
            sendConversationList(userId);
        } else {
            // 认证失败，关闭连接
            System.out.println("WebSocket连接认证失败，关闭连接");
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("收到WebSocket消息: " + payload);

        // 心跳检测
        if ("ping".equals(payload) || "heartbeat".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        try {
            // 使用类型安全的方式解析消息
            Map<String, Object> messageData = objectMapper.readValue(payload,
                    new TypeReference<Map<String, Object>>() {});
            String type = (String) messageData.get("type");
            Long userId = getUserIdFromSession(session);

            if (userId == null) {
                sendError(session, "用户未认证");
                return;
            }

            if (type == null) {
                sendError(session, "消息类型不能为空");
                return;
            }

            switch (type) {
                case "get_conversation_list":
                    sendConversationList(userId);
                    break;
                case "mark_as_read":
                    handleMarkAsRead(userId, messageData);
                    break;
                case "set_top":
                    handleSetTop(userId, messageData);
                    break;
                case "send_message":
                    handleSendMessage(userId, messageData);
                    break;
                case "recall_message":
                    handleRecallMessage(userId, messageData);
                    break;
                case "create_group":
                    handleCreateGroup(userId, messageData);
                    break;
                case "join_group":
                    handleJoinGroup(userId, messageData);
                    break;
                default:
                    System.out.println("未知消息类型: " + type);
                    sendError(session, "未知消息类型: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(session, "消息格式错误: " + e.getMessage());
        }
    }

    // 获取用户ID从WebSocket会话（支持Token认证）- 使用新的JWT工具类
    private Long getUserIdFromSession(WebSocketSession session) {
        try {
            // 首先尝试从查询参数获取token
            String query = session.getUri().getQuery();
            if (query != null && query.contains("token=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        String token = param.substring(6);
                        if (jwtTokenUtil.validateToken(token)) {
                            // 使用新的方法检查Token类型，只允许用户Token连接WebSocket
                            String userType = jwtTokenUtil.getTypeFromToken(token);
                            if ("USER".equals(userType)) {
                                return jwtTokenUtil.getIdFromToken(token);
                            } else {
                                System.out.println("拒绝管理员Token连接WebSocket");
                                return null;
                            }
                        }
                    }
                }
            }

            // 备用方案：从URL参数获取userId（仅用于测试）
            if (query != null && query.contains("userId=")) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("userId=")) {
                        try {
                            return Long.parseLong(param.substring(7));
                        } catch (NumberFormatException e) {
                            System.out.println("用户ID格式错误: " + param);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 修复后的处理发送消息方法
    @SuppressWarnings("unchecked")
    private void handleSendMessage(Long userId, Map<String, Object> messageData) {
        try {
            // 数据验证
            if (messageData == null) {
                sendErrorToUser(userId, "消息数据不能为空");
                return;
            }

            String conversationId = (String) messageData.get("conversationId");
            String content = (String) messageData.get("content");
            String messageType = (String) messageData.get("messageType");

            // 验证必要字段
            if (conversationId == null || conversationId.trim().isEmpty()) {
                sendErrorToUser(userId, "会话ID不能为空");
                return;
            }
            if (content == null || content.trim().isEmpty()) {
                sendErrorToUser(userId, "消息内容不能为空");
                return;
            }
            if (messageType == null) {
                sendErrorToUser(userId, "消息类型不能为空");
                return;
            }

            // 安全获取目标用户ID
            Long targetUserId = null;
            if (messageData.containsKey("targetUserId") && messageData.get("targetUserId") != null) {
                try {
                    targetUserId = Long.valueOf(messageData.get("targetUserId").toString());
                } catch (NumberFormatException e) {
                    System.out.println("目标用户ID格式错误: " + messageData.get("targetUserId"));
                    sendErrorToUser(userId, "目标用户ID格式错误");
                    return;
                }
            }

            // 安全获取群组ID
            Long groupId = null;
            if (messageData.containsKey("groupId") && messageData.get("groupId") != null) {
                try {
                    groupId = Long.valueOf(messageData.get("groupId").toString());
                } catch (NumberFormatException e) {
                    System.out.println("群组ID格式错误: " + messageData.get("groupId"));
                    sendErrorToUser(userId, "群组ID格式错误");
                    return;
                }
            }

            // 验证消息类型和目标
            if (targetUserId == null && groupId == null) {
                sendErrorToUser(userId, "必须指定目标用户或群组");
                return;
            }

            System.out.println("用户 " + userId + " 发送消息到会话: " + conversationId + ", 内容: " + content);

            // 保存消息到数据库
            Message message = new Message();
            message.setConversationId(conversationId);
            message.setSenderId(userId);
            message.setReceiverId(targetUserId);
            message.setGroupId(groupId);
            message.setContent(content);

            try {
                message.setMessageType(Message.MessageType.valueOf(messageType.toUpperCase()));
            } catch (IllegalArgumentException e) {
                sendErrorToUser(userId, "不支持的消息类型: " + messageType);
                return;
            }

            Message savedMessage = messageService.saveMessage(message);
            if (savedMessage == null) {
                sendErrorToUser(userId, "消息保存失败");
                return;
            }

            // 发送成功响应给发送者
            sendMessageSentResponse(userId, conversationId, content, savedMessage.getId());

            // 如果指定了目标用户，则转发消息给目标用户
            if (targetUserId != null) {
                sendNewMessageNotification(targetUserId, createMessageInfo(savedMessage));
            }

            // 如果是群消息，发送给所有群成员
            if (groupId != null) {
                sendGroupMessage(groupId, savedMessage, userId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("处理发送消息失败: " + e.getMessage());
            sendErrorToUser(userId, "消息发送失败: " + e.getMessage());
        }
    }

    // 修复后的处理撤回消息
    @SuppressWarnings("unchecked")
    private void handleRecallMessage(Long userId, Map<String, Object> messageData) {
        try {
            if (messageData == null || !messageData.containsKey("messageId")) {
                sendErrorToUser(userId, "消息ID不能为空");
                return;
            }

            Long messageId;
            try {
                messageId = Long.valueOf(messageData.get("messageId").toString());
            } catch (NumberFormatException e) {
                sendErrorToUser(userId, "消息ID格式错误");
                return;
            }

            boolean success = messageService.recallMessage(messageId, userId);
            if (success) {
                // 通知相关用户消息已撤回
                Map<String, Object> recallInfo = new HashMap<>();
                recallInfo.put("type", "message_recalled");
                recallInfo.put("messageId", messageId);
                recallInfo.put("recalledBy", userId);
                recallInfo.put("timestamp", getCurrentTime());

                // 这里可以根据消息类型（私聊/群聊）发送给相关用户
                sendRecallNotification(recallInfo);

                System.out.println("用户 " + userId + " 撤回消息: " + messageId);

                // 发送成功响应
                Map<String, Object> response = new HashMap<>();
                response.put("type", "recall_success");
                response.put("messageId", messageId);
                sendJsonMessage(userId, response);
            } else {
                sendErrorToUser(userId, "撤回消息失败：无权限或消息不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorToUser(userId, "撤回消息失败: " + e.getMessage());
        }
    }

    // 修复后的处理创建群组
    @SuppressWarnings("unchecked")
    private void handleCreateGroup(Long userId, Map<String, Object> messageData) {
        try {
            if (messageData == null) {
                sendErrorToUser(userId, "群组数据不能为空");
                return;
            }

            String groupName = (String) messageData.get("groupName");
            String description = (String) messageData.get("description");

            if (groupName == null || groupName.trim().isEmpty()) {
                sendErrorToUser(userId, "群组名称不能为空");
                return;
            }

            ChatGroup group = new ChatGroup();
            group.setGroupName(groupName);
            group.setDescription(description);
            group.setCreatorId(userId);

            ChatGroup createdGroup = chatGroupService.createGroup(group, userId);

            // 返回创建成功的响应
            Map<String, Object> response = new HashMap<>();
            response.put("type", "group_created");
            response.put("groupId", createdGroup.getId());
            response.put("groupName", createdGroup.getGroupName());
            response.put("timestamp", getCurrentTime());

            sendJsonMessage(userId, response);

            // 创建群组后更新对话列表
            sendConversationList(userId);

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorToUser(userId, "创建群组失败: " + e.getMessage());
        }
    }

    // 修复后的处理加入群组
    @SuppressWarnings("unchecked")
    private void handleJoinGroup(Long userId, Map<String, Object> messageData) {
        try {
            if (messageData == null || !messageData.containsKey("groupId")) {
                sendErrorToUser(userId, "群组ID不能为空");
                return;
            }

            Long groupId;
            try {
                groupId = Long.valueOf(messageData.get("groupId").toString());
            } catch (NumberFormatException e) {
                sendErrorToUser(userId, "群组ID格式错误");
                return;
            }

            chatGroupService.addGroupMember(groupId, userId, GroupMember.GroupRole.MEMBER);

            // 返回加入成功的响应
            Map<String, Object> response = new HashMap<>();
            response.put("type", "group_joined");
            response.put("groupId", groupId);
            response.put("timestamp", getCurrentTime());

            sendJsonMessage(userId, response);

            // 发送群组列表更新
            sendConversationList(userId);

        } catch (Exception e) {
            e.printStackTrace();
            sendErrorToUser(userId, "加入群组失败: " + e.getMessage());
        }
    }

    // 修复后的标记对话为已读
    @SuppressWarnings("unchecked")
    private void handleMarkAsRead(Long userId, Map<String, Object> messageData) {
        try {
            if (messageData == null || !messageData.containsKey("conversationId")) {
                sendErrorToUser(userId, "会话ID不能为空");
                return;
            }

            String conversationId = (String) messageData.get("conversationId");
            if (conversationId == null || conversationId.trim().isEmpty()) {
                sendErrorToUser(userId, "会话ID不能为空");
                return;
            }

            conversationService.markConversationAsRead(conversationId, userId);
            System.out.println("用户 " + userId + " 标记对话 " + conversationId + " 为已读");

            // 更新后重新发送对话列表
            sendConversationList(userId);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("标记对话为已读失败: " + e.getMessage());
            sendErrorToUser(userId, "标记已读失败: " + e.getMessage());
        }
    }

    // 修复后的设置对话置顶
    @SuppressWarnings("unchecked")
    private void handleSetTop(Long userId, Map<String, Object> messageData) {
        try {
            if (messageData == null) {
                sendErrorToUser(userId, "置顶数据不能为空");
                return;
            }

            String conversationId = (String) messageData.get("conversationId");
            Boolean isTop = (Boolean) messageData.get("isTop");

            if (conversationId == null || conversationId.trim().isEmpty()) {
                sendErrorToUser(userId, "会话ID不能为空");
                return;
            }
            if (isTop == null) {
                sendErrorToUser(userId, "置顶状态不能为空");
                return;
            }

            // 修复：使用ConversationService中新增的setConversationTop方法
            conversationService.setConversationTop(conversationId, userId, isTop);
            System.out.println("用户 " + userId + " 设置对话 " + conversationId + " 置顶状态为: " + isTop);

            // 更新后重新发送对话列表
            sendConversationList(userId);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("设置对话置顶失败: " + e.getMessage());
            sendErrorToUser(userId, "设置置顶失败: " + e.getMessage());
        }
    }

    // 修复后的发送群消息给所有成员
    private void sendGroupMessage(Long groupId, Message message, Long excludeUserId) {
        try {
            if (groupId == null) {
                System.out.println("群组ID为空，无法发送群消息");
                return;
            }

            List<GroupMember> members = chatGroupService.getGroupMembers(groupId);
            if (members == null || members.isEmpty()) {
                System.out.println("群组 " + groupId + " 没有成员");
                return;
            }

            Map<String, Object> messageInfo = createMessageInfo(message);

            int successCount = 0;
            int totalCount = members.size();

            for (GroupMember member : members) {
                try {
                    if (!member.getUserId().equals(excludeUserId)) {
                        sendNewMessageNotification(member.getUserId(), messageInfo);
                        successCount++;
                    }
                } catch (Exception e) {
                    System.out.println("向群成员 " + member.getUserId() + " 发送消息失败: " + e.getMessage());
                }
            }

            System.out.println("群消息发送完成: 成功 " + successCount + "/" + (totalCount - 1) + " 个成员");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("发送群消息失败: " + e.getMessage());
        }
    }

    // 修复后的创建消息信息对象
    private Map<String, Object> createMessageInfo(Message message) {
        Map<String, Object> messageInfo = new HashMap<>();
        messageInfo.put("type", "new_message");
        messageInfo.put("messageId", message.getId());
        messageInfo.put("fromUserId", message.getSenderId());
        messageInfo.put("content", message.getContent());
        messageInfo.put("conversationId", message.getConversationId());
        messageInfo.put("groupId", message.getGroupId());
        messageInfo.put("messageType", message.getMessageType().name().toLowerCase());

        // 安全处理时间戳
        if (message.getCreatedTime() != null) {
            messageInfo.put("timestamp", message.getCreatedTime().toString());
        } else {
            messageInfo.put("timestamp", Instant.now().toString());
        }

        return messageInfo;
    }

    // 发送撤回通知
    private void sendRecallNotification(Map<String, Object> recallInfo) {
        broadcastToAllOnlineUsers(recallInfo);
    }

    // 广播给所有在线用户
    private void broadcastToAllOnlineUsers(Map<String, Object> message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            int sentCount = 0;
            for (WebSocketSession session : userSessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(jsonMessage));
                        sentCount++;
                    } catch (Exception e) {
                        System.out.println("广播消息发送失败: " + e.getMessage());
                    }
                }
            }
            System.out.println("广播消息完成，成功发送给 " + sentCount + " 个用户");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 发送JSON消息到指定用户
    private void sendJsonMessage(Long userId, Map<String, Object> message) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                String jsonResponse = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonResponse));
            } else {
                System.out.println("用户 " + userId + " 不在线，无法发送消息");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 发送系统消息
    private void sendSystemMessage(WebSocketSession session, String message, String level) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "system");
            response.put("message", message);
            response.put("level", level);
            response.put("timestamp", getCurrentTime());

            String jsonResponse = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(jsonResponse));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 发送对话列表
    private void sendConversationList(Long userId) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                // 修复：使用ConversationService中新增的重载方法
                List<Map<String, Object>> conversationList = conversationService.getUserConversationList(userId);

                Map<String, Object> response = new HashMap<>();
                response.put("type", "conversation_list");
                response.put("data", conversationList);
                response.put("timestamp", getCurrentTime());

                String jsonResponse = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(jsonResponse));
                System.out.println("向用户 " + userId + " 发送对话列表");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("发送对话列表失败: " + e.getMessage());
        }
    }

    // 发送错误消息到会话
    private void sendError(WebSocketSession session, String error) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "error");
            response.put("message", error);
            response.put("timestamp", getCurrentTime());

            String jsonResponse = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(jsonResponse));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 发送错误消息到用户ID
    private void sendErrorToUser(Long userId, String error) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null) {
            sendError(session, error);
        } else {
            System.out.println("用户 " + userId + " 不在线，无法发送错误消息: " + error);
        }
    }

    // 发送消息发送成功的响应
    private void sendMessageSentResponse(Long userId, String conversationId, String content, Long messageId) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "message_sent");
                response.put("conversationId", conversationId);
                response.put("content", content);
                response.put("messageId", messageId);
                response.put("timestamp", getCurrentTime());

                String jsonResponse = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(jsonResponse));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 发送新消息通知
    public void sendNewMessageNotification(Long userId, Map<String, Object> messageInfo) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                String jsonResponse = objectMapper.writeValueAsString(messageInfo);
                session.sendMessage(new TextMessage(jsonResponse));
                System.out.println("向用户 " + userId + " 发送新消息: " + jsonResponse);
            } else {
                System.out.println("用户 " + userId + " 不在线，无法发送消息");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("WebSocket传输错误: " + exception.getMessage());
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("用户 " + userId + " 因传输错误被移除，当前在线用户: " + userSessions.size());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            System.out.println("用户 " + userId + " WebSocket连接关闭，当前在线用户: " + userSessions.size());
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    // 获取在线用户数量（用于监控）
    public int getOnlineUserCount() {
        return userSessions.size();
    }

    // 检查用户是否在线
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
}