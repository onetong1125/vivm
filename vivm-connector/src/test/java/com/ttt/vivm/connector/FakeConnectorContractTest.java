package com.ttt.vivm.connector;

import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.cycle.port.ImageStorageGateway;
import com.ttt.vivm.cycle.port.VirtualizationGateway;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeConnectorContractTest {

    @Test
    void shouldKeepExternalStateIndependentFromCaller() {
        FakeExternalEnvironment environment = new FakeExternalEnvironment();
        ImageStorageGateway imageGateway = environment;
        VirtualizationGateway virtualizationGateway = environment;

        imageGateway.createPool("pool-a", "/images/pool-a");
        ImageStorageGateway.StoredImage image = imageGateway.createBaseImage("pool-a", "base-a", 10);
        VirtualizationGateway.VirtualMachineRuntime vm = virtualizationGateway.createCloneVm(
                "vm-a", image.path());

        assertThat(image.path()).isEqualTo("/images/pool-a/base-a.qcow2");
        assertThat(vm.running()).isTrue();
        assertThat(environment.hasImage("pool-a", "base-a")).isTrue();
        assertThat(environment.hasVirtualMachine("vm-a")).isTrue();
    }

    @Test
    void shouldExposeKnownFailureWithoutCreatingResource() {
        FakeExternalEnvironment environment = new FakeExternalEnvironment();
        environment.failNext(FakeExternalOperation.CREATE_POOL);

        assertThatThrownBy(() -> environment.createPool("pool-a", "/images/pool-a"))
                .isInstanceOfSatisfying(ExternalOperationException.class, error ->
                        assertThat(error.outcome()).isEqualTo(ExternalOperationException.Outcome.FAILED));
        assertThat(environment.hasPool("pool-a")).isFalse();
    }

    @Test
    void shouldExposeUnknownResultAfterExternalSideEffect() {
        FakeExternalEnvironment environment = new FakeExternalEnvironment();
        environment.loseResponseAfterNext(FakeExternalOperation.CREATE_POOL);

        assertThatThrownBy(() -> environment.createPool("pool-a", "/images/pool-a"))
                .isInstanceOfSatisfying(ExternalOperationException.class, error ->
                        assertThat(error.outcome()).isEqualTo(ExternalOperationException.Outcome.UNKNOWN));
        assertThat(environment.hasPool("pool-a")).isTrue();
    }

    @Test
    void shouldAllowAControlledBlockingWindow() throws Exception {
        FakeExternalEnvironment environment = new FakeExternalEnvironment();
        FakeExternalEnvironment.BlockedOperation blocked = environment.blockNext(
                FakeExternalOperation.CREATE_POOL);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> call = executor.submit(() -> environment.createPool("pool-a", "/images/pool-a"));

            assertThat(blocked.awaitStarted(Duration.ofSeconds(1))).isTrue();
            assertThat(call.isDone()).isFalse();
            blocked.release();
            call.get();
            assertThat(environment.hasPool("pool-a")).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
