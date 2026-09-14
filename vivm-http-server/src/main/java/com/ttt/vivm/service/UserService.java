package com.ttt.vivm.service;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;
import com.ttt.vivm.cycle.domain.identity.repo.UserPage;
import com.ttt.vivm.cycle.domain.identity.repo.UserRepository;
import com.ttt.vivm.dto.RegisterUserRequest;
import com.ttt.vivm.dto.UserOptDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional("jdbcTransactionManager")
    public Map<String, String> registerUser(RegisterUserRequest request) {
        Map<String, String> errors = new HashMap<>();
        if (userRepository.findByUserName(request.userName()).isPresent()) {
            errors.put("usernameError", "用户名已存在");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            errors.put("emailError", "邮箱已注册");
        }
        if (!errors.isEmpty()) {
            return errors;
        }
        userRepository.save(UserE.registered(
                request.userName(),
                passwordEncoder.encode(request.userPassword()),
                request.email()));
        return null;
    }

    @Transactional("jdbcTransactionManager")
    public void deleteUserById(int userId) {
        userRepository.findById(userId).orElseThrow();
        userRepository.deleteById(userId);
    }

    public Page<UserE> getUsers(int page, int size, String keyword) {
        UserPage result = userRepository.findPage(page, size, keyword);
        return new PageImpl<>(result.users(), PageRequest.of(page, size), result.total());
    }

    public Optional<UserE> findById(int id) {
        return userRepository.findById(id);
    }

    /**
     * 更新用户信息
     * @param userId 用户id
     * @param dto 要更新的用户数据传输对象
     */
    @Transactional("jdbcTransactionManager")
    public void updateUser(int userId, UserOptDTO dto) {
        Optional<UserE> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            UserE user = userOptional.get().withProfile(
                    dto.getEmail(),
                    dto.getRole(),
                    BigDecimal.valueOf(dto.getBalance()),
                    dto.getOssSpace());
            userRepository.update(user);
        }
    }
}
