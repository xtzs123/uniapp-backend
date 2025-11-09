package com.example.uniapp_backend.repository;

import com.example.uniapp_backend.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    // 根据用户ID和状态查找联系人
    List<Contact> findByUserIdAndStatus(Long userId, Integer status);

    // 根据用户ID和分组查找联系人
    List<Contact> findByUserIdAndContactGroupAndStatus(Long userId, String contactGroup, Integer status);

    // 检查是否已经是联系人
    Optional<Contact> findByUserIdAndFriendIdAndStatus(Long userId, Long friendId, Integer status);

    // 查找双向联系人关系
    @Query("SELECT c FROM Contact c WHERE ((c.userId = :user1 AND c.friendId = :user2) OR (c.userId = :user2 AND c.friendId = :user1)) AND c.status = 1")
    List<Contact> findContactRelationship(@Param("user1") Long user1, @Param("user2") Long user2);

    // 统计用户联系人数量
    Long countByUserIdAndStatus(Long userId, Integer status);

    // 删除重复的方法定义，只保留一个
    // List<Contact> findByUserIdAndStatus(Long userId, Boolean isBlocked); // 删除这行
}