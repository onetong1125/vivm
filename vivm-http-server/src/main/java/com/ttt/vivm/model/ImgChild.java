package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "img_child")
public class ImgChild {

    @EmbeddedId
    private ImgChildId id;

    @ManyToOne
    @MapsId("imgId")
    @JoinColumn(name = "img_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Img parentImg;

    @ManyToOne
    @MapsId("imgChildId")
    @JoinColumn(name = "img_child_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Img childImg;

}