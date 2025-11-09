package com.example.uniapp_backend.repository;

import com.example.uniapp_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // 添加分页参数
    Page<User> findByUsernameContainingOrNicknameContaining(String username, String nickname, Pageable pageable);

    // 添加不分页的查询方法
    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.nickname LIKE %:keyword%")
    List<User> findByUsernameOrNicknameContaining(@Param("keyword") String keyword);

    // 添加缺失的方法
    List<User> findByNicknameContaining(String nickname);

    @Query("SELECT u FROM User u WHERE u.lastOnline >= :since")
    List<User> findActiveUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    Long countTodayNewUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastOnline >= :since")
    Long countOnlineUsers(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    Long countNewUsersByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}