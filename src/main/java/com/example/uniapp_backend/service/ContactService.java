package com.example.uniapp_backend.service;

import com.example.uniapp_backend.entity.*;
import com.example.uniapp_backend.repository.*;
import com.example.uniapp_backend.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private ContactGroupRepository contactGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    // 获取用户的所有联系人（正常状态）
    public List<Contact> getUserContacts(Long userId) {
        List<Contact> contacts = contactRepository.findByUserIdAndStatus(userId, 1);

        // 填充联系人详细信息
        return contacts.stream().map(contact -> {
            Optional<User> friend = userRepository.findById(contact.getFriendId());
            if (friend.isPresent()) {
                User user = friend.get();
                contact.setFriendUsername(user.getUsername());
                contact.setFriendNickname(user.getNickname());
                contact.setFriendAvatar(user.getAvatar());
                contact.setFriendEmail(user.getEmail());
            }
            return contact;
        }).collect(Collectors.toList());
    }

    // 发送好友申请
    @Transactional
    public FriendRequest sendFriendRequest(Long fromUserId, AddFriendRequest request) {
        // 查找目标用户
        Optional<User> targetUser = userRepository.findByUsername(request.getUsername());
        if (!targetUser.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User toUser = targetUser.get();

        // 检查是否是自己
        if (fromUserId.equals(toUser.getId())) {
            throw new RuntimeException("不能添加自己为好友");
        }

        // 检查是否已经是好友
        Optional<Contact> existingContact = contactRepository.findByUserIdAndFriendIdAndStatus(fromUserId, toUser.getId(), 1);
        if (existingContact.isPresent()) {
            throw new RuntimeException("该用户已经是您的好友");
        }

        // 检查是否已经有待处理的申请
        Optional<FriendRequest> existingRequest = friendRequestRepository.findByFromUserIdAndToUserIdAndStatus(fromUserId, toUser.getId(), 0);
        if (existingRequest.isPresent()) {
            throw new RuntimeException("已发送过好友申请，请等待对方处理");
        }

        // 创建好友申请
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setFromUserId(fromUserId);
        friendRequest.setToUserId(toUser.getId());
        friendRequest.setMessage(request.getMessage());
        friendRequest.setStatus(0);

        FriendRequest savedRequest = friendRequestRepository.save(friendRequest);

        // 填充用户信息
        Optional<User> fromUser = userRepository.findById(fromUserId);
        fromUser.ifPresent(user -> {
            savedRequest.setFromUsername(user.getUsername());
            savedRequest.setFromNickname(user.getNickname());
            savedRequest.setFromAvatar(user.getAvatar());
        });

        savedRequest.setToUsername(toUser.getUsername());
        savedRequest.setToNickname(toUser.getNickname());

        log.info("用户 {} 向用户 {} 发送好友申请", fromUserId, toUser.getId());
        return savedRequest;
    }

    // 处理好友申请
    @Transactional
    public void handleFriendRequest(Long userId, HandleFriendRequest request) {
        Optional<FriendRequest> friendRequestOpt = friendRequestRepository.findById(request.getRequestId());
        if (!friendRequestOpt.isPresent()) {
            throw new RuntimeException("好友申请不存在");
        }

        FriendRequest friendRequest = friendRequestOpt.get();

        // 检查是否有权限处理
        if (!friendRequest.getToUserId().equals(userId)) {
            throw new RuntimeException("无权处理此好友申请");
        }

        // 检查申请状态
        if (friendRequest.getStatus() != 0) {
            throw new RuntimeException("该好友申请已处理");
        }

        if (request.getAccepted()) {
            // 同意好友申请
            friendRequest.setStatus(1);

            // 创建双向好友关系
            createMutualContact(friendRequest.getFromUserId(), friendRequest.getToUserId());

            // 自动创建对话
            try {
                conversationService.getOrCreatePrivateConversation(friendRequest.getFromUserId(), friendRequest.getToUserId());
            } catch (Exception e) {
                log.warn("创建对话失败: {}", e.getMessage());
            }

            log.info("用户 {} 同意了用户 {} 的好友申请", userId, friendRequest.getFromUserId());
        } else {
            // 拒绝好友申请
            friendRequest.setStatus(2);
            log.info("用户 {} 拒绝了用户 {} 的好友申请", userId, friendRequest.getFromUserId());
        }

        friendRequestRepository.save(friendRequest);
    }

    // 创建双向好友关系
    private void createMutualContact(Long user1Id, Long user2Id) {
        // 为用户1创建联系人
        Contact contact1 = new Contact();
        contact1.setUserId(user1Id);
        contact1.setFriendId(user2Id);
        contact1.setStatus(1);
        contactRepository.save(contact1);

        // 为用户2创建联系人
        Contact contact2 = new Contact();
        contact2.setUserId(user2Id);
        contact2.setFriendId(user1Id);
        contact2.setStatus(1);
        contactRepository.save(contact2);
    }

    // 获取好友申请列表
    public List<FriendRequest> getFriendRequests(Long userId) {
        List<FriendRequest> requests = friendRequestRepository.findByToUserIdAndStatus(userId, 0);

        // 填充发送者信息
        return requests.stream().map(request -> {
            Optional<User> fromUser = userRepository.findById(request.getFromUserId());
            fromUser.ifPresent(user -> {
                request.setFromUsername(user.getUsername());
                request.setFromNickname(user.getNickname());
                request.setFromAvatar(user.getAvatar());
            });
            return request;
        }).collect(Collectors.toList());
    }

    // 更新联系人信息
    @Transactional
    public Contact updateContact(Long userId, UpdateContactRequest request) {
        if (request.getId() == null) {
            throw new RuntimeException("联系人ID不能为空");
        }

        Optional<Contact> contactOpt = contactRepository.findById(request.getId());
        if (!contactOpt.isPresent()) {
            throw new RuntimeException("联系人不存在");
        }

        Contact contact = contactOpt.get();
        if (!contact.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此联系人");
        }

        if (request.getRemarkName() != null) {
            contact.setRemarkName(request.getRemarkName());
        }
        if (request.getContactGroup() != null) {
            contact.setContactGroup(request.getContactGroup());
        }
        if (request.getStatus() != null) {
            contact.setStatus(request.getStatus());
        }

        return contactRepository.save(contact);
    }

    // 删除联系人（软删除）
    @Transactional
    public void deleteContact(Long userId, Long contactId) {
        Optional<Contact> contactOpt = contactRepository.findById(contactId);
        if (!contactOpt.isPresent()) {
            throw new RuntimeException("联系人不存在");
        }

        Contact contact = contactOpt.get();
        if (!contact.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此联系人");
        }

        // 软删除，修改状态为3
        contact.setStatus(3);
        contactRepository.save(contact);

        log.info("用户 {} 删除联系人 {} 成功", userId, contactId);
    }

    // 拉黑联系人
    @Transactional
    public Contact blockContact(Long userId, Long contactId) {
        Optional<Contact> contactOpt = contactRepository.findById(contactId);
        if (!contactOpt.isPresent()) {
            throw new RuntimeException("联系人不存在");
        }

        Contact contact = contactOpt.get();
        if (!contact.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此联系人");
        }

        contact.setStatus(2); // 2-拉黑
        return contactRepository.save(contact);
    }

    // 搜索用户（用于添加好友）
    public List<User> searchUsers(Long userId, String keyword) {
        // 使用新的不分页查询方法
        return userRepository.findByUsernameOrNicknameContaining(keyword)
                .stream()
                .filter(user -> !user.getId().equals(userId)) // 排除自己
                .collect(Collectors.toList());
    }

    // 发送消息给联系人
    @Transactional
    public void sendMessageToContact(Long userId, Long contactId, String content, Integer messageType) {
        Optional<Contact> contactOpt = contactRepository.findById(contactId);
        if (!contactOpt.isPresent()) {
            throw new RuntimeException("联系人不存在");
        }

        Contact contact = contactOpt.get();
        if (!contact.getUserId().equals(userId)) {
            throw new RuntimeException("无权向此联系人发送消息");
        }

        if (contact.getStatus() != 1) {
            throw new RuntimeException("该联系人状态异常，无法发送消息");
        }

        // 获取或创建对话
        String conversationId = conversationService.getOrCreatePrivateConversation(userId, contact.getFriendId());

        // 发送消息 - 使用正确的方法签名
        // messageService.sendMessage(userId, conversationId, content, messageType);
        // 暂时注释，等MessageService实现后再取消注释

        log.info("用户 {} 向联系人 {} 发送消息", userId, contact.getFriendId());
    }

    // 创建联系人分组
    @Transactional
    public ContactGroup createContactGroup(Long userId, CreateGroupRequest request) {
        // 检查分组是否已存在
        Optional<ContactGroup> existingGroup = contactGroupRepository.findByUserIdAndGroupName(userId, request.getGroupName());
        if (existingGroup.isPresent()) {
            throw new RuntimeException("分组名称已存在");
        }

        ContactGroup group = new ContactGroup();
        group.setUserId(userId);
        group.setGroupName(request.getGroupName());
        group.setSortOrder(request.getSortOrder());

        return contactGroupRepository.save(group);
    }

    // 获取用户的分组列表
    public List<ContactGroup> getUserContactGroups(Long userId) {
        return contactGroupRepository.findByUserIdOrderBySortOrderAsc(userId);
    }

    // 删除分组
    @Transactional
    public void deleteContactGroup(Long userId, Long groupId) {
        // 检查分组是否存在
        Optional<ContactGroup> groupOpt = contactGroupRepository.findById(groupId);
        if (!groupOpt.isPresent()) {
            throw new RuntimeException("分组不存在");
        }

        ContactGroup group = groupOpt.get();
        if (!group.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此分组");
        }

        // 检查分组中是否有联系人
        List<Contact> contactsInGroup = contactRepository.findByUserIdAndContactGroupAndStatus(userId, group.getGroupName(), 1);
        if (!contactsInGroup.isEmpty()) {
            throw new RuntimeException("分组中还有联系人，无法删除");
        }

        contactGroupRepository.delete(group);
    }

    // 移动联系人到其他分组
    @Transactional
    public Contact moveContactToGroup(Long userId, MoveContactRequest request) {
        Optional<Contact> contactOpt = contactRepository.findById(request.getContactId());
        if (!contactOpt.isPresent()) {
            throw new RuntimeException("联系人不存在");
        }

        Contact contact = contactOpt.get();
        if (!contact.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此联系人");
        }

        contact.setContactGroup(request.getTargetGroup());
        return contactRepository.save(contact);
    }

    // 获取待处理的好友申请数量
    public Long getPendingFriendRequestCount(Long userId) {
        return friendRequestRepository.countByToUserIdAndStatus(userId, 0);
    }
}