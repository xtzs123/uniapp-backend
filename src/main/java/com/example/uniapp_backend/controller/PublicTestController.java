package com.example.uniapp_backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
// 移除这行: @CrossOrigin(origins = "*")
public class PublicTestController {

    @GetMapping("/test")
    public String publicTest() {
        System.out.println("=== 公开测试端点被调用 ===");
        return "Public test success! Time: " + System.currentTimeMillis();
    }

    @PostMapping("/test-post")
    public String publicTestPost(@RequestBody String data) {
        System.out.println("=== 公开POST测试端点被调用，数据: " + data + " ===");
        return "Public POST success! Received: " + data;
    }

    @GetMapping("/cors-test")
    public String corsTest() {
        System.out.println("=== 公开CORS测试端点被调用 ===");
        return "Public CORS test success!";
    }
}