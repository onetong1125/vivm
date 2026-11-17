package com.ttt.vivm.dao.image.repo.impl;

import com.ttt.vivm.cycle.domain.resource.image.model.entity.ImageE;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageActor;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageCatalog;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageDetails;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageSummary;
import com.ttt.vivm.cycle.domain.resource.image.repo.ImageRepository;
import com.ttt.vivm.dao.image.assembler.ImageAssembler;
import com.ttt.vivm.dao.image.mapper.ImageMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class ImageRepositoryImpl implements ImageCatalog, ImageRepository {
    private final ImageMapper mapper;

    public ImageRepositoryImpl(ImageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ImageSummary> listVisible(ImageActor actor, String search) {
        return mapper.selectActiveVisible(actor.id(), actor.admin(),
                        search == null || search.isBlank() ? null : search)
                .stream().map(ImageAssembler::toSummary).toList();
    }

    @Override
    public Optional<ImageDetails> getVisible(ImageActor actor, int imageId) {
        return Optional.ofNullable(mapper.selectActiveVisibleById(actor.id(), actor.admin(), imageId))
                .map(ImageAssembler::toDetails);
    }

    @Override
    public Optional<ImageE> findByIdIncludingDeleted(int imageId) {
        return Optional.ofNullable(mapper.selectById(imageId)).map(ImageAssembler::toEntity);
    }

    @Override
    public List<ImageE> findAncestorsIncludingDeleted(int imageId) {
        List<ImageE> ancestors = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        ImageE image = findByIdIncludingDeleted(imageId).orElse(null);
        while (image != null && image.parentImageId() != null) {
            if (!visited.add(image.id())) {
                throw new IllegalStateException("Cyclic image ancestry at " + image.id());
            }
            image = findByIdIncludingDeleted(image.parentImageId())
                    .orElseThrow(() -> new IllegalStateException("Missing image ancestor"));
            ancestors.add(image);
        }
        return ancestors;
    }
}
