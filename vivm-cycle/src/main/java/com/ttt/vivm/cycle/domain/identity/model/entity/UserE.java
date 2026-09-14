package com.ttt.vivm.cycle.domain.identity.model.entity;

import com.ttt.vivm.cycle.domain.identity.model.valueobj.AccountStatus;

import java.math.BigDecimal;

/**
 * 账户领域对象
 */
public record UserE(
        Integer id,
        String userName,
        String passwordHash,
        int role,
        long ossSpace,
        String email,
        BigDecimal balance,
        AccountStatus accountStatus) {

    public static UserE registered(String userName, String passwordHash, String email) {
        return new UserE(null, userName, passwordHash, 0, 0, email, new BigDecimal("0.00"), AccountStatus.ACTIVE);
    }

    public UserE withProfile(String email, int role, BigDecimal balance, long ossSpace) {
        return new UserE(id, userName, passwordHash, role, ossSpace, email, balance, accountStatus);
    }
}
