package com.ttt.vivm.dao.operation.repo.impl;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.repo.OperationRepository;
import com.ttt.vivm.cycle.domain.resource.image.repo.ImageOccupancyRepository;
import com.ttt.vivm.cycle.domain.resource.machine.repo.VmStartRepository;
import com.ttt.vivm.dao.operation.mapper.VmStartMapper;
import java.util.Optional;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VmStartRepositoryImpl implements VmStartRepository {
    private final VmStartMapper mapper;
    private final OperationRepository operations;
    private final ImageOccupancyRepository occupancy;

    public VmStartRepositoryImpl(VmStartMapper mapper, OperationRepository operations, ImageOccupancyRepository occupancy) {
        this.mapper = mapper;
        this.operations = operations;
        this.occupancy = occupancy;
    }

    @Override
    public Optional<Integer> findVmId(String vmName) {
        return mapper.findVmId(vmName);
    }

    @Override
    @Transactional("jdbcTransactionManager")
    public void complete(int vmId, String vncEndpoint, OperationE updating, OperationE completed) {
        operations.update(updating, completed);
        if (mapper.markRunning(vmId, vncEndpoint) != 1) {
            throw new DataRetrievalFailureException("Missing VM " + vmId);
        }
        mapper.markLegacyImageLocked(completed.id());
        mapper.releasePool(completed.id());
    }

    @Override
    @Transactional("jdbcTransactionManager")
    public void recordFailure(int vmId, OperationE executing, OperationE failed) {
        operations.update(executing, failed);
        occupancy.releaseForVm(vmId, failed.id());
        mapper.releasePool(failed.id());
    }
}
