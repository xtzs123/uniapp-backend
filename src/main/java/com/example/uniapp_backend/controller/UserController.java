package com.example.uniapp_backend.controller;

import com.example.uniapp_backend.config.JwtTokenUtil;
import com.example.uniapp_backend.entity.ApiResponse;
import com.example.uniapp_backend.entity.User;
import com.example.uniapp_backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // 用户注册
    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody Map<String, String> registerData, HttpServletRequest request) {
        try {
            String username = registerData.get("username");
            String password = registerData.get("password");
            String inviteCode = registerData.get("inviteCode");

            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                return ApiResponse.error(400, "用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return ApiResponse.error(400, "密码不能为空");
            }

            // 检查用户名是否存在
            if (userRepository.existsByUsername(username)) {
                return ApiResponse.error(400, "用户名已存在");
            }

            // 创建用户
            User user = new User();
            user.setUsername(username);
            user.setPassword(password); // 实际项目中应该加密
            user.setUsedInviteCode(inviteCode);
            user.setIp(getClientIP(request));

            User savedUser = userRepository.save(user);

            // 生成Token - 使用统一的JWT工具类
            String token = jwtTokenUtil.generateUserToken(savedUser.getId(), savedUser.getUsername());

            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("user", savedUser);
            data.put("token", token);

            return ApiResponse.success(data);

        } catch (Exception e) {
            return ApiResponse.error(500, "注册失败: " + e.getMessage());
        }
    }

    // 用户登录
    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody Map<String, String> loginData, HttpServletRequest request) {
        try {
            String username = loginData.get("username");
            String password = loginData.get("password");

            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                return ApiResponse.error(400, "用户名不能为空");
            }
            if (password == null || password.trim().isEmpty()) {
                return ApiResponse.error(400, "密码不能为空");
            }

            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return ApiResponse.error(400, "用户不存在");
            }

            User user = userOptional.get();
            if (!user.getPassword().equals(password)) {
                return ApiResponse.error(400, "密码错误");
            }

            // 更新最后在线时间
            user.setLastOnline(LocalDateTime.now());
            user.setIp(getClientIP(request));
            userRepository.save(user);

            // 生成Token - 使用统一的JWT工具类
            String token = jwtTokenUtil.generateUserToken(user.getId(), user.getUsername());

            // 构建响应数据
            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            data.put("token", token);

            return ApiResponse.success(data);

        } catch (Exception e) {
            return ApiResponse.error(500, "登录失败: " + e.getMessage());
        }
    }

    // 获取用户信息（需要Token验证）
    @GetMapping("/info")
    public ApiResponse<?> getUserInfo(HttpServletRequest request) {
        try {
            // 从过滤器中获取用户ID - 使用新的统一属性名
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                // 备用方案：尝试使用统一属性名
                userId = (Long) request.getAttribute("id");
            }

            if (userId == null) {
                return ApiResponse.error(401, "用户未认证");
            }

            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isEmpty()) {
                return ApiResponse.error(404, "用户不存在");
            }

            return ApiResponse.success(userOptional.get());

        } catch (Exception e) {
            return ApiResponse.error(500, "获取用户信息失败: " + e.getMessage());
        }
    }

    // 更新用户信息
    @PostMapping("/update")
    public ApiResponse<?> updateUser(@RequestBody Map<String, Object> updateData, HttpServletRequest request) {
        try {
            // 从过滤器中获取用户ID - 使用新的统一属性名
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                // 备用方案：尝试使用统一属性名
                userId = (Long) request.getAttribute("id");
            }

            if (userId == null) {
                return ApiResponse.error(401, "用户未认证");
            }

            Optional<User> userOptional = userRepository.findById(userId);

            if (userOptional.isEmpty()) {
                return ApiResponse.error(404, "用户不存在");
            }

            User user = userOptional.get();

            // 更新允许修改的字段
            if (updateData.containsKey("nickname")) {
                String nickname = (String) updateData.get("nickname");
                if (nickname != null && !nickname.trim().isEmpty()) {
                    user.setNickname(nickname);
                }
            }
            if (updateData.containsKey("avatar")) {
                String avatar = (String) updateData.get("avatar");
                user.setAvatar(avatar);
            }
            if (updateData.containsKey("birthday")) {
                try {
                    String birthdayStr = (String) updateData.get("birthday");
                    if (birthdayStr != null && !birthdayStr.trim().isEmpty()) {
                        user.setBirthday(LocalDate.parse(birthdayStr));
                    }
                } catch (Exception e) {
                    return ApiResponse.error(400, "生日格式错误，请使用YYYY-MM-DD格式");
                }
            }
            if (updateData.containsKey("gender")) {
                String gender = (String) updateData.get("gender");
                if (gender != null && (gender.equals("男") || gender.equals("女") || gender.equals("未知"))) {
                    user.setGender(gender);
                }
            }

            User updatedUser = userRepository.save(user);

            return ApiResponse.success(updatedUser);

        } catch (Exception e) {
            return ApiResponse.error(500, "更新失败: " + e.getMessage());
        }
    }

    // 用户登出
    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletRequest request) {
        try {
            // JWT是无状态的，服务端不需要做特殊处理
            // 客户端只需要删除本地存储的Token即可
            // 如果需要服务端黑名单功能，可以在这里实现

            return ApiResponse.success("登出成功");

        } catch (Exception e) {
            return ApiResponse.error(500, "登出失败: " + e.getMessage());
        }
    }

    // 更新用户余额
    @PostMapping("/balance/update")
    public ApiResponse<?> updateBalance(@RequestBody Map<String, Object> balanceData, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = (Long) request.getAttribute("id");
            }

            if (userId == null) {
                return ApiResponse.error(401, "用户未认证");
            }

            if (!balanceData.containsKey("balance")) {
                return ApiResponse.error(400, "余额参数不能为空");
            }

            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ApiResponse.error(404, "用户不存在");
            }

            User user = userOptional.get();
            try {
                BigDecimal balance = new BigDecimal(balanceData.get("balance").toString());
                user.setBalance(balance);
                User updatedUser = userRepository.save(user);

                return ApiResponse.success(updatedUser);
            } catch (NumberFormatException e) {
                return ApiResponse.error(400, "余额格式错误");
            }

        } catch (Exception e) {
            return ApiResponse.error(500, "更新余额失败: " + e.getMessage());
        }
    }

    // 获取用户余额
    @GetMapping("/balance")
    public ApiResponse<?> getBalance(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                userId = (Long) request.getAttribute("id");
            }

            if (userId == null) {
                return ApiResponse.error(401, "用户未认证");
            }

            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ApiResponse.error(404, "用户不存在");
            }

            Map<String, Object> balanceInfo = new HashMap<>();
            balanceInfo.put("balance", userOptional.get().getBalance());
            balanceInfo.put("userId", userId);

            return ApiResponse.success(balanceInfo);

        } catch (Exception e) {
            return ApiResponse.error(500, "获取余额失败: " + e.getMessage());
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

    // 添加CORS测试端点
    @GetMapping("/cors-test")
    public String userCorsTest() {
        System.out.println("=== 用户CORS测试端点被调用 ===");
        return "User CORS test success! Time: " + System.currentTimeMillis();
    }
}