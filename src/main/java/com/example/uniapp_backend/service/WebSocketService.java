package com.example.uniapp_backend.service;

import com.example.uniapp_backend.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 发送消息给特定用户
     */
    public void sendToUser(Long userId, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/conversations",
                    message
            );
            log.debug("WebSocket消息发送成功 - 用户: {}, 类型: {}", userId, message.getType());
        } catch (Exception e) {
            log.error("WebSocket消息发送失败 - 用户: {}, 错误: {}", userId, e.getMessage());
        }
    }

    /**
     * 广播消息给所有用户
     */
    public void broadcast(WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/conversations", message);
            log.debug("WebSocket广播消息成功 - 类型: {}", message.getType());
        } catch (Exception e) {
            log.error("WebSocket广播消息失败 - 错误: {}", e.getMessage());
        }
    }

    /**
     * 发送对话更新通知
     */
    public void notifyConversationUpdate(Long userId, Object conversationData) {
        WebSocketMessage message = new WebSocketMessage(
                "CONVERSATION_UPDATE",
                conversationData,
                userId
        );
        sendToUser(userId, message);
    }

    /**
     * 发送对话删除通知
     */
    public void notifyConversationDelete(Long userId, String conversationId) {
        WebSocketMessage message = new WebSocketMessage(
                "CONVERSATION_DELETE",
                Map.of("conversationId", conversationId),
                userId
        );
        sendToUser(userId, message);
    }

    /**
     * 发送标记已读通知
     */
    public void notifyMarkAsRead(Long userId, String conversationId) {
        WebSocketMessage message = new WebSocketMessage(
                "CONVERSATION_READ",
                Map.of("conversationId", conversationId),
                userId
        );
        sendToUser(userId, message);
    }

    /**
     * 发送新消息通知
     */
    public void notifyNewMessage(Long userId, Object messageData) {
        WebSocketMessage message = new WebSocketMessage(
                "NEW_MESSAGE",
                messageData,
                userId
        );
        sendToUser(userId, message);
    }
}