package com.example.uniapp_backend;

import com.example.uniapp_backend.entity.Admin;
import com.example.uniapp_backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UniappBackendApplication implements CommandLineRunner {

    @Autowired
    private AdminRepository adminRepository;

    public static void main(String[] args) {
        SpringApplication.run(UniappBackendApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // 创建默认管理员账号（如果不存在）
        if (adminRepository.findByUsername("admin").isEmpty()) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setNickname("系统管理员");
            admin.setRole("super_admin");
            admin.setPermissions("all");
            adminRepository.save(admin);
            System.out.println("默认管理员账号已创建: admin / admin123");
        }
    }
}
测试