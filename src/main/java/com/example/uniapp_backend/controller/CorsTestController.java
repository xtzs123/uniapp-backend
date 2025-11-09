package com.example.uniapp_backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cors-test")
// 移除这行: @CrossOrigin(origins = "*")
public class CorsTestController {

    @GetMapping("/simple")
    public String simpleTest() {
        System.out.println("=== GET /api/cors-test/simple 被调用 ===");
        return "CORS简单测试成功！时间：" + System.currentTimeMillis();
    }

    @PostMapping("/test")
    public String testPost(@RequestBody String data) {
        System.out.println("=== POST /api/cors-test/test 被调用，数据：" + data + " ===");
        return "CORS POST测试成功！收到数据：" + data;
    }

    @GetMapping
    public String root() {
        System.out.println("=== GET /api/cors-test 被调用 ===");
        return "CORS根路径测试成功！";
    }
}