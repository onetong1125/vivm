package com.ttt.vivm.connector.real;

import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.cycle.port.ImageStorageGateway;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StorageVol;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("real")
final class LibvirtImageStorageAdapter implements ImageStorageGateway {

    private final LibvirtConnection libvirt;
    private final SshCommandExecutor ssh;
    private final ImageXmlRenderer imageXml = new ImageXmlRenderer();

    LibvirtImageStorageAdapter(LibvirtConnection libvirt, SshCommandExecutor ssh) {
        this.libvirt = libvirt;
        this.ssh = ssh;
    }

    @Override
    public void createPool(String poolName, String directory) {
        ssh.execute("CREATE_POOL_DIRECTORY", "mkdir -- " + ShellArgument.quote(directory));
        ssh.execute("DEFINE_POOL", "virsh -c qemu:///system pool-define-as "
                + ShellArgument.quote(poolName) + " dir --target " + ShellArgument.quote(directory));
        ssh.execute("AUTOSTART_POOL", "virsh -c qemu:///system pool-autostart "
                + ShellArgument.quote(poolName));
        ssh.execute("START_POOL", "virsh -c qemu:///system pool-start " + ShellArgument.quote(poolName));
    }

    @Override
    public StoredImage createBaseImage(String poolName, String imageName, long sizeGiB) {
        try {
            StoragePool pool = libvirt.get().storagePoolLookupByName(poolName);
            StorageVol volume = pool.storageVolCreateXML(imageXml.baseImage(imageName, sizeGiB), 0);
            return new StoredImage(volume.getInfo().capacity, volume.getPath());
        } catch (LibvirtException error) {
            throw unknown("CREATE_BASE_IMAGE", error);
        }
    }

    @Override
    public String createDeltaImage(String poolName, String imageName, String backingImagePath) {
        String path = "/var/lib/libvirt/images/" + poolName + "/" + imageName + ".qcow2";
        ssh.execute("CREATE_DELTA_IMAGE", "qemu-img create -f qcow2 -F qcow2 -b "
                + ShellArgument.quote(backingImagePath) + " " + ShellArgument.quote(path));
        refresh(poolName);
        return path;
    }

    @Override
    public void deleteImage(String poolName, String imageName) {
        ssh.execute("DELETE_IMAGE", "virsh -c qemu:///system vol-delete "
                + ShellArgument.quote(imageName + ".qcow2") + " --pool " + ShellArgument.quote(poolName));
        refresh(poolName);
    }

    private void refresh(String poolName) {
        ssh.execute("REFRESH_POOL", "virsh -c qemu:///system pool-refresh " + ShellArgument.quote(poolName));
    }

    private ExternalOperationException unknown(String operation, LibvirtException error) {
        return new ExternalOperationException(
                operation,
                ExternalOperationException.Outcome.UNKNOWN,
                "Libvirt operation result is unknown: " + error.getMessage(),
                error);
    }
}
