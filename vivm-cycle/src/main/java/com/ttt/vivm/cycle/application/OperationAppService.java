package com.ttt.vivm.cycle.application;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import java.util.Optional;

public interface OperationAppService {
    Optional<OperationE> findOwnOperation(int actorId, String operationId);
}
