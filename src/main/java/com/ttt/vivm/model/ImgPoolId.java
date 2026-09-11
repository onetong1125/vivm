package com.ttt.vivm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;

@Embeddable
@Data
public class ImgPoolId implements Serializable {
    @Column(name = "img_id")
    private int imgId;

    @Column(name = "pool_id")
    private int poolId;

    // Constructors
    public ImgPoolId() {}

    public ImgPoolId(int imgId, int poolId) {
        this.imgId = imgId;
        this.poolId = poolId;
    }
}
