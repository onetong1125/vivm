package com.ttt.vivm.connector;

import com.ttt.vivm.cycle.port.BackupGateway;
import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.cycle.port.ImageStorageGateway;
import com.ttt.vivm.cycle.port.VirtualizationGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Component
@Profile({"demo", "test"})
public class FakeExternalEnvironment implements ImageStorageGateway, VirtualizationGateway, BackupGateway {

    private enum NextOutcome {
        FAILED,
        UNKNOWN
    }

    private final Map<String, String> pools = new ConcurrentHashMap<>();
    private final Map<String, StoredImage> images = new ConcurrentHashMap<>();
    private final Map<String, VirtualMachineRuntime> virtualMachines = new ConcurrentHashMap<>();
    private final Set<String> backups = ConcurrentHashMap.newKeySet();
    private final Map<FakeExternalOperation, NextOutcome> nextOutcomes = new ConcurrentHashMap<>();
    private final Map<FakeExternalOperation, BlockedOperation> blockedOperations = new ConcurrentHashMap<>();

    public void failNext(FakeExternalOperation operation) {
        nextOutcomes.put(operation, NextOutcome.FAILED);
    }

    public void loseResponseAfterNext(FakeExternalOperation operation) {
        nextOutcomes.put(operation, NextOutcome.UNKNOWN);
    }

    public BlockedOperation blockNext(FakeExternalOperation operation) {
        BlockedOperation blocked = new BlockedOperation();
        blockedOperations.put(operation, blocked);
        return blocked;
    }

    public boolean hasPool(String poolName) {
        return pools.containsKey(poolName);
    }

    public boolean hasImage(String poolName, String imageName) {
        return images.containsKey(imageKey(poolName, imageName));
    }

    public boolean hasVirtualMachine(String vmName) {
        return virtualMachines.containsKey(vmName);
    }

    @Override
    public void createPool(String poolName, String directory) {
        execute(FakeExternalOperation.CREATE_POOL, () -> {
            if (pools.putIfAbsent(poolName, directory) != null) {
                throw failed(FakeExternalOperation.CREATE_POOL, "Pool already exists: " + poolName);
            }
        });
    }

    @Override
    public StoredImage createBaseImage(String poolName, String imageName, long sizeGiB) {
        return execute(FakeExternalOperation.CREATE_BASE_IMAGE, () -> {
            String directory = requiredPool(poolName);
            StoredImage image = new StoredImage(sizeGiB * 1024L * 1024L * 1024L,
                    directory + "/" + imageName + ".qcow2");
            if (images.putIfAbsent(imageKey(poolName, imageName), image) != null) {
                throw failed(FakeExternalOperation.CREATE_BASE_IMAGE, "Image already exists: " + imageName);
            }
            return image;
        });
    }

    @Override
    public String createDeltaImage(String poolName, String imageName, String backingImagePath) {
        return execute(FakeExternalOperation.CREATE_DELTA_IMAGE, () -> {
            String directory = requiredPool(poolName);
            StoredImage image = new StoredImage(0, directory + "/" + imageName + ".qcow2");
            if (images.putIfAbsent(imageKey(poolName, imageName), image) != null) {
                throw failed(FakeExternalOperation.CREATE_DELTA_IMAGE, "Image already exists: " + imageName);
            }
            return image.path();
        });
    }

    @Override
    public void deleteImage(String poolName, String imageName) {
        execute(FakeExternalOperation.DELETE_IMAGE, () -> {
            if (images.remove(imageKey(poolName, imageName)) == null) {
                throw failed(FakeExternalOperation.DELETE_IMAGE, "Image does not exist: " + imageName);
            }
        });
    }

    @Override
    public VirtualMachineRuntime createModelVm(String vmName, String diskPath, String isoPath) {
        return createVm(vmName, diskPath);
    }

    @Override
    public VirtualMachineRuntime createCloneVm(String vmName, String diskPath) {
        return createVm(vmName, diskPath);
    }

    @Override
    public VirtualMachineRuntime startVm(String vmName) {
        return execute(FakeExternalOperation.START_VM,
                () -> replaceVmState(FakeExternalOperation.START_VM, vmName, true));
    }

    @Override
    public VirtualMachineRuntime forceStopVm(String vmName) {
        return execute(FakeExternalOperation.FORCE_STOP_VM,
                () -> replaceVmState(FakeExternalOperation.FORCE_STOP_VM, vmName, false));
    }

