package com.example.uniapp_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Column(unique = true, nullable = false)
    private String username;

    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    // 保持数据库兼容性 - 使用原有的列名
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 新增更新时间字段
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    // 密码字段
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少6位")
    private String password;

    // 个人信息字段
    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatar;

    @Size(max = 10, message = "性别长度不能超过10个字符")
    private String gender;

    private LocalDate birthday;

    // 财务字段
    @Column(columnDefinition = "DECIMAL(10,2) default 0.00")
    private BigDecimal balance = BigDecimal.ZERO;

    // 状态字段
    @Column(columnDefinition = "INT default 1")
    private Integer status = 1;

    // 在线状态
    @Column(name = "last_online")
    private LocalDateTime lastOnline;

    // 邀请码相关
    @Column(name = "used_invite_code")
    @Size(max = 50, message = "邀请码长度不能超过50个字符")
    private String usedInviteCode;

    // IP地址
    @Size(max = 45, message = "IP地址长度不能超过45个字符")
    private String ip;

    // 构造函数
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.balance = BigDecimal.ZERO;
        this.status = 1; // 默认状态为激活
    }

    public User(String username, String nickname, String email) {
        this();
        this.username = username;
        this.nickname = nickname;
        this.email = email;
    }

    // 生命周期回调方法
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updateTime == null) {
            updateTime = LocalDateTime.now();
        }
        if (balance == null) {
            balance = BigDecimal.ZERO;
        }
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // 保持原有的getter/setter以兼容Repository
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // 新增的更新时间字段
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getLastOnline() { return lastOnline; }
    public void setLastOnline(LocalDateTime lastOnline) { this.lastOnline = lastOnline; }

    public String getUsedInviteCode() { return usedInviteCode; }
    public void setUsedInviteCode(String usedInviteCode) { this.usedInviteCode = usedInviteCode; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    // 业务方法
    public boolean isActive() {
        return status != null && status == 1;
    }

    public boolean isOnline() {
        return lastOnline != null &&
                lastOnline.isAfter(LocalDateTime.now().minusMinutes(5));
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return balance != null && amount != null &&
                balance.compareTo(amount) >= 0;
    }

    public void deductBalance(BigDecimal amount) {
        if (hasSufficientBalance(amount)) {
            balance = balance.subtract(amount);
        } else {
            throw new IllegalArgumentException("余额不足");
        }
    }

    public void addBalance(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            if (balance == null) {
                balance = BigDecimal.ZERO;
            }
            balance = balance.add(amount);
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                ", updateTime=" + updateTime +
                ", avatar='" + avatar + '\'' +
                ", gender='" + gender + '\'' +
                ", balance=" + balance +
                ", status=" + status +
                ", lastOnline=" + lastOnline +
                '}';
    }
}