package com.ttt.vivm.cycle.domain.resource.image.query;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageActor;

import java.util.List;
import java.util.Optional;

public interface ImageCatalog {
    List<ImageSummary> listVisible(ImageActor actor, String search);

    Optional<ImageDetails> getVisible(ImageActor actor, int imageId);
}
