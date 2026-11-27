package com.ttt.vivm.dao.operation.repo.impl;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.repo.PoolAdmissionRepository;
import com.ttt.vivm.cycle.domain.resource.image.repo.ImageOccupancyRepository;
import com.ttt.vivm.dao.operation.assembler.OperationAssembler;
import com.ttt.vivm.dao.operation.mapper.OperationMapper;
import com.ttt.vivm.dao.operation.mapper.PoolAdmissionMapper;
import com.ttt.vivm.dao.operation.record.OperationRecord;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PoolAdmissionRepositoryImpl implements PoolAdmissionRepository {
    private final PoolAdmissionMapper admissions;
    private final OperationMapper operations;
    private final ImageOccupancyRepository occupancy;

    public PoolAdmissionRepositoryImpl(PoolAdmissionMapper admissions, OperationMapper operations,
                                       ImageOccupancyRepository occupancy) {
        this.admissions = admissions;
        this.operations = operations;
        this.occupancy = occupancy;
    }

    @Override
    @Transactional(transactionManager = "jdbcTransactionManager", isolation = Isolation.READ_COMMITTED)
    public boolean tryAdmit(int poolId, OperationE operation) {
        admissions.lockPool(poolId);
        if (admissions.findActiveOperation(poolId) != null) {
            return false;
        }

        reserveAndCreateTask(poolId, operation, null);
        return true;
    }

    @Override
    @Transactional(transactionManager = "jdbcTransactionManager", isolation = Isolation.READ_COMMITTED)
    public boolean tryAdmitVmStart(int vmId, OperationE operation) {
        PoolAdmissionMapper.VmStartTarget target = admissions.lockVmStartTarget(vmId);
        if (target == null) {
            throw new NoSuchElementException("Missing image or pool binding for VM " + vmId);
        }
        if (admissions.findActiveOperation(target.poolId()) != null) {
            return false;
        }
        if (!occupancy.tryAcquireForVmStart(target.imageId(), vmId, operation.id(), operation.createdAt())) {
            return false;
        }
        reserveAndCreateTask(target.poolId(), operation, Integer.toString(vmId));
        return true;
    }

    private void reserveAndCreateTask(int poolId, OperationE operation, String resourceId) {
        admissions.reserve(poolId, operation.id(), operation.createdAt());
        OperationRecord record = OperationAssembler.toRecord(operation);
        record.poolId = poolId;
        record.resourceId = resourceId;
        operations.insert(record);
    }

    @Override
    public void release(int poolId, String operationId) {
        admissions.release(poolId, operationId);
    }
}
