package com.ttt.vivm.cycle.domain.resource.image.query;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;

import java.time.LocalDateTime;

public record ImageDetails(
        int id,
        String name,
        long size,
        String backingImagePath,
        String imagePath,
        ImageStorageKind storageKind,
        ImageVisibility visibility,
        ImageLifecycle lifecycle,
        LocalDateTime createDate,
        LocalDateTime deleteDate,
        String osType,
        String description,
        String tag,
        String poolName,
        int poolId,
        Integer parentImageId,
        Integer ownerId,
        Integer createdBy,
        int legacyStatus) {
}