    @Override
    public VirtualMachineRuntime restartVm(String vmName) {
        return execute(FakeExternalOperation.RESTART_VM,
                () -> replaceVmState(FakeExternalOperation.RESTART_VM, vmName, true));
    }

    @Override
    public void deleteVm(String vmName) {
        execute(FakeExternalOperation.DELETE_VM, () -> {
            if (virtualMachines.remove(vmName) == null) {
                throw failed(FakeExternalOperation.DELETE_VM, "VM does not exist: " + vmName);
            }
        });
    }

    @Override
    public void createBackup(String poolName, String versionName) {
        execute(FakeExternalOperation.CREATE_BACKUP, () -> {
            requiredPool(poolName);
            backups.add(poolName + ":" + versionName);
        });
    }

    @Override
    public void restoreBackup(String poolName, String versionName) {
        execute(FakeExternalOperation.RESTORE_BACKUP, () -> {
            if (!backups.contains(poolName + ":" + versionName)) {
                throw failed(FakeExternalOperation.RESTORE_BACKUP,
                        "Backup does not exist: " + poolName + "/" + versionName);
            }
        });
    }

    private VirtualMachineRuntime createVm(String vmName, String diskPath) {
        return execute(FakeExternalOperation.CREATE_VM, () -> {
            VirtualMachineRuntime runtime = new VirtualMachineRuntime(
                    true,
                    10L * 1024L * 1024L * 1024L,
                    "02:00:00:00:00:01",
                    "127.0.0.1:5900",
                    UUID.nameUUIDFromBytes(vmName.getBytes(StandardCharsets.UTF_8)).toString());
            if (virtualMachines.putIfAbsent(vmName, runtime) != null) {
                throw failed(FakeExternalOperation.CREATE_VM, "VM already exists: " + vmName);
            }
            return runtime;
        });
    }

    private VirtualMachineRuntime replaceVmState(FakeExternalOperation operation, String vmName, boolean running) {
        VirtualMachineRuntime current = virtualMachines.get(vmName);
        if (current == null) {
            throw failed(operation, "VM does not exist: " + vmName);
        }
        VirtualMachineRuntime updated = new VirtualMachineRuntime(
                running,
                current.diskCapacityBytes(),
                current.macAddress(),
                current.vncEndpoint(),
                current.uuid());
        virtualMachines.put(vmName, updated);
        return updated;
    }

    private String requiredPool(String poolName) {
        String directory = pools.get(poolName);
        if (directory == null) {
            throw failed(FakeExternalOperation.CREATE_BASE_IMAGE, "Pool does not exist: " + poolName);
        }
        return directory;
    }

    private String imageKey(String poolName, String imageName) {
        return poolName + ":" + imageName;
    }

    private void execute(FakeExternalOperation operation, Runnable sideEffect) {
        execute(operation, () -> {
            sideEffect.run();
            return null;
        });
    }

    private <T> T execute(FakeExternalOperation operation, Operation<T> sideEffect) {
        waitIfBlocked(operation);
        NextOutcome outcome = nextOutcomes.remove(operation);
        if (outcome == NextOutcome.FAILED) {
            throw failed(operation, "Injected external failure");
        }
        T result = sideEffect.run();
        if (outcome == NextOutcome.UNKNOWN) {
            throw new ExternalOperationException(operation.name(), ExternalOperationException.Outcome.UNKNOWN,
                    "External side effect completed but its response was lost");
        }
        return result;
    }

    private void waitIfBlocked(FakeExternalOperation operation) {
        BlockedOperation blocked = blockedOperations.remove(operation);
        if (blocked != null) {
            blocked.awaitRelease(operation);
        }
    }

    private ExternalOperationException failed(FakeExternalOperation operation, String message) {
        return new ExternalOperationException(operation.name(), ExternalOperationException.Outcome.FAILED, message);
    }

    @FunctionalInterface
    private interface Operation<T> {
        T run();
    }

    public static final class BlockedOperation {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);

        public boolean awaitStarted(Duration timeout) throws InterruptedException {
            return started.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        public void release() {
            released.countDown();
        }

        private void awaitRelease(FakeExternalOperation operation) {
            started.countDown();
            try {
                released.await();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new ExternalOperationException(operation.name(), ExternalOperationException.Outcome.FAILED,
                        "Fake operation was interrupted before execution", error);
            }
        }
    }
}
