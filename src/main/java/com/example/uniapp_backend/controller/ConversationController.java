package com.example.uniapp_backend.controller;

import com.example.uniapp_backend.entity.Conversation;
import com.example.uniapp_backend.service.ConversationService;
import com.example.uniapp_backend.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    // 获取对话列表 - 修复：使用正确的返回类型
    @GetMapping
    public ApiResponse<?> getConversations(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            // 修复：使用返回 List<Conversation> 的方法
            List<Conversation> conversations = conversationService.getUserConversations(userId);
            return ApiResponse.success(conversations);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取对话列表失败: " + e.getMessage());
        }
    }

    // 标记对话已读
    @PostMapping("/{conversationId}/read")
    public ApiResponse<?> markAsRead(@PathVariable String conversationId,
                                     HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            conversationService.markAsRead(userId, conversationId);
            return ApiResponse.success(null, "标记已读成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "标记已读失败: " + e.getMessage());
        }
    }

    // 删除对话
    @DeleteMapping("/{conversationId}")
    public ApiResponse<?> deleteConversation(@PathVariable String conversationId,
                                             HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            conversationService.deleteConversation(userId, conversationId);
            return ApiResponse.success(null, "删除对话成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "删除对话失败: " + e.getMessage());
        }
    }

    // 置顶对话
    @PostMapping("/{conversationId}/top")
    public ApiResponse<?> setTop(@PathVariable String conversationId,
                                 @RequestParam Boolean isTop,
                                 HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            conversationService.setTop(userId, conversationId, isTop);
            String message = isTop ? "置顶成功" : "取消置顶成功";
            return ApiResponse.success(null, message);
        } catch (Exception e) {
            return ApiResponse.error(500, "操作失败: " + e.getMessage());
        }
    }

    // 获取对话详情
    @GetMapping("/{conversationId}")
    public ApiResponse<?> getConversationDetail(@PathVariable String conversationId,
                                                HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            Conversation conversation = conversationService.getConversationDetail(userId, conversationId);
            return ApiResponse.success(conversation);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取对话详情失败: " + e.getMessage());
        }
    }

    // 获取置顶对话列表
    @GetMapping("/top")
    public ApiResponse<?> getTopConversations(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            List<Conversation> topConversations = conversationService.getTopConversations(userId);
            return ApiResponse.success(topConversations);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取置顶对话失败: " + e.getMessage());
        }
    }

    // 获取未读消息总数
    @GetMapping("/unread-count")
    public ApiResponse<?> getUnreadCount(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            Integer unreadCount = conversationService.getTotalUnreadCount(userId);
            return ApiResponse.success(unreadCount);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取未读消息数失败: " + e.getMessage());
        }
    }

    // 根据类型获取对话
    @GetMapping("/type/{type}")
    public ApiResponse<?> getConversationsByType(@PathVariable String type,
                                                 HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            List<Conversation> conversations = conversationService.getConversationsByType(userId, type);
            return ApiResponse.success(conversations);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取对话失败: " + e.getMessage());
        }
    }
}