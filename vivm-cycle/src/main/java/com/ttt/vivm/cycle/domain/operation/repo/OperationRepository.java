package com.ttt.vivm.cycle.domain.operation.repo;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;

import java.util.Optional;

public interface OperationRepository {

  void create(OperationE operation);

  Optional<OperationE> findById(String id);

  Optional<OperationE> findByIdAndActorId(String id, int actorId);

  /** Persists one transition if the stored version and status still match the expected operation. */
  boolean tryUpdate(OperationE expected, OperationE next);

  default OperationE update(OperationE expected, OperationE next) {
    if (!tryUpdate(expected, next)) {
      throw new java.util.ConcurrentModificationException("Stale operation " + expected.id());
    }
    return next;
  }
}
