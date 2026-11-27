package com.ttt.vivm.cycle.domain.resource.machine.repo;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import java.util.Optional;

public interface VmStartRepository {
    Optional<Integer> findVmId(String vmName);

    void complete(int vmId, String vncEndpoint, OperationE updating, OperationE completed);

    void recordFailure(int vmId, OperationE executing, OperationE failed);
}
