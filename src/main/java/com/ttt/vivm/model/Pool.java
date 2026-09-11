package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@Data
@Entity
@Table(name = "pool")
public class Pool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "dir")
    private String dir;

    @OneToMany(mappedBy = "pool")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ImgPool> imgPools;

    @OneToMany(mappedBy = "pool")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<VersionPool> versionPools;
}
