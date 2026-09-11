package com.ttt.vivm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;

@Embeddable
@Data
public class ImgVmId implements Serializable {
    @Column(name = "img_id")
    private int imgId;

    @Column(name = "vm_id")
    private int vmId;

    // Constructors
    public ImgVmId() {}

    public ImgVmId(int imgId, int vmId) {
        this.imgId = imgId;
        this.vmId = vmId;
    }
}
