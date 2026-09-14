package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.Stack;

@Data
@Entity
@Table(name = "img")
public class Img {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "img_id")
    private int imgId;

    @Column(name = "img_name")
    private String imgName;

    @Column(columnDefinition = "BIGINT", name = "img_size")
    private long imgSize;

    @Column(name = "back_img_path")
    private String backImgPath;

    @Column(name = "vm_img_path")
    private String vmImgPath;

    @Column(name = "img_type")
    private int imgType;

    @Column(columnDefinition = "DATETIME", name = "create_date")
    private LocalDateTime createDate;

    @Column(columnDefinition = "DATETIME", name = "delete_date")
    private LocalDateTime deleteDate;

    @Column(name = "is_delete")
    private int isDelete;

    @Column(name = "os_type")
    private String osType;

    @Column(name = "description")
    private String description;

    @Column(name = "tag")
    private String tag;

    @Column(name = "status")
    private int status;

    @OneToMany(mappedBy = "img")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<UserImg> userImgs;

    @OneToMany(mappedBy = "img")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ImgVm> imgVms;

    @OneToMany(mappedBy = "parentImg", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ImgChild> children;

    @OneToOne(mappedBy = "img")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ImgPool imgPool;

    @OneToMany(mappedBy = "img")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<VersionImg> versionImgs;

    public static final int PUBLIC_IMG_TYPE = 0;    // 旧模型同时表示公共与基础，R2 后仅用于兼容旧路径

    public static final int PRIVATE_IMG_TYPE = 1;   // 旧模型同时表示私有与增量，R2 后仅用于兼容旧路径

    public static final int IS_DELETE = 1;

    public static final int NOT_DELETE = 0;

    public static final int FILE_LOCKED = 0;

    public static final int FILE_UNLOCK = 1;

    // hashcode and equals

}
