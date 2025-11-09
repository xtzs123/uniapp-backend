package com.example.uniapp_backend.repository;

import com.example.uniapp_backend.entity.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    // 根据接收者ID和状态查找好友申请
    List<FriendRequest> findByToUserIdAndStatus(Long toUserId, Integer status);

    // 根据发送者ID查找好友申请
    List<FriendRequest> findByFromUserId(Long fromUserId);

    // 检查是否已经存在待处理的好友申请
    Optional<FriendRequest> findByFromUserIdAndToUserIdAndStatus(Long fromUserId, Long toUserId, Integer status);

    // 查找双方的好友申请记录
    @Query("SELECT fr FROM FriendRequest fr WHERE (fr.fromUserId = :user1 AND fr.toUserId = :user2) OR (fr.fromUserId = :user2 AND fr.toUserId = :user1) ORDER BY fr.createdTime DESC")
    List<FriendRequest> findFriendRequestsBetweenUsers(@Param("user1") Long user1, @Param("user2") Long user2);

    // 统计待处理的好友申请数量
    Long countByToUserIdAndStatus(Long toUserId, Integer status);
}