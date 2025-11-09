package com.example.uniapp_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddFriendRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    private String message;

    // 显式添加 getter/setter 以确保 Lombok 工作正常
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}