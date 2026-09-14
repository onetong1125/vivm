package com.ttt.vivm.service;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;
import com.ttt.vivm.cycle.domain.identity.repo.UserRepository;
import com.ttt.vivm.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserE user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        User principal = new User();
        principal.setUserId(user.id());
        principal.setUserName(user.userName());
        principal.setUserPassword(user.passwordHash());
        principal.setRole(user.role());
        principal.setOssSpace(user.ossSpace());
        principal.setEmail(user.email());
        principal.setBalance(user.balance());
        return principal;
    }
}
