package com.ttt.vivm.cycle.domain.resource.image.repo;

import java.time.LocalDateTime;

public interface ImageOccupancyRepository {
    /** Acquires the whole chain. A conflict leaves all existing holders unchanged. */
    boolean tryAcquireForVmStart(int imageId, int vmId, String holderId, LocalDateTime acquiredAt);

    /** Releases the chain for one VM start. Repeating the release is safe. */
    void releaseForVm(int vmId, String holderId);
}
