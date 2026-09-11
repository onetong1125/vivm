package com.ttt.vivm.service;

import org.springframework.stereotype.Service;

@Service
public class VmTemplateService {
    // 虚拟机配置文件模版
    private static final String MODEL_VM_TEMPLATE = """
        <domain type='kvm'>
          <name>{{vmName}}</name>
          <memory unit='GiB'>2</memory>
          <vcpu>2</vcpu>
          <os>
            <type arch='x86_64'>hvm</type>
            <boot dev='cdrom'/>         <!-- 先从CD-ROM启动 -->
            <boot dev='hd'/>
          </os>
          <devices>
            <!-- 硬盘 -->
            <disk type='file' device='disk'>
              <driver name='qemu' type='qcow2'/>
              <source file='{{baseImagePath}}'/>
              <target dev='vda' bus='virtio'/>
            </disk>
            
            <!-- 控制台 -->
            <console type='pty'>
                <target type='serial' port='0'/>
            </console>
        
            <!-- ISO 文件作为 CD-ROM -->
            <disk type='file' device='cdrom'>
              <driver name='qemu' type='raw'/>
              <source file='{{isoPath}}'/>
              <target dev='hdc' bus='ide'/>
              <readonly/>
            </disk>
        
            <!-- 桥接网络接口 -->
            <interface type='bridge'>          <!-- 桥接模式 -->
              <source bridge='br0'/>           <!-- 桥接接口名称 -->
              <model type='virtio'/>           <!-- 高性能虚拟网卡 -->
            </interface>
        
            <!-- 虚拟机启动后，libvirt自动分配一个VNC端口 -->
            <graphics type='vnc' port='-1' listen='0.0.0.0'/>
          </devices>
        </domain>
        """;

    private static final String CLONE_VM_TEMPLATE = """
            <domain type='kvm'>
              <name>{{vmName}}</name>
              <memory unit='GiB'>2</memory>
              <vcpu>2</vcpu>
              <os>
                <type arch='x86_64'>hvm</type>
              </os>
              <devices>
              
                <!-- 硬盘 -->
                <disk type='file' device='disk'>
                  <driver name='qemu' type='qcow2'/>
                  <source file='{{vmImagePath}}'/>
                  <target dev='vda' bus='virtio'/>
                </disk>
                
                <!-- 控制台 -->
                <console type='pty'>
                    <target type='serial' port='0'/>
                </console>
                
                <!-- 桥接网络接口 -->
                <interface type='bridge'>          <!-- 桥接模式 -->
                  <source bridge='br0'/>           <!-- 桥接接口名称 -->
                  <model type='virtio'/>           <!-- 高性能虚拟网卡 -->
                </interface>
                
                <!-- 虚拟机启动后，libvirt自动分配一个VNC端口 -->
                <graphics type='vnc' port='-1' listen='0.0.0.0'/>  
                
              </devices>
            </domain>
            """;

    public String generateModelVmXML(String vmName, String baseImagePath, String isoPath) {
        return MODEL_VM_TEMPLATE
                .replace("{{vmName}}", vmName)
                .replace("{{baseImagePath}}", baseImagePath)
                .replace("{{isoPath}}", isoPath);
    }

    public String generateCloneVmXML(String vmName, String vmImagePath) {
        return CLONE_VM_TEMPLATE
                .replace("{{vmName}}", vmName)
                .replace("{{vmImagePath}}", vmImagePath);
    }
}
