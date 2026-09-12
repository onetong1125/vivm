package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_vm")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVm {

    @EmbeddedId
    private UserVmId id; // 复合主键

    @ManyToOne
    @MapsId("userId") // 关联复合主键中的 userId
    @JoinColumn(name = "user_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private User user;

    @ManyToOne
    @MapsId("vmId") // 关联复合主键中的 vmId
    @JoinColumn(name = "vm_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Vm vm;

}
