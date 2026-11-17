package com.ttt.vivm.dto;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageDetails;

import java.time.LocalDateTime;

public record ImgDetailsDTO(
        int imgId,
        String imgName,
        long imgSize,
        String backImgPath,
        String vmImgPath,
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
        Integer ownerId) {

    public ImgDetailsDTO(ImageDetails image) {
        this(image.id(), image.name(), image.size(), image.backingImagePath(), image.imagePath(),
                image.storageKind(), image.visibility(), image.lifecycle(), image.createDate(),
                image.deleteDate(), image.osType(), image.description(), image.tag(),
                image.poolName(), image.poolId(), image.parentImageId(), image.ownerId());
    }
}
