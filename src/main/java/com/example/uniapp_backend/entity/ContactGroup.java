package com.example.uniapp_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_groups")
@Data
public class ContactGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    // 手动添加 getter/setter 方法
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }
}