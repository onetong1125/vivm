package com.ttt.vivm.dao.operation.repo.impl;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.repo.PoolAdmissionRepository;
import com.ttt.vivm.dao.operation.assembler.OperationAssembler;
import com.ttt.vivm.dao.operation.mapper.OperationMapper;
import com.ttt.vivm.dao.operation.mapper.PoolAdmissionMapper;
import com.ttt.vivm.dao.operation.record.OperationRecord;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PoolAdmissionRepositoryImpl implements PoolAdmissionRepository {
    private final PoolAdmissionMapper admissions;
    private final OperationMapper operations;

    public PoolAdmissionRepositoryImpl(PoolAdmissionMapper admissions, OperationMapper operations) {
        this.admissions = admissions;
        this.operations = operations;
    }

    @Override
    @Transactional(transactionManager = "jdbcTransactionManager", isolation = Isolation.READ_COMMITTED)
    public boolean tryAdmit(int poolId, OperationE operation) {
        admissions.lockPool(poolId);
        if (admissions.findActiveOperation(poolId) != null) {
            return false;
        }

        admissions.reserve(poolId, operation.id(), operation.createdAt());
        OperationRecord record = OperationAssembler.toRecord(operation);
        record.poolId = poolId;
        operations.insert(record);
        return true;
    }

    @Override
    public void release(int poolId, String operationId) {
        admissions.release(poolId, operationId);
    }
}
