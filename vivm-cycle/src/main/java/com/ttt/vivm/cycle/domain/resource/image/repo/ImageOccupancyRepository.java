package com.ttt.vivm.cycle.domain.resource.image.repo;

import java.time.LocalDateTime;

public interface ImageOccupancyRepository {
    /** Acquires the whole chain. A conflict leaves all existing holders unchanged. */
    boolean tryAcquireForVmStart(int imageId, int vmId, String holderId, LocalDateTime acquiredAt);
}
