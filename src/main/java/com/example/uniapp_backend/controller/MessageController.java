package com.example.uniapp_backend.controller;

import com.example.uniapp_backend.entity.Message;
import com.example.uniapp_backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    // 获取会话消息历史
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getConversationMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdTime").descending());
            Page<Message> messages = messageService.getConversationMessages(conversationId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", messages.getContent());
            response.put("total", messages.getTotalElements());
            response.put("page", page);
            response.put("size", size);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 获取未读消息数量
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(
            @RequestParam String conversationId,
            @RequestParam Long receiverId) {
        try {
            Long count = messageService.getUnreadCount(conversationId, receiverId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", count
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 标记消息为已读
    @PostMapping("/mark-read")
    public ResponseEntity<?> markMessagesAsRead(
            @RequestParam String conversationId,
            @RequestParam Long receiverId) {
        try {
            messageService.markConversationAsRead(conversationId, receiverId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "标记已读成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 撤回消息
    @PostMapping("/recall")
    public ResponseEntity<?> recallMessage(
            @RequestParam Long messageId,
            @RequestParam Long senderId) {
        try {
            boolean success = messageService.recallMessage(messageId, senderId);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "消息撤回成功"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "消息撤回失败"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 获取消息详情
    @GetMapping("/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable Long messageId) {
        try {
            Optional<Message> message = messageService.getMessageById(messageId);

            if (message.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", message.get()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "消息不存在"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}