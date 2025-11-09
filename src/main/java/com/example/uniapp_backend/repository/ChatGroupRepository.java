package com.example.uniapp_backend.repository;

import com.example.uniapp_backend.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    Optional<ChatGroup> findByGroupName(String groupName);

    List<ChatGroup> findByCreatorId(Long creatorId);

    // 查询用户加入的群组
    @Query("SELECT g FROM ChatGroup g WHERE g.id IN (SELECT gm.groupId FROM GroupMember gm WHERE gm.userId = :userId)")
    List<ChatGroup> findGroupsByUserId(@Param("userId") Long userId);
}