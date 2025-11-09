// controller/DebugController.java
package com.example.uniapp_backend.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;

@RestController
public class DebugController {

    @GetMapping("/debug-raw")
    public void debugRaw(HttpServletResponse response) throws IOException {
        System.out.println("🎯 Debug Raw端点被调用！");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter writer = response.getWriter();
        writer.write("{\"debug\": \"success\", \"message\": \"This bypasses all processors\"}");
        writer.flush();
    }

    @GetMapping("/debug-text")
    public void debugText(HttpServletResponse response) throws IOException {
        System.out.println("🎯 Debug Text端点被调用！");

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        PrintWriter writer = response.getWriter();
        writer.write("Debug text response - Working!");
        writer.flush();
    }
}