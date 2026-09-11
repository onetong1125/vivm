package com.ttt.vivm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Entity
@Table(name = "vm")
public class Vm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vm_id")
    private int vmId;

    @Column(nullable = false, unique = true, name = "vm_name")
    private String vmName;

    @Column(nullable = false, name = "vm_type")
    private int vmType;

    @Column(nullable = false, name = "vm_status")
    private int vmStatus;

    @Column(columnDefinition = "BIGINT", name = "vm_size")
    private long vmSize;

    @Column(columnDefinition = "DATETIME", name = "vm_create_time")
    private LocalDateTime vmCreateTime;

    @Column(name = "vm_vnc")
    private String vmVnc;

    @Column(name = "vm_mac")
    private String vmMac;

    @Column(name = "vm_uuid")
    private String vmUuid;

    @OneToMany(mappedBy = "vm")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<UserVm> userVms;

    @OneToOne(mappedBy = "vm")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private ImgVm imgVm;

    public static final int MODEL_VM_TYPE = 0;

    public static final int CLONE_VM_TYPE = 1;

    public static final int STOPPED_STATUS = 0;

    public static final int RUNNING_STATUS = 1;

}
