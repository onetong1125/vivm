package com.ttt.vivm.cycle.domain.operation.repo;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;

public interface PoolAdmissionRepository {
    /** Creates the operation only if it can reserve the pool in the same transaction. */
    boolean tryAdmit(int poolId, OperationE operation);

    void release(int poolId, String operationId);
}
