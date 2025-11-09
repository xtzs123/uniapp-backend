package com.example.uniapp_backend.service;

import com.example.uniapp_backend.entity.User;
import com.example.uniapp_backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // 根据昵称搜索用户
    public List<User> searchUsersByNickname(String nickname) {
        return userRepository.findByNicknameContaining(nickname);
    }

    // 获取活跃用户（最近在线的用户）
    public List<User> getActiveUsersSince(LocalDateTime since) {
        return userRepository.findActiveUsersSince(since);
    }

    // 统计指定日期范围内的新用户数量
    public Long countNewUsersByDateRange(LocalDateTime start, LocalDateTime end) {
        return userRepository.countNewUsersByDateRange(start, end);
    }

    // 获取所有用户（分页）
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    // 根据ID获取用户
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // 更新用户信息
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // 删除用户
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // 统计总用户数
    public Long getTotalUserCount() {
        return userRepository.count();
    }

    // 检查用户名是否存在
    public boolean isUsernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    // 检查邮箱是否存在
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}