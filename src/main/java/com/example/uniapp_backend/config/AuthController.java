package com.example.uniapp_backend.controller;

import com.example.uniapp_backend.config.JwtTokenUtil;
import com.example.uniapp_backend.entity.ApiResponse;
import com.example.uniapp_backend.entity.User;
import com.example.uniapp_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");

            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                return ApiResponse.error(400, "用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return ApiResponse.error(400, "密码不能为空");
            }

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
                User user = userOpt.get();

                // 使用统一的JWT工具类生成Token
                String token = jwtTokenUtil.generateUserToken(user.getId(), user.getUsername());

                // 构建用户信息
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("nickname", user.getNickname());
                userInfo.put("avatar", user.getAvatar());
                userInfo.put("email", user.getEmail());
                userInfo.put("balance", user.getBalance());
                userInfo.put("status", user.getStatus());

                // 构建响应数据
                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("user", userInfo);

                return ApiResponse.success(data);
            } else {
                return ApiResponse.error(400, "用户名或密码错误");
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody User user) {
        try {
            // 参数验证
            if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
                return ApiResponse.error(400, "用户名不能为空");
            }
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return ApiResponse.error(400, "密码不能为空");
            }

            // 检查用户名是否已存在
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ApiResponse.error(400, "用户名已存在");
            }

            // 设置默认值
            if (user.getBalance() == null) {
                user.setBalance(java.math.BigDecimal.ZERO);
            }
            if (user.getStatus() == null) {
                user.setStatus(1);
            }

            User savedUser = userRepository.save(user);

            // 使用统一的JWT工具类生成Token
            String token = jwtTokenUtil.generateUserToken(savedUser.getId(), savedUser.getUsername());

            // 构建用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", savedUser.getId());
            userInfo.put("username", savedUser.getUsername());
            userInfo.put("nickname", savedUser.getNickname());
            userInfo.put("avatar", savedUser.getAvatar());
            userInfo.put("email", savedUser.getEmail());
            userInfo.put("balance", savedUser.getBalance());
            userInfo.put("status", savedUser.getStatus());

            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", userInfo);

            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.error(500, "注册失败: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ApiResponse<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtTokenUtil.validateToken(token)) {
                    // 使用新的方法获取用户ID
                    Long userId = jwtTokenUtil.getIdFromToken(token);
                    String username = jwtTokenUtil.getUsernameFromToken(token);

                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();

                        // 构建用户信息
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("id", user.getId());
                        userInfo.put("username", user.getUsername());
                        userInfo.put("nickname", user.getNickname());
                        userInfo.put("avatar", user.getAvatar());
                        userInfo.put("email", user.getEmail());
                        userInfo.put("balance", user.getBalance());
                        userInfo.put("status", user.getStatus());

                        return ApiResponse.success(userInfo);
                    }
                }
            }

            return ApiResponse.error(401, "Token无效");
        } catch (Exception e) {
            return ApiResponse.error(500, "Token验证失败: " + e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public ApiResponse<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtTokenUtil.validateToken(token)) {
                    // 检查Token类型，只允许刷新用户Token
                    String userType = jwtTokenUtil.getTypeFromToken(token);
                    if (!"USER".equals(userType)) {
                        return ApiResponse.error(403, "不支持刷新管理员Token");
                    }

                    // 刷新Token
                    String newToken = jwtTokenUtil.refreshToken(token);

                    Map<String, Object> data = new HashMap<>();
                    data.put("token", newToken);

                    return ApiResponse.success(data);
                }
            }

            return ApiResponse.error(401, "Token无效");
        } catch (Exception e) {
            return ApiResponse.error(500, "Token刷新失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // JWT是无状态的，服务端不需要做特殊处理
            // 客户端只需要删除本地存储的Token即可

            return ApiResponse.success("登出成功");
        } catch (Exception e) {
            return ApiResponse.error(500, "登出失败: " + e.getMessage());
        }
    }

    @GetMapping("/check-username")
    public ApiResponse<?> checkUsername(@RequestParam String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return ApiResponse.error(400, "用户名不能为空");
            }

            boolean exists = userRepository.findByUsername(username).isPresent();

            Map<String, Object> data = new HashMap<>();
            data.put("username", username);
            data.put("available", !exists);
            data.put("message", exists ? "用户名已存在" : "用户名可用");

            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.error(500, "检查用户名失败: " + e.getMessage());
        }
    }

    @GetMapping("/check-email")
    public ApiResponse<?> checkEmail(@RequestParam String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ApiResponse.error(400, "邮箱不能为空");
            }

            // 简单的邮箱格式验证
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ApiResponse.error(400, "邮箱格式不正确");
            }

            boolean exists = userRepository.existsByEmail(email);

            Map<String, Object> data = new HashMap<>();
            data.put("email", email);
            data.put("available", !exists);
            data.put("message", exists ? "邮箱已存在" : "邮箱可用");

            return ApiResponse.success(data);
        } catch (Exception e) {
            return ApiResponse.error(500, "检查邮箱失败: " + e.getMessage());
        }
    }
}