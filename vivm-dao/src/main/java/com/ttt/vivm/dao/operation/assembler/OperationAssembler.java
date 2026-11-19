package com.ttt.vivm.dao.operation.assembler;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStatus;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStep;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationType;
import com.ttt.vivm.dao.operation.record.OperationRecord;

public final class OperationAssembler {
  private OperationAssembler() {}

  public static OperationRecord toRecord(OperationE operation) {
    OperationRecord record = new OperationRecord();
    record.id = operation.id();
    record.actorId = operation.actorId();
    record.operationType = operation.operationType().name();
    record.resourceType = operation.operationType().resourceType();
    record.status = operation.status().name();
    record.step = operation.step().name();
    record.createdAt = operation.createdAt();
    record.startedAt = operation.startedAt();
    record.finishedAt = operation.finishedAt();
    record.version = operation.version();
    return record;
  }

  public static OperationE toEntity(OperationRecord record) {
    return new OperationE(
        record.id,
        record.actorId,
        OperationType.valueOf(record.operationType),
        OperationStatus.valueOf(record.status),
        OperationStep.valueOf(record.step),
        record.createdAt,
        record.startedAt,
        record.finishedAt,
        record.version);
  }
}
