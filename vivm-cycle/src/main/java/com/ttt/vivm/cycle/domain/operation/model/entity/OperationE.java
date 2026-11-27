package com.ttt.vivm.cycle.domain.operation.model.entity;

import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStatus;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStep;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationType;
import java.time.LocalDateTime;

public record OperationE(
    String id,
    int actorId,
    OperationType operationType,
    OperationStatus status,
    OperationStep step,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    long version) {

  public static OperationE start(
      String id,
      int actorId,
      OperationType type,
      LocalDateTime now) {
    return new OperationE(
        id,
        actorId,
        type,
        OperationStatus.RUNNING,
        OperationStep.VALIDATING,
        now,
        now,
        null,
        0L);
  }

  public OperationE beginRemoteExecution() {
    return advance(OperationStep.VALIDATING, OperationStep.REMOTE_EXECUTING);
  }

  public OperationE markRemoteSucceeded() {
    return advance(OperationStep.REMOTE_EXECUTING, OperationStep.REMOTE_SUCCEEDED);
  }

  public OperationE beginMetadataUpdate() {
    return advance(OperationStep.REMOTE_SUCCEEDED, OperationStep.METADATA_UPDATING);
  }

  public OperationE succeed(LocalDateTime completedAt) {
    if (status != OperationStatus.RUNNING || step != OperationStep.METADATA_UPDATING) {
      throw new IllegalStateException("Cannot complete operation " + id + " from " + status + "/" + step);
    }
    return new OperationE(
        id, actorId, operationType, OperationStatus.SUCCEEDED, step,
        createdAt, startedAt, completedAt, version + 1);
  }

  private OperationE advance(OperationStep expected, OperationStep next) {
    if (status != OperationStatus.RUNNING || step != expected) {
      throw new IllegalStateException("Cannot advance operation " + id + " from " + status + "/" + step + " to " + next);
    }
    return new OperationE(
        id, actorId, operationType, status, next,
        createdAt, startedAt, finishedAt, version + 1);
  }

  public OperationE requireReview() {
    if (status != OperationStatus.RUNNING) {
      throw new IllegalStateException(
          "Cannot mark operation " + id + " for review from " + status + "/" + step);
    }
    return new OperationE(
        id, actorId, operationType, OperationStatus.REQUIRES_REVIEW, step,
        createdAt, startedAt, finishedAt, version + 1);
  }

  public OperationE failRemoteExecution(LocalDateTime failedAt) {
    if (status != OperationStatus.RUNNING || step != OperationStep.REMOTE_EXECUTING) {
      throw new IllegalStateException("Cannot fail remote operation " + id + " from " + status + "/" + step);
    }
    return new OperationE(id, actorId, operationType, OperationStatus.FAILED, step,
        createdAt, startedAt, failedAt, version + 1);
  }

  public OperationE failValidation(LocalDateTime failedAt) {
    if (status != OperationStatus.RUNNING || step != OperationStep.VALIDATING) {
      throw new IllegalStateException(
          "Cannot fail validation for operation " + id + " from " + status + "/" + step);
    }
    return new OperationE(
        id, actorId, operationType, OperationStatus.FAILED, step,
        createdAt, startedAt, failedAt, version + 1);
  }
}
