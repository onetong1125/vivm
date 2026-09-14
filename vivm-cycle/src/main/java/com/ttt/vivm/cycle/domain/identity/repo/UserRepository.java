package com.ttt.vivm.cycle.domain.identity.repo;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;

import java.util.Optional;

public interface UserRepository {

    UserE save(UserE user);

    Optional<UserE> findById(int id);

    Optional<UserE> findByUserName(String userName);

    Optional<UserE> findByEmail(String email);

    UserPage findPage(int page, int size, String keyword);

    void update(UserE user);

    void deleteById(int id);
}
