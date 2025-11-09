package com.example.uniapp_backend.repository;

import com.example.uniapp_backend.entity.ContactGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactGroupRepository extends JpaRepository<ContactGroup, Long> {

    // 根据用户ID查找分组
    List<ContactGroup> findByUserIdOrderBySortOrderAsc(Long userId);

    // 根据用户ID和分组名查找分组
    Optional<ContactGroup> findByUserIdAndGroupName(Long userId, String groupName);

    // 删除用户的分组
    void deleteByUserIdAndId(Long userId, Long id);

    // 统计用户的分组数量
    Long countByUserId(Long userId);
}