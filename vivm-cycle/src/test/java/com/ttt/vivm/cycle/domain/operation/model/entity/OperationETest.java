package com.ttt.vivm.cycle.domain.operation.model.entity;

import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStatus;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStep;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OperationETest {

  @Test
  void startCreatesRunningOperationAtValidationStep() {
    String operationId = "00000000-0000-0000-0000-000000000001";
    int actorId = 7;
    LocalDateTime now = LocalDateTime.of(2026, 11, 18, 10, 0);

    OperationE operation = OperationE.start(operationId, actorId, OperationType.START_VM, now);

    assertEquals(operationId, operation.id());
    assertEquals(actorId, operation.actorId());
    assertEquals(OperationStatus.RUNNING, operation.status());
    assertEquals(OperationStep.VALIDATING, operation.step());
    assertEquals(now, operation.createdAt());
    assertEquals(now, operation.startedAt());
    assertNull(operation.finishedAt());
  }

  @Test
  void remoteSuccessDoesNotCompleteTheOperation() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 18, 10, 0);
    OperationE operation = OperationE.start(
        "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, now);

    OperationE executing = operation.beginRemoteExecution();

    assertEquals(OperationStatus.RUNNING, executing.status());
    assertEquals(OperationStep.REMOTE_EXECUTING, executing.step());
    assertNull(executing.finishedAt());

    OperationE remoteSucceeded = executing.markRemoteSucceeded();

    assertEquals(OperationStatus.RUNNING, remoteSucceeded.status());
    assertEquals(OperationStep.REMOTE_SUCCEEDED, remoteSucceeded.step());
    assertNull(remoteSucceeded.finishedAt());
    assertEquals(now, remoteSucceeded.startedAt());
  }

  @Test
  void unknownRemoteOutcomeRequiresReviewAtTheLastKnownStep() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 18, 10, 0);
    OperationE executing = OperationE.start(
            "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, now)
        .beginRemoteExecution();

    OperationE pendingReview = executing.requireReview();

    assertEquals(OperationStatus.REQUIRES_REVIEW, pendingReview.status());
    assertEquals(OperationStep.REMOTE_EXECUTING, pendingReview.step());
  }

  @Test
  void ordinaryExecutionCannotAdvanceAnOperationAwaitingReview() {
    OperationE pendingReview = OperationE.start(
            "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, LocalDateTime.of(2026, 11, 19, 10, 0))
        .beginRemoteExecution()
        .requireReview();

    assertThrows(IllegalStateException.class, pendingReview::beginRemoteExecution);
    assertThrows(IllegalStateException.class, pendingReview::markRemoteSucceeded);
  }

  @Test
  void remoteExecutionCannotBeSkippedOrRepeated() {
    OperationE validating = OperationE.start(
        "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, LocalDateTime.of(2026, 11, 19, 10, 0));

    assertThrows(IllegalStateException.class, validating::markRemoteSucceeded);

    OperationE executing = validating.beginRemoteExecution();
    assertThrows(IllegalStateException.class, executing::beginRemoteExecution);
  }

  @Test
  void operationSucceedsAfterMetadataUpdateAndRecordsFinishTime() {
    LocalDateTime startedAt = LocalDateTime.of(2026, 11, 19, 10, 0);
    LocalDateTime finishedAt = startedAt.plusSeconds(3);
    OperationE remoteSucceeded = OperationE.start(
            "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, startedAt)
        .beginRemoteExecution()
        .markRemoteSucceeded();

    OperationE updating = remoteSucceeded.beginMetadataUpdate();
    assertEquals(OperationStep.METADATA_UPDATING, updating.step());
    assertEquals(OperationStatus.RUNNING, updating.status());
    assertNull(updating.finishedAt());

    OperationE completed = updating.succeed(finishedAt);
    assertEquals(OperationStatus.SUCCEEDED, completed.status());
    assertEquals(finishedAt, completed.finishedAt());
    assertEquals(startedAt, completed.startedAt());
  }

  @Test
  void cannotCompleteBeforeMetadataUpdateBegins() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 19, 10, 0);
    OperationE remoteSucceeded = OperationE.start(
            "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, now)
        .beginRemoteExecution()
        .markRemoteSucceeded();

    assertThrows(IllegalStateException.class, () -> remoteSucceeded.succeed(now));
  }

  @Test
  void cannotCompleteTwiceOrWhileAwaitingReview() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 19, 10, 0);
    OperationE updating = OperationE.start(
            "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, now)
        .beginRemoteExecution()
        .markRemoteSucceeded()
        .beginMetadataUpdate();

    assertThrows(IllegalStateException.class, () -> updating.succeed(now).succeed(now.plusSeconds(1)));
    assertThrows(IllegalStateException.class, () -> updating.requireReview().succeed(now));
  }

  @Test
  void successfulOperationCannotBeMarkedForReview() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 19, 10, 0);
    OperationE completed = OperationE.start(
            "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, now)
        .beginRemoteExecution()
        .markRemoteSucceeded()
        .beginMetadataUpdate()
        .succeed(now.plusSeconds(3));

    assertThrows(IllegalStateException.class, completed::requireReview);
  }

  @Test
  void validationFailureEndsOperationAtValidationStep() {
    LocalDateTime startedAt = LocalDateTime.of(2026, 11, 19, 14, 0);
    LocalDateTime failedAt = startedAt.plusSeconds(1);
    OperationE validating = OperationE.start(
        "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, startedAt);

    OperationE failed = validating.failValidation(failedAt);

    assertEquals(OperationStatus.FAILED, failed.status());
    assertEquals(OperationStep.VALIDATING, failed.step());
    assertEquals(failedAt, failed.finishedAt());
  }

  @Test
  void validationFailureCannotBeRecordedTwice() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 19, 14, 0);
    OperationE failed = OperationE.start(
            "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, now)
        .failValidation(now.plusSeconds(1));

    assertThrows(IllegalStateException.class, () -> failed.failValidation(now.plusSeconds(2)));
  }

  @Test
  void remoteExecutionCannotBeReportedAsValidationFailure() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 19, 14, 0);
    OperationE executing = OperationE.start(
            "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, now)
        .beginRemoteExecution();

    assertThrows(IllegalStateException.class, () -> executing.failValidation(now.plusSeconds(1)));
  }

  @Test
  void startVmOperationRetainsItsTypeWhenExecutionBegins() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 19, 15, 0);
    OperationE started = OperationE.start(
        "00000000-0000-0000-0000-000000000001", 7, OperationType.START_VM, now);

    OperationE executing = started.beginRemoteExecution();

    assertEquals(OperationType.START_VM, executing.operationType());
    assertEquals("VM", executing.operationType().resourceType());
  }
}
