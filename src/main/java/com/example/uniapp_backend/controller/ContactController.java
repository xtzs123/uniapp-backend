package com.example.uniapp_backend.controller;

import com.example.uniapp_backend.entity.*;
import com.example.uniapp_backend.dto.*;
import com.example.uniapp_backend.service.ContactService;
import com.example.uniapp_backend.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {

    @Autowired
    private ContactService contactService;

    // 获取联系人列表
    @GetMapping
    public ApiResponse<?> getContacts(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            List<Contact> contacts = contactService.getUserContacts(userId);
            return ApiResponse.success(contacts);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取联系人列表失败: " + e.getMessage());
        }
    }

    // 发送好友申请
    @PostMapping("/friend-request")
    public ApiResponse<?> sendFriendRequest(@Valid @RequestBody AddFriendRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            FriendRequest friendRequest = contactService.sendFriendRequest(userId, request);
            return ApiResponse.success(friendRequest, "好友申请发送成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "发送好友申请失败: " + e.getMessage());
        }
    }

    // 处理好友申请
    @PostMapping("/handle-friend-request")
    public ApiResponse<?> handleFriendRequest(@Valid @RequestBody HandleFriendRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            contactService.handleFriendRequest(userId, request);
            String message = request.getAccepted() ? "已同意好友申请" : "已拒绝好友申请";
            return ApiResponse.success(null, message);
        } catch (Exception e) {
            return ApiResponse.error(500, "处理好友申请失败: " + e.getMessage());
        }
    }

    // 获取好友申请列表
    @GetMapping("/friend-requests")
    public ApiResponse<?> getFriendRequests(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            List<FriendRequest> friendRequests = contactService.getFriendRequests(userId);
            return ApiResponse.success(friendRequests);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取好友申请列表失败: " + e.getMessage());
        }
    }

    // 更新联系人信息
    @PutMapping("/update")
    public ApiResponse<?> updateContact(@Valid @RequestBody UpdateContactRequest request,
                                        HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            Contact contact = contactService.updateContact(userId, request);
            return ApiResponse.success(contact, "更新联系人成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "更新联系人失败: " + e.getMessage());
        }
    }

    // 删除联系人
    @DeleteMapping("/{contactId}")
    public ApiResponse<?> deleteContact(@PathVariable Long contactId,
                                        HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            contactService.deleteContact(userId, contactId);
            return ApiResponse.success(null, "删除联系人成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "删除联系人失败: " + e.getMessage());
        }
    }

    // 拉黑联系人
    @PostMapping("/{contactId}/block")
    public ApiResponse<?> blockContact(@PathVariable Long contactId,
                                       HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            Contact contact = contactService.blockContact(userId, contactId);
            return ApiResponse.success(contact, "拉黑联系人成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "拉黑联系人失败: " + e.getMessage());
        }
    }

    // 搜索用户
    @GetMapping("/search")
    public ApiResponse<?> searchUsers(@RequestParam String keyword,
                                      HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            if (keyword == null || keyword.trim().isEmpty()) {
                return ApiResponse.error(400, "搜索关键词不能为空");
            }

            List<User> users = contactService.searchUsers(userId, keyword.trim());
            return ApiResponse.success(users);
        } catch (Exception e) {
            return ApiResponse.error(500, "搜索用户失败: " + e.getMessage());
        }
    }

    // 发送消息给联系人
    @PostMapping("/send-message")
    public ApiResponse<?> sendMessageToContact(@Valid @RequestBody SendMessageRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            contactService.sendMessageToContact(userId, request.getContactId(),
                    request.getContent(), request.getMessageType());
            return ApiResponse.success(null, "发送消息成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "发送消息失败: " + e.getMessage());
        }
    }

    // 创建联系人分组
    @PostMapping("/groups")
    public ApiResponse<?> createContactGroup(@Valid @RequestBody CreateGroupRequest request,
                                             HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            ContactGroup group = contactService.createContactGroup(userId, request);
            return ApiResponse.success(group, "创建分组成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "创建分组失败: " + e.getMessage());
        }
    }

    // 获取联系人分组列表
    @GetMapping("/groups")
    public ApiResponse<?> getContactGroups(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            List<ContactGroup> groups = contactService.getUserContactGroups(userId);
            return ApiResponse.success(groups);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取分组列表失败: " + e.getMessage());
        }
    }

    // 删除联系人分组
    @DeleteMapping("/groups/{groupId}")
    public ApiResponse<?> deleteContactGroup(@PathVariable Long groupId,
                                             HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            contactService.deleteContactGroup(userId, groupId);
            return ApiResponse.success(null, "删除分组成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "删除分组失败: " + e.getMessage());
        }
    }

    // 移动联系人到其他分组
    @PostMapping("/move-to-group")
    public ApiResponse<?> moveContactToGroup(@Valid @RequestBody MoveContactRequest request,
                                             HttpServletRequest httpRequest) {
        try {
            Long userId = (Long) httpRequest.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            Contact contact = contactService.moveContactToGroup(userId, request);
            return ApiResponse.success(contact, "移动联系人成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "移动联系人失败: " + e.getMessage());
        }
    }

    // 获取待处理的好友申请数量
    @GetMapping("/friend-requests/count")
    public ApiResponse<?> getPendingFriendRequestCount(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            Long count = contactService.getPendingFriendRequestCount(userId);
            return ApiResponse.success(count);
        } catch (Exception e) {
            return ApiResponse.error(500, "获取申请数量失败: " + e.getMessage());
        }
    }

    // 根据分组获取联系人
    @GetMapping("/group/{groupName}")
    public ApiResponse<?> getContactsByGroup(@PathVariable String groupName,
                                             HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ApiResponse.error(401, "用户未登录");
            }

            // 这里可以调用contactService中的方法获取特定分组的联系人
            return ApiResponse.success("根据分组获取联系人功能");
        } catch (Exception e) {
            return ApiResponse.error(500, "获取联系人失败: " + e.getMessage());
        }
    }
}