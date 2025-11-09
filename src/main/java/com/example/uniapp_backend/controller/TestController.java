package com.example.uniapp_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "服务器运行正常！";
    }

    @GetMapping("/api/user/test")
    public String userTest() {
        return "用户接口测试成功！";
    }
}