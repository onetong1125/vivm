package com.ttt.vivm.connector.real;

import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.cycle.port.VirtualizationGateway;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Component
@Profile("real")
final class LibvirtVirtualizationAdapter implements VirtualizationGateway {

    private final LibvirtConnection libvirt;
    private final SshCommandExecutor ssh;
    private final String vncHost;
    private final VmXmlRenderer vmXml = new VmXmlRenderer();

    LibvirtVirtualizationAdapter(
            LibvirtConnection libvirt,
            SshCommandExecutor ssh,
            @Value("${vivm.vnc-host}") String vncHost) {
        this.libvirt = libvirt;
        this.ssh = ssh;
        this.vncHost = vncHost;
    }

    @Override
    public VirtualMachineRuntime createModelVm(String vmName, String diskPath, String isoPath) {
        return defineAndStart("CREATE_MODEL_VM", vmName, vmXml.modelVm(vmName, diskPath, isoPath));
    }

    @Override
    public VirtualMachineRuntime createCloneVm(String vmName, String diskPath) {
        return defineAndStart("CREATE_CLONE_VM", vmName, vmXml.cloneVm(vmName, diskPath));
    }

    @Override
    public VirtualMachineRuntime startVm(String vmName) {
        Domain domain = lookup("START_VM", vmName);
        try {
            domain.create();
            return runtime(domain);
        } catch (ExternalOperationException | NumberFormatException error) {
            // Metadata reads can fail after libvirt has already started the domain.
            throw new ExternalOperationException("START_VM", ExternalOperationException.Outcome.UNKNOWN,
                    "VM start could not be confirmed: " + error.getMessage(), error);
        } catch (LibvirtException error) {
            throw unknown("START_VM", error);
        }
    }

    @Override
    public VirtualMachineRuntime forceStopVm(String vmName) {
        Domain domain = lookup("FORCE_STOP_VM", vmName);
        try {
            domain.destroy();
            return runtime(domain);
        } catch (LibvirtException error) {
            throw unknown("FORCE_STOP_VM", error);
        }
    }

    @Override
    public VirtualMachineRuntime restartVm(String vmName) {
        Domain domain = lookup("RESTART_VM", vmName);
        try {
            domain.reboot(0);
            return runtime(domain);
        } catch (LibvirtException error) {
            throw unknown("RESTART_VM", error);
        }
    }

    @Override
    public void deleteVm(String vmName) {
        Domain domain = lookup("DELETE_VM", vmName);
        try {
            if (domain.isActive() == 1) {
                domain.destroy();
            }
            domain.undefine();
        } catch (LibvirtException error) {
            throw unknown("DELETE_VM", error);
        }
    }

    private VirtualMachineRuntime defineAndStart(String operation, String vmName, String xml) {
        try {
            Connect connection = libvirt.get();
            connection.domainDefineXML(xml).create();
            return runtime(connection.domainLookupByName(vmName));
        } catch (LibvirtException error) {
            throw unknown(operation, error);
        }
    }

    private Domain lookup(String operation, String vmName) {
        try {
            return libvirt.get().domainLookupByName(vmName);
        } catch (LibvirtException error) {
            throw new ExternalOperationException(
                    operation,
                    ExternalOperationException.Outcome.FAILED,
                    "VM does not exist in libvirt: " + vmName,
                    error);
        }
    }

    private VirtualMachineRuntime runtime(Domain domain) throws LibvirtException {
        return new VirtualMachineRuntime(
                domain.isActive() == 1,
                domain.blockInfo("vda").getCapacity(),
                extractMac(domain.getXMLDesc(0)),
                vncEndpoint(domain.getName()),
                domain.getUUIDString());
    }

    private String vncEndpoint(String vmName) {
        String output = ssh.execute("QUERY_VM_VNC", "virsh -c qemu:///system vncdisplay "
                + ShellArgument.quote(vmName)).stdout().trim();
        if (!output.startsWith(":")) {
            return null;
        }
        int display = Integer.parseInt(output.substring(1));
        return vncHost + ":" + (5900 + display);
    }

    private String extractMac(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList interfaces = document.getElementsByTagName("interface");
            for (int index = 0; index < interfaces.getLength(); index++) {
                Element networkInterface = (Element) interfaces.item(index);
                Element mac = (Element) networkInterface.getElementsByTagName("mac").item(0);
                if (mac != null) {
                    return mac.getAttribute("address");
                }
            }
            return null;
        } catch (Exception error) {
            throw new ExternalOperationException(
                    "READ_VM_METADATA",
                    ExternalOperationException.Outcome.FAILED,
                    "Unable to parse libvirt VM metadata",
                    error);
        }
    }

    private ExternalOperationException unknown(String operation, LibvirtException error) {
        return new ExternalOperationException(
                operation,
                ExternalOperationException.Outcome.UNKNOWN,
                "Libvirt operation result is unknown: " + error.getMessage(),
                error);
    }
}
