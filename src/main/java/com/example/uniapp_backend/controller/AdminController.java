package com.example.uniapp_backend.controller;

import com.example.uniapp_backend.config.JwtTokenUtil;
import com.example.uniapp_backend.entity.Admin;
import com.example.uniapp_backend.entity.ApiResponse;
import com.example.uniapp_backend.entity.User;
import com.example.uniapp_backend.repository.AdminRepository;
import com.example.uniapp_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // 管理员登录
    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody Map<String, String> loginData, HttpServletRequest request) {
        try {
            String username = loginData.get("username");
            String password = loginData.get("password");

            if (username == null || username.trim().isEmpty()) {
                return ApiResponse.error(400, "用户名不能为空");
            }

            if (password == null || password.trim().isEmpty()) {
                return ApiResponse.error(400, "密码不能为空");
            }

            Optional<Admin> adminOptional = adminRepository.findByUsername(username);

            if (adminOptional.isEmpty()) {
                return ApiResponse.error(400, "管理员账号不存在");
            }

            Admin admin = adminOptional.get();
            if (!admin.getPassword().equals(password)) {
                return ApiResponse.error(400, "密码错误");
            }

            // 更新最后登录信息
            admin.setLastLoginIp(getClientIP(request));
            admin.setLastLoginTime(LocalDateTime.now());
            adminRepository.save(admin);

            // 生成管理员Token - 使用统一的JWT工具类
            String token = jwtTokenUtil.generateAdminToken(admin.getId(), admin.getUsername());

            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("admin", admin);
            data.put("token", token);

            return ApiResponse.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "登录失败: " + e.getMessage());
        }
    }

    // 获取管理员信息
    @GetMapping("/info")
    public ApiResponse<?> getAdminInfo(HttpServletRequest request) {
        try {
            // 从过滤器中获取管理员ID - 使用新的统一属性名
            Long adminId = (Long) request.getAttribute("adminId");
            if (adminId == null) {
                // 备用方案：尝试使用统一属性名
                adminId = (Long) request.getAttribute("id");
            }

            if (adminId == null) {
                return ApiResponse.error(401, "未授权访问");
            }

            Optional<Admin> adminOptional = adminRepository.findById(adminId);

            if (adminOptional.isEmpty()) {
                return ApiResponse.error(404, "管理员不存在");
            }

            return ApiResponse.success(adminOptional.get());

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "获取管理员信息失败: " + e.getMessage());
        }
    }

    // 获取用户列表（分页）
    @GetMapping("/users")
    public ApiResponse<?> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {

        try {
            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 10;

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(sort).descending());
            Page<User> userPage = userRepository.findAll(pageable);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("currentPage", page);
            pageInfo.put("pageSize", size);
            pageInfo.put("totalUsers", userPage.getTotalElements());
            pageInfo.put("totalPages", userPage.getTotalPages());

            Map<String, Object> data = new HashMap<>();
            data.put("users", userPage.getContent());
            data.put("pageInfo", pageInfo);

            return ApiResponse.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "获取用户列表失败: " + e.getMessage());
        }
    }

    // 搜索用户
    @GetMapping("/users/search")
    public ApiResponse<?> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ApiResponse.error(400, "搜索关键词不能为空");
            }

            if (page < 1) page = 1;
            if (size < 1 || size > 100) size = 10;

            Pageable pageable = PageRequest.of(page - 1, size);
            Page<User> userPage = userRepository.findByUsernameContainingOrNicknameContaining(keyword, keyword, pageable);

            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("currentPage", page);
            pageInfo.put("pageSize", size);
            pageInfo.put("totalUsers", userPage.getTotalElements());
            pageInfo.put("totalPages", userPage.getTotalPages());

            Map<String, Object> data = new HashMap<>();
            data.put("users", userPage.getContent());
            data.put("pageInfo", pageInfo);

            return ApiResponse.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "搜索用户失败: " + e.getMessage());
        }
    }

    // 获取用户详情
    @GetMapping("/users/{id}")
    public ApiResponse<?> getUserDetail(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ApiResponse.error(400, "用户ID格式错误");
            }

            Optional<User> userOptional = userRepository.findById(id);

            if (userOptional.isEmpty()) {
                return ApiResponse.error(404, "用户不存在");
            }

            return ApiResponse.success(userOptional.get());

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "获取用户详情失败: " + e.getMessage());
        }
    }

    // 更新用户信息（管理员操作）
    @PostMapping("/users/{id}/update")
    public ApiResponse<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updateData) {
        try {
            if (id == null || id <= 0) {
                return ApiResponse.error(400, "用户ID格式错误");
            }

            Optional<User> userOptional = userRepository.findById(id);

            if (userOptional.isEmpty()) {
                return ApiResponse.error(404, "用户不存在");
            }

            User user = userOptional.get();

            // 管理员可以更新更多字段
            if (updateData.containsKey("nickname")) {
                String nickname = (String) updateData.get("nickname");
                if (nickname != null && !nickname.trim().isEmpty()) {
                    user.setNickname(nickname);
                }
            }

            if (updateData.containsKey("balance")) {
                try {
                    BigDecimal balance = new BigDecimal(updateData.get("balance").toString());
                    user.setBalance(balance);
                } catch (NumberFormatException e) {
                    return ApiResponse.error(400, "余额格式错误");
                }
            }

            if (updateData.containsKey("status")) {
                try {
                    Integer status = Integer.valueOf(updateData.get("status").toString());
                    user.setStatus(status);
                } catch (NumberFormatException e) {
                    return ApiResponse.error(400, "状态格式错误");
                }
            }

            if (updateData.containsKey("avatar")) {
                String avatar = (String) updateData.get("avatar");
                user.setAvatar(avatar);
            }

            if (updateData.containsKey("gender")) {
                String gender = (String) updateData.get("gender");
                user.setGender(gender);
            }

            // 管理员可以更新邮箱
            if (updateData.containsKey("email")) {
                String email = (String) updateData.get("email");
                user.setEmail(email);
            }

            User updatedUser = userRepository.save(user);

            return ApiResponse.success(updatedUser);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "更新用户失败: " + e.getMessage());
        }
    }

    // 删除用户
    @DeleteMapping("/users/{id}")
    public ApiResponse<?> deleteUser(@PathVariable Long id) {
        try {
            if (id == null || id <= 0) {
                return ApiResponse.error(400, "用户ID格式错误");
            }

            Optional<User> userOptional = userRepository.findById(id);

            if (userOptional.isEmpty()) {
                return ApiResponse.error(404, "用户不存在");
            }

            userRepository.deleteById(id);

            return ApiResponse.success("删除用户成功");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "删除用户失败: " + e.getMessage());
        }
    }

    // 获取统计信息
    @GetMapping("/stats")
    public ApiResponse<?> getStats() {
        try {
            long totalUsers = userRepository.count();

            // 计算今日新增用户
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            Long todayNewUsers = userRepository.countTodayNewUsers(startOfDay, endOfDay);

            // 计算活跃用户（过去24小时内在线）
            Long activeUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusHours(24));

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("todayNewUsers", todayNewUsers != null ? todayNewUsers : 0);
            stats.put("activeUsers", activeUsers != null ? activeUsers : 0);
            stats.put("updateTime", LocalDateTime.now().toString());

            return ApiResponse.success(stats);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "获取统计信息失败: " + e.getMessage());
        }
    }

    // 获取每日统计信息
    @GetMapping("/stats/daily")
    public ApiResponse<?> getDailyStats() {
        try {
            LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

            Long todayNewUsers = userRepository.countTodayNewUsers(startOfDay, endOfDay);
            Long totalUsers = userRepository.count();
            Long activeUsers = userRepository.countOnlineUsers(LocalDateTime.now().minusHours(24));

            Map<String, Object> stats = new HashMap<>();
            stats.put("todayNewUsers", todayNewUsers != null ? todayNewUsers : 0);
            stats.put("totalUsers", totalUsers);
            stats.put("activeUsers", activeUsers != null ? activeUsers : 0);
            stats.put("date", LocalDate.now().toString());

            return ApiResponse.success(stats);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "获取每日统计失败: " + e.getMessage());
        }
    }

    // 获取最近在线用户
    @GetMapping("/users/recently-online")
    public ApiResponse<?> getRecentlyOnlineUsers(
            @RequestParam(defaultValue = "10") int size) {

        try {
            if (size < 1 || size > 50) size = 10;

            Pageable pageable = PageRequest.of(0, size, Sort.by("lastOnline").descending());
            Page<User> userPage = userRepository.findAll(pageable);

            return ApiResponse.success(userPage.getContent());

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "获取最近在线用户失败: " + e.getMessage());
        }
    }

    // 批量操作用户状态
    @PostMapping("/users/batch-update-status")
    public ApiResponse<?> batchUpdateUserStatus(@RequestBody Map<String, Object> batchData) {
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Long> userIds = (java.util.List<Long>) batchData.get("userIds");
            Integer status = (Integer) batchData.get("status");

            if (userIds == null || userIds.isEmpty()) {
                return ApiResponse.error(400, "用户ID列表不能为空");
            }

            if (status == null) {
                return ApiResponse.error(400, "状态不能为空");
            }

            int updatedCount = 0;
            for (Long userId : userIds) {
                Optional<User> userOptional = userRepository.findById(userId);
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    user.setStatus(status);
                    userRepository.save(user);
                    updatedCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("updatedCount", updatedCount);
            result.put("totalCount", userIds.size());

            return ApiResponse.success(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(500, "批量更新用户状态失败: " + e.getMessage());
        }
    }

    // 获取客户端IP
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}