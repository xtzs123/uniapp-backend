package com.example.uniapp_backend.repository;

import com.example.uniapp_backend.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findByGroupId(Long groupId);

    List<GroupMember> findByUserId(Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.groupId = :groupId AND gm.userId = :userId")
    void removeMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.groupId = :groupId")
    Long countMembersByGroupId(@Param("groupId") Long groupId);

    @Modifying
    @Query("DELETE FROM GroupMember gm WHERE gm.groupId = :groupId")
    void deleteByGroupId(@Param("groupId") Long groupId);
}