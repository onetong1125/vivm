package com.ttt.vivm.cycle.domain.resource.image.service;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageOccupancy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageOccupancy.Mode.READ;
import static com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageOccupancy.Mode.WRITE;

public final class ImageOccupancyPolicy {
    public Set<ImageOccupancy> forVmStart(int targetId, List<Integer> ancestorIds) {
        Set<ImageOccupancy> requested = new HashSet<>();
        requested.add(new ImageOccupancy(targetId, WRITE));
        for (int ancestorId : ancestorIds) {
            requested.add(new ImageOccupancy(ancestorId, READ));
        }
        return Set.copyOf(requested);
    }

    /** Held modes may be combined across holders for this check; ownership is needed for release. */
    public boolean canAcquire(Set<ImageOccupancy> requested, Set<ImageOccupancy> held) {
        for (ImageOccupancy request : requested) {
            int imageId = request.imageId();
            if (held.contains(new ImageOccupancy(imageId, WRITE))) {
                return false;
            }
            if (request.mode() == WRITE && held.contains(new ImageOccupancy(imageId, READ))) {
                return false;
            }
        }
        return true;
    }
}
