package com.ttt.vivm.cycle.domain.resource.image.service;

import com.ttt.vivm.cycle.domain.resource.image.model.entity.ImageE;
import com.ttt.vivm.cycle.domain.resource.image.model.entity.StoragePoolE;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageActor;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImagePolicyTest {
    private final ImagePolicy policy = new ImagePolicy();

    @Test
    void administratorCanViewButCannotDeriveFromAnotherUsersPrivateImage() {
        ImageE image = image(2, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE, ImageLifecycle.ACTIVE, 10);
        ImageActor admin = new ImageActor(20, true);

        assertTrue(policy.canView(admin, image));
        assertFalse(policy.canDerive(admin, image));
    }

    @Test
    void oneRootCanHaveMultipleSamePoolChildren() {
        StoragePoolE pool = new StoragePoolE(7, "pool", 1);
        ImageE root = image(1, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC, ImageLifecycle.ACTIVE, null);
        ImageE first = image(2, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE, ImageLifecycle.ACTIVE, 10);
        ImageE second = image(3, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE, ImageLifecycle.ACTIVE, 20);

        assertDoesNotThrow(() -> policy.validate(root, pool, List.of()));
        assertDoesNotThrow(() -> policy.validate(first, pool, List.of(root)));
        assertDoesNotThrow(() -> policy.validate(second, pool, List.of(root)));
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(image(4, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC,
                        ImageLifecycle.ACTIVE, null), pool, List.of()));
    }

    @Test
    void storageKindAndVisibilityAllowPrivateBaseAndPublicDelta() {
        StoragePoolE privatePool = new StoragePoolE(8, "private-pool", 10);
        ImageE privateBase = new ImageE(10, "private-base", 8, null, ImageStorageKind.BASE,
                ImageVisibility.PRIVATE, ImageLifecycle.ACTIVE, 20, 10);
        StoragePoolE publicPool = new StoragePoolE(7, "public-pool", 1);
        ImageE publicRoot = image(1, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC,
                ImageLifecycle.ACTIVE, null);
        ImageE publicMiddle = image(2, 1, ImageStorageKind.DELTA, ImageVisibility.PUBLIC,
                ImageLifecycle.ACTIVE, null);
        ImageE publicDelta = image(3, 2, ImageStorageKind.DELTA, ImageVisibility.PUBLIC,
                ImageLifecycle.ACTIVE, null);

        assertDoesNotThrow(() -> policy.validate(privateBase, privatePool, List.of()));
        assertDoesNotThrow(() -> policy.validate(publicDelta, publicPool, List.of(publicMiddle, publicRoot)));
    }

    @Test
    void deltaRequiresCompleteSamePoolAncestryToTheOneRoot() {
        StoragePoolE pool = new StoragePoolE(7, "pool", 1);
        ImageE root = image(1, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC, ImageLifecycle.ACTIVE, null);
        ImageE middle = image(2, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE, ImageLifecycle.ACTIVE, 10);
        ImageE leaf = image(3, 2, ImageStorageKind.DELTA, ImageVisibility.PRIVATE, ImageLifecycle.ACTIVE, 10);
        ImageE otherPoolParent = new ImageE(2, "other", 8, 1, ImageStorageKind.DELTA,
                ImageVisibility.PRIVATE, ImageLifecycle.ACTIVE, 10, 10);

        assertDoesNotThrow(() -> policy.validate(leaf, pool, List.of(middle, root)));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(leaf, pool, List.of(middle)));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(leaf, pool, List.of(otherPoolParent, root)));
    }

    @Test
    void cyclicAncestryCannotPassAsPoolTree() {
        StoragePoolE pool = new StoragePoolE(7, "pool", 1);
        ImageE first = image(2, 3, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);
        ImageE second = image(3, 2, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);
        ImageE leaf = image(4, 2, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);

        assertThrows(IllegalArgumentException.class, () -> policy.validate(leaf, pool, List.of(first, second, first)));
    }

    @Test
    void ancestorKindMustMatchItsPlaceInTheChain() {
        StoragePoolE pool = new StoragePoolE(7, "pool", 1);
        ImageE root = image(1, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC, ImageLifecycle.ACTIVE, null);
        ImageE invalidMiddle = image(2, 1, ImageStorageKind.BASE, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);
        ImageE leaf = image(3, 2, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);

        assertThrows(IllegalArgumentException.class, () -> policy.validate(leaf, pool, List.of(invalidMiddle, root)));
    }

    @Test
    void publicImageRequiresPublicAncestryAndNoPrivateOwner() {
        StoragePoolE pool = new StoragePoolE(7, "pool", 1);
        ImageE root = image(1, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC, ImageLifecycle.ACTIVE, null);
        ImageE privateMiddle = image(2, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);
        ImageE publicParent = image(3, 2, ImageStorageKind.DELTA, ImageVisibility.PUBLIC,
                ImageLifecycle.ACTIVE, null);
        ImageE publicLeaf = image(4, 3, ImageStorageKind.DELTA, ImageVisibility.PUBLIC,
                ImageLifecycle.ACTIVE, null);
        ImageE assignedPublic = image(5, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC,
                ImageLifecycle.ACTIVE, 10);

        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(publicLeaf, pool, List.of(publicParent, privateMiddle, root)));
        assertThrows(IllegalArgumentException.class,
                () -> policy.validate(assignedPublic, new StoragePoolE(7, "other", 5), List.of()));
    }

    @Test
    void privateImageRequiresOwnerAndSameOwnerForPrivateAncestors() {
        StoragePoolE pool = new StoragePoolE(7, "pool", 1);
        ImageE root = image(1, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC, ImageLifecycle.ACTIVE, null);
        ImageE aliceParent = image(2, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);
        ImageE aliceLeaf = image(3, 2, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);
        ImageE bobLeaf = image(4, 2, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 20);
        ImageE unowned = image(5, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, null);

        assertDoesNotThrow(() -> policy.validate(aliceLeaf, pool, List.of(aliceParent, root)));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(bobLeaf, pool, List.of(aliceParent, root)));
        assertThrows(IllegalArgumentException.class, () -> policy.validate(unowned, pool, List.of(root)));
    }

    @Test
    void deletedAncestorSupportsExistingDescendantButCannotBeNewParent() {
        StoragePoolE pool = new StoragePoolE(7, "pool", 1);
        ImageE root = image(1, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC, ImageLifecycle.ACTIVE, null);
        ImageE deletedParent = image(2, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.DELETED, 10);
        ImageE activeLeaf = image(3, 2, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);

        assertDoesNotThrow(() -> policy.validate(activeLeaf, pool, List.of(deletedParent, root)));
        assertFalse(policy.canDerive(new ImageActor(10, false), deletedParent));
        assertTrue(policy.canDerive(new ImageActor(10, false), root));
        assertFalse(policy.canDerive(new ImageActor(20, true), activeLeaf));
    }

    @Test
    void publicCreatorDoesNotRestrictVisibility() {
        ImageE publicRoot = image(1, null, ImageStorageKind.BASE, ImageVisibility.PUBLIC,
                ImageLifecycle.ACTIVE, null);
        ImageE privateLeaf = image(2, 1, ImageStorageKind.DELTA, ImageVisibility.PRIVATE,
                ImageLifecycle.ACTIVE, 10);

        assertTrue(policy.canView(new ImageActor(20, false), publicRoot));
        assertFalse(policy.canView(new ImageActor(20, false), privateLeaf));
        assertTrue(policy.canView(new ImageActor(10, false), privateLeaf));
    }

    private ImageE image(int id, Integer parentId, ImageStorageKind kind, ImageVisibility visibility,
                         ImageLifecycle lifecycle, Integer ownerId) {
        return new ImageE(id, "image-" + id, 7, parentId, kind, visibility, lifecycle, ownerId, 10);
    }
}
