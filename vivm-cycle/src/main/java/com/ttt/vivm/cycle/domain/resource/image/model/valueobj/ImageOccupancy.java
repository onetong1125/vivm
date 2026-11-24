package com.ttt.vivm.cycle.domain.resource.image.model.valueobj;

public record ImageOccupancy(int imageId, Mode mode) {
    public enum Mode { READ, WRITE }
}
