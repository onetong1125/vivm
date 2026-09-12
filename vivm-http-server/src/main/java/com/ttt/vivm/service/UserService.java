package com.ttt.vivm.service;


import com.ttt.vivm.dto.UserOptDTO;
import com.ttt.vivm.model.*;
import com.ttt.vivm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, String> registerUser(User user) {
        Map<String, String> errors = new HashMap<>();
        if (userRepository.findByUserName(user.getUsername()).isPresent()) {
            errors.put("usernameError", "用户名已存在");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) { // 修正为 Optional 判断
            errors.put("emailError", "邮箱已注册");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        // 加密密码
        user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
        // 设置默认值
        user.setBalance(new BigDecimal("0.00"));
        user.setRole(0);        // 默认注册普通用户
        user.setOssSpace(0);
        userRepository.save(user);
        return null;
    }

    public void deleteUserById(int userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        userRepository.delete(userOptional.get());
    }

    // 获取分页用户列表
    public Page<User> getUsers(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("userId").ascending());
        if (keyword != null && !keyword.isEmpty()) {
            return userRepository.findByUserNameContaining(keyword, pageable);
        }
        return userRepository.findAll(pageable);
    }

    public Optional<User> findById(int id) {
        return userRepository.findById(id);
    }

    /**
     * 更新用户信息
     * @param userId 用户id
     * @param dto 要更新的用户数据传输对象
     */
    public void updateUser(int userId, UserOptDTO dto) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setEmail(dto.getEmail());
            user.setRole(dto.getRole());
            user.setBalance(BigDecimal.valueOf(dto.getBalance()));
            user.setOssSpace(dto.getOssSpace());
            userRepository.save(user); // 保存修改
        }
    }

}
