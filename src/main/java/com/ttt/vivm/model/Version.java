package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "version")
public class Version {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "description")
    private String description;

    @OneToOne(mappedBy = "version")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private VersionPool versionPool;

    @OneToMany(mappedBy = "version")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<VersionImg> versionImgs;
}
