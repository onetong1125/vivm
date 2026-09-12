package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "version_pool")
@NoArgsConstructor
@AllArgsConstructor
public class VersionPool {
    @EmbeddedId
    private VersionPoolId versionPoolId;

    @OneToOne
    @MapsId("versionId")
    @JoinColumn(name = "version_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Version version;

    @ManyToOne
    @MapsId("poolId")
    @JoinColumn(name = "pool_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Pool pool;
}
