package com.ttt.vivm.dao.identity.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;
import com.ttt.vivm.cycle.domain.identity.repo.UserPage;
import com.ttt.vivm.cycle.domain.identity.repo.UserRepository;
import com.ttt.vivm.dao.identity.assembler.UserAssembler;
import com.ttt.vivm.dao.identity.mapper.UserMapper;
import com.ttt.vivm.dao.identity.record.UserRecord;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserE save(UserE user) {
        UserRecord record = UserAssembler.toRecord(user);
        userMapper.insert(record);
        return UserAssembler.toEntity(record);
    }

    @Override
    public Optional<UserE> findById(int id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(UserAssembler::toEntity);
    }

    @Override
    public Optional<UserE> findByUserName(String userName) {
        return selectOne(UserRecord::getUserName, userName);
    }

    @Override
    public Optional<UserE> findByEmail(String email) {
        return selectOne(UserRecord::getEmail, email);
    }

    @Override
    public UserPage findPage(int page, int size, String keyword) {
        LambdaQueryWrapper<UserRecord> query = new LambdaQueryWrapper<UserRecord>()
                .like(keyword != null && !keyword.isBlank(), UserRecord::getUserName, keyword)
                .orderByAsc(UserRecord::getId);
        Page<UserRecord> result = userMapper.selectPage(new Page<>(page + 1L, size), query);
        return new UserPage(result.getRecords().stream().map(UserAssembler::toEntity).toList(), result.getTotal());
    }

    @Override
    public void update(UserE user) {
        userMapper.updateById(UserAssembler.toRecord(user));
    }

    @Override
    public void deleteById(int id) {
        userMapper.deleteById(id);
    }

    private Optional<UserE> selectOne(
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<UserRecord, ?> column,
            Object value) {
        UserRecord record = userMapper.selectOne(new LambdaQueryWrapper<UserRecord>().eq(column, value));
        return Optional.ofNullable(record).map(UserAssembler::toEntity);
    }
}
