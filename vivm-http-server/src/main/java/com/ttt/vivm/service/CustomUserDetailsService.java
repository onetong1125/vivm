package com.ttt.vivm.service;

import com.ttt.vivm.model.User;
import com.ttt.vivm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 直接返回 User 对象（因为它已实现 UserDetails）
        return user;
    }
}
