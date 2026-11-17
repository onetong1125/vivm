package com.ttt.vivm.cycle.domain.resource.image.query;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;

import java.time.LocalDateTime;

public record ImageSummary(
        int id,
        String name,
        ImageStorageKind storageKind,
        ImageVisibility visibility,
        ImageLifecycle lifecycle,
        LocalDateTime createDate,
        int poolId,
        Integer parentImageId,
        Integer ownerId) {
}
