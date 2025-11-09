// config/DiagnosticFilter.java
package com.example.uniapp_backend.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  // 最高优先级，最先执行
public class DiagnosticFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getServletPath();
        String method = httpRequest.getMethod();

        System.out.println("🔍 [DiagnosticFilter] 开始处理: " + method + " " + path);

        // 如果是终极测试路径，直接返回成功
        if ("/ultimate-test".equals(path)) {
            System.out.println("✅ [DiagnosticFilter] 直接处理终极测试路径");
            httpResponse.setStatus(200);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"diagnostic\": \"success\", \"message\": \"DiagnosticFilter直接处理\"}");
            return;
        }

        // 继续过滤器链
        chain.doFilter(request, response);
        System.out.println("🔍 [DiagnosticFilter] 完成处理: " + method + " " + path);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        System.out.println("=== DiagnosticFilter 初始化 ===");
    }

    @Override
    public void destroy() {
        // 清理资源
    }
}