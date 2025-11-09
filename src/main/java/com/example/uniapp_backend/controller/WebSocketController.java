package com.example.uniapp_backend.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
public class WebSocketController {

    /**
     * 处理用户上线通知
     */
    @MessageMapping("/user.online")
    @SendTo("/topic/user.status")
    public Map<String, Object> userOnline(Map<String, Object> message) {
        Long userId = Long.valueOf(message.get("userId").toString());
        String username = (String) message.get("username");

        return Map.of(
                "type", "USER_ONLINE",
                "userId", userId,
                "username", username,
                "timestamp", LocalDateTime.now().toString(),
                "message", username + " 上线了"
        );
    }

    /**
     * 处理用户下线通知
     */
    @MessageMapping("/user.offline")
    @SendTo("/topic/user.status")
    public Map<String, Object> userOffline(Map<String, Object> message) {
        Long userId = Long.valueOf(message.get("userId").toString());
        String username = (String) message.get("username");

        return Map.of(
                "type", "USER_OFFLINE",
                "userId", userId,
                "username", username,
                "timestamp", LocalDateTime.now().toString(),
                "message", username + " 下线了"
        );
    }

    /**
     * 处理系统通知
     */
    @MessageMapping("/system.notification")
    @SendTo("/topic/notifications")
    public Map<String, Object> systemNotification(Map<String, Object> message) {
        String title = (String) message.get("title");
        String content = (String) message.get("content");
        String level = (String) message.get("level"); // INFO, WARNING, ERROR

        return Map.of(
                "type", "SYSTEM_NOTIFICATION",
                "title", title,
                "content", content,
                "level", level != null ? level : "INFO",
                "timestamp", LocalDateTime.now().toString()
        );
    }

    /**
     * 心跳检测
     */
    @MessageMapping("/heartbeat")
    @SendToUser("/queue/heartbeat")
    public Map<String, Object> heartbeat(Map<String, Object> message) {
        Long userId = Long.valueOf(message.get("userId").toString());

        return Map.of(
                "type", "HEARTBEAT_RESPONSE",
                "userId", userId,
                "timestamp", System.currentTimeMillis(),
                "status", "OK"
        );
    }
}