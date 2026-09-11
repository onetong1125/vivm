package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "img_pool")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImgPool {
    @EmbeddedId
    private ImgPoolId id;

    @OneToOne
    @MapsId("imgId")
    @JoinColumn(name = "img_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Img img;

    @ManyToOne
    @MapsId("poolId")
    @JoinColumn(name = "pool_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Pool pool;

}
