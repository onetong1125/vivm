package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Table(name = "version_img")
@NoArgsConstructor
@AllArgsConstructor
public class VersionImg {
    @EmbeddedId
    private VersionImgId versionImgId;

    @ManyToOne
    @MapsId("versionId")
    @JoinColumn(name = "version_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Version version;

    @ManyToOne
    @MapsId("imgId")
    @JoinColumn(name = "img_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Img img;
}
