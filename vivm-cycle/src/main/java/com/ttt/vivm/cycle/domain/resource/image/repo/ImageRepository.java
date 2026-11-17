package com.ttt.vivm.cycle.domain.resource.image.repo;

import com.ttt.vivm.cycle.domain.resource.image.model.entity.ImageE;

import java.util.List;
import java.util.Optional;

public interface ImageRepository {
    Optional<ImageE> findByIdIncludingDeleted(int imageId);

    /** Ordered from direct parent to root, including deleted ancestors. */
    List<ImageE> findAncestorsIncludingDeleted(int imageId);
}
