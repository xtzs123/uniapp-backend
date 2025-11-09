package com.example.uniapp_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置CORS跨域
     * 注意：这里与CorsConfig中的配置保持一致，避免重复配置
     * 在实际项目中建议只在一个地方配置CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 移除了拦截器配置，因为：
     * 1. 已经使用统一的 JwtAuthenticationFilter 处理所有认证
     * 2. 拦截器和过滤器功能重复，会造成重复认证
     * 3. 统一使用过滤器更符合REST API的认证模式
     */

    // 如果需要添加其他Web MVC配置，可以在这里添加
    // 例如：静态资源处理、视图解析器、格式化等
}