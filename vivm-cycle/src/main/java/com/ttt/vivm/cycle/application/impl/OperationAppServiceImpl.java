package com.ttt.vivm.cycle.application.impl;

import com.ttt.vivm.cycle.application.OperationAppService;
import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.repo.OperationRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OperationAppServiceImpl implements OperationAppService {
    private final OperationRepository operations;

    public OperationAppServiceImpl(OperationRepository operations) {
        this.operations = operations;
    }

    @Override
    public Optional<OperationE> findOwnOperation(int actorId, String operationId) {
        return operations.findByIdAndActorId(operationId, actorId);
    }
}
