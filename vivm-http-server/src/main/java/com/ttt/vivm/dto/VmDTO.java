package com.ttt.vivm.dto;

import com.ttt.vivm.model.Vm;
import lombok.Data;

@Data
public class VmDTO {
    private int vmId;
    private String vmName;
    private int vmType;
    private int vmStatus;
    private Long vmSize;
    private String vmCreateTime;

    // 构造方法
    public VmDTO(Vm vm) {
        this.vmId = vm.getVmId();
        this.vmName = vm.getVmName();
        this.vmType = vm.getVmType();
        this.vmStatus = vm.getVmStatus();
        this.vmSize = vm.getVmSize();
        this.vmCreateTime = vm.getVmCreateTime().toString();
    }
}
