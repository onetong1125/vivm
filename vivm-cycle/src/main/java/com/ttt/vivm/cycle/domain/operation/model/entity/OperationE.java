package com.ttt.vivm.cycle.domain.operation.model.entity;

import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStatus;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStep;

import java.time.LocalDateTime;

public record OperationE(
    String id,
    int actorId,
    OperationStatus status,
    OperationStep step,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt) {

  public static OperationE start(
      String id,
      int actorId,
      LocalDateTime now) {
    return new OperationE(
        id,
        actorId,
        OperationStatus.RUNNING,
        OperationStep.VALIDATING,
        now,
        now,
        null);
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
        id, actorId, OperationStatus.SUCCEEDED, step,
        createdAt, startedAt, completedAt);
  }

  private OperationE advance(OperationStep expected, OperationStep next) {
    if (status != OperationStatus.RUNNING || step != expected) {
      throw new IllegalStateException("Cannot advance operation " + id + " from " + status + "/" + step + " to " + next);
    }
    return new OperationE(
        id, actorId, status, next,
        createdAt, startedAt, finishedAt);
  }

  public OperationE requireReview() {
    if (status != OperationStatus.RUNNING) {
      throw new IllegalStateException(
          "Cannot mark operation " + id + " for review from " + status + "/" + step);
    }
    return new OperationE(
        id, actorId, OperationStatus.REQUIRES_REVIEW, step,
        createdAt, startedAt, finishedAt);
  }

  public OperationE failValidation(LocalDateTime failedAt) {
    if (status != OperationStatus.RUNNING || step != OperationStep.VALIDATING) {
      throw new IllegalStateException(
          "Cannot fail validation for operation " + id + " from " + status + "/" + step);
    }
    return new OperationE(
        id, actorId, OperationStatus.FAILED, step,
        createdAt, startedAt, failedAt);
  }
}
