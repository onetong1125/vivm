package com.ttt.vivm.cycle.domain.resource.image.model.entity;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;

public record ImageE(
        Integer id,
        String name,
        Integer poolId,
        Integer parentImageId,
        ImageStorageKind storageKind,
        ImageVisibility visibility,
        ImageLifecycle lifecycle,
        Integer ownerId,
        Integer createdBy) {
}
