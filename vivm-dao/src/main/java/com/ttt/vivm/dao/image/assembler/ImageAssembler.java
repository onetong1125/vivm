package com.ttt.vivm.dao.image.assembler;

import com.ttt.vivm.cycle.domain.resource.image.model.entity.ImageE;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageDetails;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageSummary;
import com.ttt.vivm.dao.image.record.ImageRecord;

public final class ImageAssembler {
    private ImageAssembler() {
    }

    public static ImageSummary toSummary(ImageRecord record) {
        validateRelations(record);
        return new ImageSummary(record.id, record.name, storageKind(record), visibility(record),
                lifecycle(record), record.createDate, record.poolId, record.parentImageId, ownerId(record));
    }

    public static ImageDetails toDetails(ImageRecord record) {
        validateRelations(record);
        return new ImageDetails(record.id, record.name, record.size, record.backingImagePath,
                record.imagePath, storageKind(record), visibility(record), lifecycle(record),
                record.createDate, record.deleteDate, record.osType, record.description,
                record.tag, record.poolName, record.poolId, record.parentImageId,
                ownerId(record), createdBy(record), record.legacyStatus);
    }

    public static ImageE toEntity(ImageRecord record) {
        validateRelations(record);
        return new ImageE(record.id, record.name, record.poolId, record.parentImageId,
                storageKind(record), visibility(record), lifecycle(record), ownerId(record), createdBy(record));
    }

    private static void validateRelations(ImageRecord record) {
        if (record.poolCount != 1 || record.parentCount > 1) {
            throw new IllegalStateException("Invalid pool or parent relation for image " + record.id);
        }
        ownerId(record);
    }

    private static ImageStorageKind storageKind(ImageRecord record) {
        return record.parentImageId == null ? ImageStorageKind.BASE : ImageStorageKind.DELTA;
    }

    private static ImageVisibility visibility(ImageRecord record) {
        return switch (record.imageType) {
            case 0 -> ImageVisibility.PUBLIC;
            case 1 -> ImageVisibility.PRIVATE;
            default -> throw new IllegalStateException("Unknown image type for image " + record.id);
        };
    }

    private static ImageLifecycle lifecycle(ImageRecord record) {
        return switch (record.deleted) {
            case 0 -> ImageLifecycle.ACTIVE;
            case 1 -> ImageLifecycle.DELETED;
            default -> throw new IllegalStateException("Unknown deletion state for image " + record.id);
        };
    }

    private static Integer ownerId(ImageRecord record) {
        if (visibility(record) == ImageVisibility.PUBLIC) {
            return null;
        }
        if (record.linkedUserCount != 1) {
            throw new IllegalStateException("Private image " + record.id + " has "
                    + record.linkedUserCount + " owners");
        }
        return record.linkedUserId;
    }

    private static Integer createdBy(ImageRecord record) {
        return record.linkedUserCount == 1 ? record.linkedUserId : null;
    }
}
