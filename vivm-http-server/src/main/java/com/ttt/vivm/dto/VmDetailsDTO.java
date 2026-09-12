package com.ttt.vivm.dto;

import com.ttt.vivm.model.Vm;
import lombok.Data;

@Data
public class VmDetailsDTO {
    private int vmId;
    private String vmName;
    private int vmType;
    private int vmStatus;
    private long vmSize;
    private String vmCreateTime;
    private String vmVnc;
    private String vmMac;
    private String vmUuid;
    private String imageName;

    // 构造方法
    public VmDetailsDTO(Vm vm) {
        this.vmId = vm.getVmId();
        this.vmName = vm.getVmName();
        this.vmType = vm.getVmType();
        this.vmStatus = vm.getVmStatus();
        this.vmSize = vm.getVmSize();
        this.vmCreateTime = vm.getVmCreateTime().toString();
        this.vmVnc = vm.getVmVnc();
        this.vmMac = vm.getVmMac();
        this.vmUuid = vm.getVmUuid();
        this.imageName = vm.getImgVm().getImg().getImgName();
    }
}
