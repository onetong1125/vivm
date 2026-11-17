package com.ttt.vivm.cycle.domain.resource.image.domainservice;

import com.ttt.vivm.cycle.domain.resource.image.model.entity.ImageE;
import com.ttt.vivm.cycle.domain.resource.image.model.entity.StoragePoolE;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageActor;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;

import java.util.List;
import java.util.Objects;

public final class ImagePolicy {
    public boolean canView(ImageActor actor, ImageE image) {
        return actor.admin() || image.visibility() == ImageVisibility.PUBLIC
                || image.ownerId().equals(actor.id());
    }

    /** Image eligibility only; this does not grant the later clone command or pool operation. */
    public boolean canDerive(ImageActor actor, ImageE parent) {
        return parent.lifecycle() == ImageLifecycle.ACTIVE
                && (parent.visibility() == ImageVisibility.PUBLIC || parent.ownerId().equals(actor.id()));
    }

    public void validate(ImageE image, StoragePoolE pool, List<ImageE> ancestors) {
        if (image.visibility() == ImageVisibility.PUBLIC && image.ownerId() != null) {
            throw new IllegalArgumentException("PUBLIC image cannot have a private owner");
        }
        if (image.visibility() == ImageVisibility.PRIVATE && image.ownerId() == null) {
            throw new IllegalArgumentException("PRIVATE image requires an owner");
        }
        if (!Objects.equals(image.poolId(), pool.id())) {
            throw new IllegalArgumentException("Image must belong to its storage pool");
        }
        if (image.storageKind() == ImageStorageKind.BASE) {
            if (image.parentImageId() != null || !ancestors.isEmpty()
                    || !Objects.equals(pool.rootImageId(), image.id())) {
                throw new IllegalArgumentException("Pool must have one BASE root");
            }
        } else if (image.parentImageId() == null || ancestors.isEmpty()
                || !Objects.equals(image.parentImageId(), ancestors.get(0).id())) {
            throw new IllegalArgumentException("DELTA must have its direct parent");
        }
        Integer expectedParent = image.parentImageId();
        for (int i = 0; i < ancestors.size(); i++) {
            ImageE ancestor = ancestors.get(i);
            if (!Objects.equals(ancestor.id(), expectedParent)
                    || !Objects.equals(ancestor.poolId(), pool.id())) {
                throw new IllegalArgumentException("Image ancestry must remain in one pool");
            }
            if (i < ancestors.size() - 1 && ancestor.storageKind() != ImageStorageKind.DELTA) {
                throw new IllegalArgumentException("Only the root may be BASE");
            }
            if (image.visibility() == ImageVisibility.PUBLIC && ancestor.visibility() != ImageVisibility.PUBLIC) {
                throw new IllegalArgumentException("PUBLIC image requires public ancestry");
            }
            if (image.visibility() == ImageVisibility.PRIVATE
                    && ancestor.visibility() == ImageVisibility.PRIVATE
                    && !Objects.equals(image.ownerId(), ancestor.ownerId())) {
                throw new IllegalArgumentException("PRIVATE ancestry must have the same owner");
            }
            expectedParent = ancestor.parentImageId();
        }
        if (!ancestors.isEmpty()) {
            ImageE root = ancestors.get(ancestors.size() - 1);
            if (expectedParent != null || root.storageKind() != ImageStorageKind.BASE
                    || !Objects.equals(root.id(), pool.rootImageId())) {
                throw new IllegalArgumentException("Image ancestry must reach the pool root");
            }
        }
    }
}
