package com.ttt.vivm.cycle.application;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import java.util.Optional;

public interface VmAppService {
    /** Empty means admission was refused and no operation was created. */
    Optional<OperationE> startVm(int actorId, String vmName);

    class StartFailure extends RuntimeException {
        private final OperationE operation;

        public StartFailure(OperationE operation, RuntimeException cause) {
            super(cause.getMessage(), cause);
            this.operation = operation;
        }

        public OperationE operation() {
            return operation;
        }
    }
}
