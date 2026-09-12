package com.ttt.vivm.model;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.*;

import lombok.*;

@Data
@Embeddable
public class UserVmId implements Serializable {
    @Column(name = "user_id")
    private int userId;

    @Column(name = "vm_id")
    private int vmId;

    // Constructors
    public UserVmId() {}

    public UserVmId(int userId, int vmId) {
        this.userId = userId;
        this.vmId = vmId;
    }

}
