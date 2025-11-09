package com.example.uniapp_backend.service;

import com.example.uniapp_backend.entity.ChatGroup;
import com.example.uniapp_backend.entity.GroupMember;
import com.example.uniapp_backend.repository.ChatGroupRepository;
import com.example.uniapp_backend.repository.GroupMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChatGroupService {

    @Autowired
    private ChatGroupRepository chatGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    // 创建群组
    @Transactional
    public ChatGroup createGroup(ChatGroup group, Long creatorId) {
        ChatGroup savedGroup = chatGroupRepository.save(group);

        // 添加创建者为群主
        GroupMember creator = new GroupMember();
        creator.setGroupId(savedGroup.getId());
        creator.setUserId(creatorId);
        creator.setRole(GroupMember.GroupRole.OWNER);
        groupMemberRepository.save(creator);

        return savedGroup;
    }

    // 获取群组信息
    public Optional<ChatGroup> getGroupById(Long groupId) {
        return chatGroupRepository.findById(groupId);
    }

    // 获取用户创建的群组
    public List<ChatGroup> getGroupsByCreator(Long creatorId) {
        return chatGroupRepository.findByCreatorId(creatorId);
    }

    // 获取用户加入的群组
    public List<ChatGroup> getGroupsByUser(Long userId) {
        return chatGroupRepository.findGroupsByUserId(userId);
    }

    // 更新群组信息
    @Transactional
    public ChatGroup updateGroup(ChatGroup group) {
        return chatGroupRepository.save(group);
    }

    // 解散群组
    @Transactional
    public void deleteGroup(Long groupId, Long operatorId) {
        Optional<ChatGroup> groupOpt = chatGroupRepository.findById(groupId);
        if (groupOpt.isPresent()) {
            ChatGroup group = groupOpt.get();
            // 只有群主可以解散群组
            if (group.getCreatorId().equals(operatorId)) {
                // 先删除群成员
                groupMemberRepository.deleteByGroupId(groupId);
                // 再删除群组
                chatGroupRepository.delete(group);
            } else {
                throw new SecurityException("只有群主可以解散群组");
            }
        }
    }

    // 添加群成员
    @Transactional
    public void addGroupMember(Long groupId, Long userId, GroupMember.GroupRole role) {
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException("用户已是群成员");
        }

        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setRole(role);
        groupMemberRepository.save(member);

        // 更新群成员数量
        updateMemberCount(groupId);
    }

    // 移除群成员
    @Transactional
    public void removeGroupMember(Long groupId, Long userId, Long operatorId) {
        Optional<GroupMember> operatorOpt = groupMemberRepository.findByGroupIdAndUserId(groupId, operatorId);
        Optional<GroupMember> targetOpt = groupMemberRepository.findByGroupIdAndUserId(groupId, userId);

        if (targetOpt.isPresent()) {
            GroupMember target = targetOpt.get();
            // 只有群主或管理员可以移除成员，且不能移除自己（群主除外）
            if (operatorOpt.isPresent()) {
                GroupMember operator = operatorOpt.get();
                boolean canRemove = operator.getRole() == GroupMember.GroupRole.OWNER ||
                        (operator.getRole() == GroupMember.GroupRole.ADMIN &&
                                target.getRole() == GroupMember.GroupRole.MEMBER);

                if (canRemove && (!operator.getUserId().equals(userId) || operator.getRole() == GroupMember.GroupRole.OWNER)) {
                    groupMemberRepository.removeMember(groupId, userId);
                    updateMemberCount(groupId);
                } else {
                    throw new SecurityException("没有权限移除该成员");
                }
            }
        }
    }

    // 更新群成员数量
    private void updateMemberCount(Long groupId) {
        Long count = groupMemberRepository.countMembersByGroupId(groupId);
        Optional<ChatGroup> groupOpt = chatGroupRepository.findById(groupId);
        if (groupOpt.isPresent()) {
            ChatGroup group = groupOpt.get();
            group.setMemberCount(count.intValue());
            chatGroupRepository.save(group);
        }
    }

    // 获取群成员
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }

    // 检查用户是否是群成员
    public boolean isGroupMember(Long groupId, Long userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }
}