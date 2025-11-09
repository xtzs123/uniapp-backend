package com.example.uniapp_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    // 定义公开路径列表 - 包含所有测试和认证相关路径
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/",
            "/websocket",
            "/api/cors-test/",
            "/api/test/",
            "/public/",
            "/debug",
            "/bypass",
            "/direct",
            "/minimal",
            "/error",
            // 用户认证相关路径
            "/api/user/login",
            "/api/user/register",
            "/api/user/logout",
            "/api/user/cors-test",
            // 管理员认证相关路径
            "/api/admin/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();

        System.out.println("=== 统一认证过滤器: " + requestMethod + " " + requestPath + " ===");

        // 放行OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
            System.out.println("=== OPTIONS请求，直接放行 ===");
            chain.doFilter(request, response);
            return;
        }

        // 检查是否为公开路径
        boolean isPublicPath = PUBLIC_PATHS.stream()
                .anyMatch(requestPath::startsWith);

        if (isPublicPath) {
            System.out.println("=== 公开路径，跳过认证: " + requestPath + " ===");
            chain.doFilter(request, response);
            return;
        }

        final String requestTokenHeader = request.getHeader("Authorization");
        Long id = null;
        String userType = null;
        String username = null;

        // 从Header中获取token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            String jwtToken = requestTokenHeader.substring(7);
            try {
                if (jwtTokenUtil.validateToken(jwtToken)) {
                    id = jwtTokenUtil.getIdFromToken(jwtToken);
                    userType = jwtTokenUtil.getTypeFromToken(jwtToken);
                    username = jwtTokenUtil.getUsernameFromToken(jwtToken);

                    // 将用户信息设置到请求属性中
                    request.setAttribute("id", id);
                    request.setAttribute("userType", userType);
                    request.setAttribute("username", username);

                    // 根据用户类型设置不同的属性（向后兼容）
                    if ("ADMIN".equals(userType)) {
                        request.setAttribute("adminId", id);
                        request.setAttribute("adminUsername", username);
                        request.setAttribute("adminRole", "admin"); // 默认角色，可以从数据库查询实际角色
                        System.out.println("=== 管理员认证成功，管理员ID: " + id + " ===");
                    } else {
                        request.setAttribute("userId", id);
                        System.out.println("=== 用户认证成功，用户ID: " + id + " ===");
                    }
                } else {
                    System.out.println("=== Token验证失败 ===");
                    sendUnauthorizedError(response, "Token无效或已过期");
                    return;
                }
            } catch (Exception e) {
                logger.warn("Token验证失败: " + e.getMessage());
                System.out.println("=== Token验证异常: " + e.getMessage() + " ===");
                sendUnauthorizedError(response, "Token解析失败: " + e.getMessage());
                return;
            }
        } else {
            System.out.println("=== 未找到有效的Authorization头 ===");
            sendUnauthorizedError(response, "请提供有效的Token");
            return;
        }

        if (id == null) {
            System.out.println("=== 认证失败，返回未授权错误 ===");
            sendUnauthorizedError(response, "未授权访问");
            return;
        }

        // 检查路径权限（管理员路径需要管理员权限）
        if (requestPath.startsWith("/api/admin/") && !"ADMIN".equals(userType)) {
            System.out.println("=== 权限不足，访问管理员接口需要管理员权限 ===");
            sendForbiddenError(response, "权限不足，需要管理员权限");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 发送未授权错误响应
     */
    private void sendUnauthorizedError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":401,\"success\":false,\"message\":\"" + message + "\"}");
    }

    /**
     * 发送禁止访问错误响应
     */
    private void sendForbiddenError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":403,\"success\":false,\"message\":\"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // 静态资源等不需要过滤的路径
        return path.startsWith("/swagger") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/webjars") ||
                path.startsWith("/favicon.ico");
    }
}