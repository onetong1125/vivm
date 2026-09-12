package com.ttt.vivm.cycle.port;

public interface VirtualizationGateway {

    VirtualMachineRuntime createModelVm(String vmName, String diskPath, String isoPath);

    VirtualMachineRuntime createCloneVm(String vmName, String diskPath);

    VirtualMachineRuntime startVm(String vmName);

    VirtualMachineRuntime forceStopVm(String vmName);

    VirtualMachineRuntime restartVm(String vmName);

    void deleteVm(String vmName);

    record VirtualMachineRuntime(
            boolean running,
            long diskCapacityBytes,
            String macAddress,
            String vncEndpoint,
            String uuid) {
    }
}
