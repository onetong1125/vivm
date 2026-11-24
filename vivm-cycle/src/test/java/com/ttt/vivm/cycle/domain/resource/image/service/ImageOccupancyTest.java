package com.ttt.vivm.cycle.domain.resource.image.service;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageOccupancy;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageOccupancy.Mode.READ;
import static com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageOccupancy.Mode.WRITE;
import static org.junit.jupiter.api.Assertions.*;

class ImageOccupancyTest {
    private final ImageOccupancyPolicy policy = new ImageOccupancyPolicy();

    @Test
    void startNeedsWriteOnTargetAndReadOnEveryAncestor() {
        assertEquals(Set.of(new ImageOccupancy(3, WRITE), new ImageOccupancy(2, READ), new ImageOccupancy(1, READ)),
                policy.forVmStart(3, List.of(2, 1)));
    }

    @Test
    void twoStartsCannotWriteTheSameImage() {
        var first = policy.forVmStart(1, List.of());
        var second = policy.forVmStart(1, List.of());

        assertTrue(policy.canAcquire(first, Set.of()));
        assertFalse(policy.canAcquire(second, first));
    }

    @Test
    void rootWriteAndGrandchildStartConflictInEitherOrder() {
        var root = policy.forVmStart(1, List.of());
        var grandchild = policy.forVmStart(3, List.of(2, 1));

        assertFalse(policy.canAcquire(grandchild, root));
        assertFalse(policy.canAcquire(root, grandchild));
    }

    @Test
    void siblingsCanWriteTheirOwnImagesAndShareAllAncestors() {
        var first = policy.forVmStart(3, List.of(2, 1));
        var sibling = policy.forVmStart(4, List.of(2, 1));

        assertTrue(policy.canAcquire(sibling, first));
    }
}
