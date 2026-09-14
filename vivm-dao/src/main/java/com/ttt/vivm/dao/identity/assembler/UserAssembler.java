package com.ttt.vivm.dao.identity.assembler;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;
import com.ttt.vivm.cycle.domain.identity.model.valueobj.AccountStatus;
import com.ttt.vivm.dao.identity.record.UserRecord;

public final class UserAssembler {

    private UserAssembler() {
    }

    public static UserRecord toRecord(UserE user) {
        UserRecord record = new UserRecord();
        record.setId(user.id());
        record.setUserName(user.userName());
        record.setPasswordHash(user.passwordHash());
        record.setRole(user.role());
        record.setOssSpace(user.ossSpace());
        record.setEmail(user.email());
        record.setBalance(user.balance());
        record.setAccountStatus(user.accountStatus().name());
        return record;
    }

    public static UserE toEntity(UserRecord record) {
        return new UserE(
                record.getId(),
                record.getUserName(),
                record.getPasswordHash(),
                record.getRole(),
                record.getOssSpace(),
                record.getEmail(),
                record.getBalance(),
                AccountStatus.valueOf(record.getAccountStatus()));
    }
}
