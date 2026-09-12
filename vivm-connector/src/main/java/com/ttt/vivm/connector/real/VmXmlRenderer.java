package com.ttt.vivm.connector.real;

final class VmXmlRenderer {

    String modelVm(String name, String diskPath, String isoPath) {
        return """
                <domain type='kvm'>
                  <name>%s</name><memory unit='GiB'>2</memory><vcpu>2</vcpu>
                  <os><type arch='x86_64'>hvm</type><boot dev='cdrom'/><boot dev='hd'/></os>
                  <devices>
                    <disk type='file' device='disk'><driver name='qemu' type='qcow2'/><source file='%s'/><target dev='vda' bus='virtio'/></disk>
                    <console type='pty'><target type='serial' port='0'/></console>
                    <disk type='file' device='cdrom'><driver name='qemu' type='raw'/><source file='%s'/><target dev='hdc' bus='ide'/><readonly/></disk>
                    <interface type='bridge'><source bridge='br0'/><model type='virtio'/></interface>
                    <graphics type='vnc' port='-1' listen='0.0.0.0'/>
                  </devices>
                </domain>
                """.formatted(XmlText.escape(name), XmlText.escape(diskPath), XmlText.escape(isoPath));
    }

    String cloneVm(String name, String diskPath) {
        return """
                <domain type='kvm'>
                  <name>%s</name><memory unit='GiB'>2</memory><vcpu>2</vcpu>
                  <os><type arch='x86_64'>hvm</type></os>
                  <devices>
                    <disk type='file' device='disk'><driver name='qemu' type='qcow2'/><source file='%s'/><target dev='vda' bus='virtio'/></disk>
                    <console type='pty'><target type='serial' port='0'/></console>
                    <interface type='bridge'><source bridge='br0'/><model type='virtio'/></interface>
                    <graphics type='vnc' port='-1' listen='0.0.0.0'/>
                  </devices>
                </domain>
                """.formatted(XmlText.escape(name), XmlText.escape(diskPath));
    }
}
