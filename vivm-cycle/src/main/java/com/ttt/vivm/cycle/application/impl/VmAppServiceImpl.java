package com.ttt.vivm.cycle.application.impl;

import com.ttt.vivm.cycle.application.VmAppService;
import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationType;
import com.ttt.vivm.cycle.domain.operation.repo.OperationRepository;
import com.ttt.vivm.cycle.domain.operation.repo.PoolAdmissionRepository;
import com.ttt.vivm.cycle.domain.resource.machine.repo.VmStartRepository;
import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.cycle.port.VirtualizationGateway;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class VmAppServiceImpl implements VmAppService {
    private final VmStartRepository vms;
    private final OperationRepository operations;
    private final PoolAdmissionRepository admissions;
    private final VirtualizationGateway gateway;

    public VmAppServiceImpl(VmStartRepository vms, OperationRepository operations, PoolAdmissionRepository admissions,
                            VirtualizationGateway gateway) {
        this.vms = vms;
        this.operations = operations;
        this.admissions = admissions;
        this.gateway = gateway;
    }

    @Override
    public Optional<OperationE> startVm(int actorId, String vmName) {
        int vmId = vms.findVmId(vmName).orElseThrow(() -> new NoSuchElementException("Unknown VM " + vmName));
        OperationE task = OperationE.start(UUID.randomUUID().toString(), actorId, OperationType.START_VM, LocalDateTime.now());
        if (!admissions.tryAdmitVmStart(vmId, task)) {
            return Optional.empty();
        }
        OperationE executing = operations.update(task, task.beginRemoteExecution());
        VirtualizationGateway.VirtualMachineRuntime runtime;
        try {
            runtime = gateway.startVm(vmName);
        } catch (ExternalOperationException error) {
            if (error.outcome() == ExternalOperationException.Outcome.FAILED) {
                OperationE failed = executing.failRemoteExecution(LocalDateTime.now());
                vms.recordFailure(vmId, executing, failed);
                throw new StartFailure(failed, error);
            }
            throw new StartFailure(operations.update(executing, executing.requireReview()), error);
        }
        if (!runtime.running()) {
            throw new StartFailure(operations.update(executing, executing.requireReview()),
                    new IllegalStateException("VM did not report a running state after start"));
        }
        OperationE current = executing;
        try {
            current = operations.update(current, current.markRemoteSucceeded());
            current = operations.update(current, current.beginMetadataUpdate());
            OperationE completed = current.succeed(LocalDateTime.now());
            vms.complete(vmId, runtime.vncEndpoint(), current, completed);
            return Optional.of(completed);
        } catch (DataAccessException error) {
            throw new StartFailure(operations.update(current, current.requireReview()), error);
        }
    }
}
