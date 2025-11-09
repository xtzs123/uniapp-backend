// controller/UltimateBypassController.java
package com.example.uniapp_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

@RestController
public class UltimateBypassController {

    @GetMapping("/ultimate-test")
    public void ultimateTest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("🎯 终极绕过端点被调用！");
        System.out.println("请求URI: " + request.getRequestURI());
        System.out.println("请求方法: " + request.getMethod());

        // 完全绕过所有Spring处理，直接写入响应
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter writer = response.getWriter();
        writer.write("{\"ultimate\": \"success\", \"message\": \"完全绕过所有处理\", \"time\": " + System.currentTimeMillis() + "}");
        writer.flush();
        writer.close();
    }
}