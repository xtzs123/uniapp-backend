package com.example.uniapp_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单内存消息代理，在前端订阅以接收消息
        config.enableSimpleBroker("/topic", "/queue", "/user");
        // 设置应用程序目的地前缀，前端发送消息时使用
        config.setApplicationDestinationPrefixes("/app");
        // 设置用户目的地前缀（用于点对点消息）
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 WebSocket 端点，允许跨域
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 支持 SockJS 回退选项

        // 保留原有的 websocket 端点以兼容现有代码
        registry.addEndpoint("/websocket")
                .setAllowedOriginPatterns("*");
    }
}