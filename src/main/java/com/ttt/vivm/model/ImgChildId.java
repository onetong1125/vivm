package com.ttt.vivm.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
public class ImgChildId implements Serializable {
    private int imgId;
    private int imgChildId;

    public ImgChildId() {}

    public ImgChildId(int imgId, int imgChildId) {
        this.imgId = imgId;
        this.imgChildId = imgChildId;
    }

}