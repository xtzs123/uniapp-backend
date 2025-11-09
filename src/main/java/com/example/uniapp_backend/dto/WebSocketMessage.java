package com.example.uniapp_backend.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class WebSocketMessage {
    private String type; // 消息类型：CONVERSATION_UPDATE, CONVERSATION_DELETE, MARK_READ, etc.
    private Object data;
    private Long userId;
    private LocalDateTime timestamp;

    public WebSocketMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public WebSocketMessage(String type, Object data, Long userId) {
        this();
        this.type = type;
        this.data = data;
        this.userId = userId;
    }
}