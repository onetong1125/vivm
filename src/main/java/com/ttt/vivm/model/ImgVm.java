package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "img_vm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImgVm {
    @EmbeddedId
    private ImgVmId id; // 复合主键

    @ManyToOne
    @MapsId("imgId") // 关联复合主键中的 imgId
    @JoinColumn(name = "img_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Img img;

    @OneToOne
    @MapsId("vmId") // 关联复合主键中的 vmId
    @JoinColumn(name = "vm_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Vm vm;

    // hashcode and equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImgVm imgVm = (ImgVm) o;
        return Objects.equals(getImg(), imgVm.getImg()) && Objects.equals(getVm(), imgVm.getVm());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getImg(), getVm());
    }
}
