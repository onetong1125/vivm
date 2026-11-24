package com.ttt.vivm.dto;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStatus;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStep;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationType;

public record OperationViewDTO(String operationId, OperationType operationType,
                               OperationStatus status, OperationStep step, long version) {
    public OperationViewDTO(OperationE operation) {
        this(operation.id(), operation.operationType(), operation.status(), operation.step(), operation.version());
    }
}
