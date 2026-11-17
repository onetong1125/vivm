package com.ttt.vivm.dto;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageSummary;

import java.time.LocalDateTime;

public record ImgDTO(
        int imgId,
        String imgName,
        ImageStorageKind storageKind,
        ImageVisibility visibility,
        ImageLifecycle lifecycle,
        LocalDateTime createDate,
        int poolId,
        Integer parentImageId) {

    public ImgDTO(ImageSummary image) {
        this(image.id(), image.name(), image.storageKind(), image.visibility(),
                image.lifecycle(), image.createDate(), image.poolId(), image.parentImageId());
    }
}
