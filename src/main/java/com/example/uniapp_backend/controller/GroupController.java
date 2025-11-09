package com.example.uniapp_backend.controller;

import com.example.uniapp_backend.entity.ChatGroup;
import com.example.uniapp_backend.entity.GroupMember;
import com.example.uniapp_backend.service.ChatGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private ChatGroupService chatGroupService;

    // 创建群组
    @PostMapping("/create")
    public ResponseEntity<?> createGroup(
            @RequestParam String groupName,
            @RequestParam(required = false) String description,
            @RequestParam Long creatorId) {
        try {
            ChatGroup group = new ChatGroup();
            group.setGroupName(groupName);
            group.setDescription(description);
            group.setCreatorId(creatorId);

            ChatGroup createdGroup = chatGroupService.createGroup(group, creatorId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", createdGroup,
                    "message", "群组创建成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 获取群组信息
    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupInfo(@PathVariable Long groupId) {
        try {
            return chatGroupService.getGroupById(groupId)
                    .map(group -> ResponseEntity.ok(Map.of(
                            "success", true,
                            "data", group
                    )))
                    .orElse(ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "群组不存在"
                    )));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 获取用户加入的群组
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserGroups(@PathVariable Long userId) {
        try {
            List<ChatGroup> groups = chatGroupService.getGroupsByUser(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", groups
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 更新群组信息
    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(
            @PathVariable Long groupId,
            @RequestBody ChatGroup group) {
        try {
            group.setId(groupId);
            ChatGroup updatedGroup = chatGroupService.updateGroup(group);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", updatedGroup,
                    "message", "群组信息更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 解散群组
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(
            @PathVariable Long groupId,
            @RequestParam Long operatorId) {
        try {
            chatGroupService.deleteGroup(groupId, operatorId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "群组解散成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 添加群成员
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addGroupMember(
            @PathVariable Long groupId,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "MEMBER") GroupMember.GroupRole role) {
        try {
            chatGroupService.addGroupMember(groupId, userId, role);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "添加成员成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 移除群成员
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeGroupMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @RequestParam Long operatorId) {
        try {
            chatGroupService.removeGroupMember(groupId, userId, operatorId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "移除成员成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 获取群成员列表
    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long groupId) {
        try {
            List<GroupMember> members = chatGroupService.getGroupMembers(groupId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", members
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // 退出群组
    @PostMapping("/{groupId}/quit")
    public ResponseEntity<?> quitGroup(
            @PathVariable Long groupId,
            @RequestParam Long userId) {
        try {
            // 这里需要检查是否是群主，群主不能直接退出，需要转让群主或解散群组
            chatGroupService.removeGroupMember(groupId, userId, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "退出群组成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}