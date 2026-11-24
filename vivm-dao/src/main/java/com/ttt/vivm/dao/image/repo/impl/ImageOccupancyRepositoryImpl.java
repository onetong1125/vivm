package com.ttt.vivm.dao.image.repo.impl;

import com.ttt.vivm.cycle.domain.resource.image.model.entity.ImageE;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageOccupancy;
import com.ttt.vivm.cycle.domain.resource.image.repo.ImageOccupancyRepository;
import com.ttt.vivm.cycle.domain.resource.image.repo.ImageRepository;
import com.ttt.vivm.cycle.domain.resource.image.service.ImageOccupancyPolicy;
import com.ttt.vivm.dao.image.mapper.ImageOccupancyMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ImageOccupancyRepositoryImpl implements ImageOccupancyRepository {
    private final ImageOccupancyMapper mapper;
    private final ImageRepository images;
    private final ImageOccupancyPolicy policy = new ImageOccupancyPolicy();

    public ImageOccupancyRepositoryImpl(ImageOccupancyMapper mapper, ImageRepository images) {
        this.mapper = mapper;
        this.images = images;
    }

    @Override
    @Transactional(transactionManager = "jdbcTransactionManager", isolation = Isolation.READ_COMMITTED)
    public boolean tryAcquireForVmStart(int imageId, int vmId, String holderId, LocalDateTime acquiredAt) {
        int poolId = mapper.lockPoolForImage(imageId);
        List<ImageE> ancestors = images.findAncestorsIncludingDeleted(imageId);
        if (ancestors.stream().anyMatch(image -> image.poolId() != poolId)) {
            throw new IllegalStateException("Image ancestry crosses the locked pool for image " + imageId);
        }
        Set<ImageOccupancy> requested = policy.forVmStart(imageId, ancestors.stream().map(ImageE::id).toList());
        if (!policy.canAcquire(requested, Set.copyOf(mapper.findHeld(requested)))) {
            return false;
        }
        mapper.insertGroup(requested, holderId, vmId, acquiredAt);
        return true;
    }

    @Override
    public void releaseForVm(int vmId, String holderId) {
        mapper.deleteForVm(vmId, holderId);
    }
}
