package com.ttt.vivm.repository;

import com.ttt.vivm.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUserName(String userName);
    Optional<User> findByEmail(String email);
    // 分页查询所有用户
    Page<User> findAll(Pageable pageable);

    // 分页 + 按用户名搜索（可选）
    Page<User> findByUserNameContaining(String keyword, Pageable pageable);
}
