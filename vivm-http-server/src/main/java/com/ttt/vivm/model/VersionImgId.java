package com.ttt.vivm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VersionImgId implements Serializable {
    @Column(name = "version_id")
    private int versionId;

    @Column(name = "img_id")
    private int imgId;
}
