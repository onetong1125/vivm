package com.ttt.vivm.operation;

import com.ttt.vivm.connector.FakeExternalEnvironment;
import com.ttt.vivm.cycle.application.VmAppService;
import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationStatus;
import com.ttt.vivm.cycle.port.ExternalOperationException;
import com.ttt.vivm.cycle.port.VirtualizationGateway;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationType;
import com.ttt.vivm.cycle.domain.operation.repo.OperationRepository;
import com.ttt.vivm.cycle.domain.operation.repo.PoolAdmissionRepository;
import com.ttt.vivm.cycle.domain.resource.image.repo.ImageOccupancyRepository;
import com.ttt.vivm.support.MySqlIntegrationSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AdmissionConcurrencyIT {
    private static final String DATABASE = "vivm_r4_admission_it_"
            + UUID.randomUUID().toString().replace("-", "");
    private static final MySqlIntegrationSupport.DatabaseConfig CONFIG = MySqlIntegrationSupport.loadConfig();

    static {
        CONFIG.createDatabase(DATABASE);
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> CONFIG.databaseUrl(DATABASE));
        registry.add("spring.datasource.username", CONFIG::username);
        registry.add("spring.datasource.password", CONFIG::password);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @AfterAll
    static void removeDatabase() {
        CONFIG.dropDatabase(DATABASE);
    }

    @Autowired private ImageOccupancyRepository occupancy;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private VmAppService vmAppService;
    @MockitoBean private FakeExternalEnvironment gateway;
    @Autowired private PoolAdmissionRepository admissions;
    @Autowired private OperationRepository operations;

    @Test
    void concurrentStartsOnSameLeafCommitExactlyOneWholeChain() throws Exception {
        seedTree(100);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = workers.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return acquire(102, 1, "first");
            });
            Future<Boolean> second = workers.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return acquire(102, 2, "second");
            });
            boolean firstWon = first.get(20, TimeUnit.SECONDS);
            assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo(!firstWon);
            assertThat(rows(100)).containsExactly(
                    "100:READ", "101:READ", "102:WRITE");
            assertThat(jdbc.queryForList("SELECT DISTINCT holder_id FROM image_occupancy WHERE image_id BETWEEN 100 AND 103",
                    String.class)).containsExactly(firstWon ? "first" : "second");
            assertThat(jdbc.queryForList("SELECT DISTINCT vm_id FROM image_occupancy WHERE image_id BETWEEN 100 AND 103",
                    Integer.class)).containsExactly(firstWon ? 1 : 2);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void siblingStartsShareAncestorsAndBlockAWriterOnTheRoot() {
        seedTree(200);
        assertThat(acquire(202, 3, "left")).isTrue();
        assertThat(acquire(203, 4, "right")).isTrue();
        assertThat(acquire(200, 5, "root")).isFalse();
        assertThat(rows(200)).containsExactly("200:READ", "200:READ", "201:READ", "201:READ", "202:WRITE", "203:WRITE");
    }

    @Test
    void insertFailureLeavesNoPartialOccupancyForSibling() {
        seedTree(300);
        assertThat(acquire(302, 6, "original")).isTrue();
        assertThatThrownBy(() -> acquire(303, 7, "original")).isInstanceOf(DuplicateKeyException.class);
        assertThat(rows(300)).containsExactly("300:READ", "301:READ", "302:WRITE");
    }

    @Test
    void releasingOneVmKeepsSiblingReadsUntilTheLastHolderReleases() {
        seedTree(400);
        assertThat(acquire(402, 8, "vm8-first")).isTrue();
        assertThat(acquire(403, 9, "vm9-first")).isTrue();

        occupancy.releaseForVm(8, "vm8-first");
        assertThat(rows(400)).containsExactly("400:READ", "401:READ", "403:WRITE");
        assertThat(acquire(400, 10, "root-after-stop")).isFalse();

        occupancy.releaseForVm(9, "vm9-first");
        assertThat(acquire(400, 10, "root-after-stop")).isTrue();
    }

    @Test
    void staleReleaseCannotClearANewerStartOfTheSameVm() {
        seedTree(500);
        assertThat(acquire(502, 11, "previous-start")).isTrue();
        occupancy.releaseForVm(11, "previous-start");
        assertThat(acquire(502, 11, "current-start")).isTrue();

        occupancy.releaseForVm(11, "previous-start");
        occupancy.releaseForVm(12, "current-start");
        assertThat(rows(500)).containsExactly("500:READ", "501:READ", "502:WRITE");
        assertThat(acquire(502, 12, "contender")).isFalse();
    }

    @Test
    void concurrentAdmissionsCreateOnlyTheWinningTask() throws Exception {
        jdbc.update("INSERT INTO pool (id) VALUES (600)");
        OperationE left = newOperation();
        OperationE right = newOperation();
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = workers.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return admissions.tryAdmit(600, left);
            });
            Future<Boolean> second = workers.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                return admissions.tryAdmit(600, right);
            });
            boolean leftWon = first.get(20, TimeUnit.SECONDS);
            assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo(!leftWon);
            String winningId = leftWon ? left.id() : right.id();
            assertThat(jdbc.queryForList("SELECT operation_id FROM pool_admission WHERE pool_id = 600", String.class))
                    .containsExactly(winningId);
            assertThat(jdbc.queryForList("SELECT id FROM operation_task WHERE pool_id = 600", String.class))
                    .containsExactly(winningId);
            assertThat(operations.findById(leftWon ? right.id() : left.id())).isEmpty();
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void activePoolDoesNotBlockOtherPoolsAndOnlyItsHolderCanReleaseIt() {
        jdbc.update("INSERT INTO pool (id) VALUES (601), (602)");
        OperationE first = newOperation();
        OperationE other = newOperation();
        OperationE next = newOperation();
        assertThat(admissions.tryAdmit(601, first)).isTrue();
        assertThat(admissions.tryAdmit(602, other)).isTrue();

        admissions.release(601, other.id());
        assertThat(admissions.tryAdmit(601, next)).isFalse();
        admissions.release(601, first.id());
        assertThat(admissions.tryAdmit(601, next)).isTrue();
        admissions.release(601, first.id());
        assertThat(admissions.tryAdmit(601, newOperation())).isFalse();
    }

    @Test
    void failedTaskInsertRollsBackPoolReservation() {
        jdbc.update("INSERT INTO pool (id) VALUES (603)");
        OperationE existing = newOperation();
        operations.create(existing);

        assertThatThrownBy(() -> admissions.tryAdmit(603, existing)).isInstanceOf(DuplicateKeyException.class);
        assertThat(admissions.tryAdmit(603, newOperation())).isTrue();
    }

    @Test
    void vmStartReservesItsBoundPoolAndChainUnderTheOperationId() {
        seedTree(700);
        bindVm(21, 702);
        OperationE operation = newOperation();

        assertThat(admissions.tryAdmitVmStart(21, operation)).isTrue();

        assertThat(jdbc.queryForList("SELECT resource_id FROM operation_task WHERE id = ? AND pool_id = 700",
                String.class, operation.id())).containsExactly("21");
        assertThat(jdbc.queryForList("SELECT operation_id FROM pool_admission WHERE pool_id = 700", String.class))
                .containsExactly(operation.id());
        assertThat(rows(700)).containsExactly("700:READ", "701:READ", "702:WRITE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM image_occupancy WHERE holder_id = ? AND vm_id = 21",
                Integer.class, operation.id())).isEqualTo(3);
    }

    @Test
    void busyPoolRejectsStartBeforeCreatingTaskOrTakingOccupancy() {
        seedTree(710);
        bindVm(22, 712);
        OperationE active = newOperation();
        OperationE rejected = newOperation();
        assertThat(admissions.tryAdmit(710, active)).isTrue();

        assertThat(admissions.tryAdmitVmStart(22, rejected)).isFalse();

        assertThat(operations.findById(rejected.id())).isEmpty();
        assertThat(rows(710)).isEmpty();
        assertThat(jdbc.queryForList("SELECT operation_id FROM pool_admission WHERE pool_id = 710", String.class))
                .containsExactly(active.id());
    }

    @Test
    void occupiedAncestorRejectsStartWithoutReservingPool() {
        seedTree(720);
        bindVm(23, 722);
        assertThat(acquire(720, 24, "running-root")).isTrue();
        OperationE rejected = newOperation();

        assertThat(admissions.tryAdmitVmStart(23, rejected)).isFalse();

        assertThat(operations.findById(rejected.id())).isEmpty();
        assertThat(rows(720)).containsExactly("720:WRITE");
        assertThat(admissions.tryAdmit(720, newOperation())).isTrue();
    }

    @Test
    void taskInsertFailureRollsBackBothPoolAndImageReservations() {
        seedTree(730);
        bindVm(25, 732);
        OperationE duplicate = newOperation();
        operations.create(duplicate);

        assertThatThrownBy(() -> admissions.tryAdmitVmStart(25, duplicate)).isInstanceOf(DuplicateKeyException.class);

        assertThat(rows(730)).isEmpty();
        assertThat(admissions.tryAdmitVmStart(25, newOperation())).isTrue();
    }

    @Test
    void busyPoolPreventsRemoteStart() {
        seedTree(800);
        bindVm(31, 802);
        assertThat(admissions.tryAdmit(800, newOperation())).isTrue();
        assertThat(vmAppService.startVm(7, "vm-31")).isEmpty();
        verifyNoInteractions(gateway);
    }

    @Test
    void confirmedStartCommitsMetadataAndReleasesOnlyThePool() {
        seedTree(810);
        bindVm(32, 812);
        when(gateway.startVm("vm-32")).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(rows(810)).hasSize(3);
            return new VirtualizationGateway.VirtualMachineRuntime(true, 0, null, "localhost:5901", "vm-32");
        });

        OperationE result = vmAppService.startVm(7, "vm-32").orElseThrow();

        assertThat(operations.findByIdAndActorId(result.id(), 7).orElseThrow().status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(jdbc.queryForObject("SELECT vm_vnc FROM vm WHERE vm_id=32 AND vm_status=1", String.class))
                .isEqualTo("localhost:5901");
        assertThat(jdbc.queryForObject("SELECT status FROM img WHERE img_id=812", Integer.class)).isZero();
        assertThat(rows(810)).containsExactly("810:READ", "811:READ", "812:WRITE");
        assertThat(admissions.tryAdmit(810, newOperation())).isTrue();
    }

    @Test
    void confirmedStartFailureReleasesPoolAndOccupancy() {
        seedTree(820);
        bindVm(33, 822);
        when(gateway.startVm("vm-33")).thenThrow(new ExternalOperationException(
                "START_VM", ExternalOperationException.Outcome.FAILED, "start rejected"));
        assertThatThrownBy(() -> vmAppService.startVm(7, "vm-33")).isInstanceOfSatisfying(
                VmAppService.StartFailure.class, error -> assertThat(operations.findById(error.operation().id())
                        .orElseThrow().status()).isEqualTo(OperationStatus.FAILED));
        assertThat(rows(820)).isEmpty();
        assertThat(admissions.tryAdmit(820, newOperation())).isTrue();
    }

    @Test
    void unknownRemoteOutcomeKeepsReservationsForReview() {
        seedTree(830);
        bindVm(34, 832);
        when(gateway.startVm("vm-34")).thenThrow(new ExternalOperationException(
                "START_VM", ExternalOperationException.Outcome.UNKNOWN, "response lost"));
        assertThatThrownBy(() -> vmAppService.startVm(7, "vm-34")).isInstanceOfSatisfying(
                VmAppService.StartFailure.class, error -> assertThat(operations.findById(error.operation().id())
                        .orElseThrow().status()).isEqualTo(OperationStatus.REQUIRES_REVIEW));
        assertThat(rows(830)).hasSize(3);
        assertThat(admissions.tryAdmit(830, newOperation())).isFalse();
    }

    @Test
    void failedMetadataWriteRollsBackSuccessAndKeepsReservationsForReview() {
        seedTree(840);
        bindVm(35, 842);
        when(gateway.startVm("vm-35")).thenReturn(new VirtualizationGateway.VirtualMachineRuntime(
                true, 0, null, "x".repeat(300), "vm-35"));
        assertThatThrownBy(() -> vmAppService.startVm(7, "vm-35")).isInstanceOfSatisfying(
                VmAppService.StartFailure.class, error -> assertThat(operations.findById(error.operation().id())
                        .orElseThrow().status()).isEqualTo(OperationStatus.REQUIRES_REVIEW));
        assertThat(jdbc.queryForObject("SELECT vm_status FROM vm WHERE vm_id=35", Integer.class)).isZero();
        assertThat(rows(840)).hasSize(3);
        assertThat(admissions.tryAdmit(840, newOperation())).isFalse();
    }

    private void bindVm(int vmId, int imageId) {
        jdbc.update("INSERT INTO vm (vm_id, vm_name, vm_status, vm_type) VALUES (?, ?, 0, 0)", vmId, "vm-" + vmId);
        jdbc.update("INSERT INTO img_vm (vm_id, img_id) VALUES (?, ?)", vmId, imageId);
    }

    private OperationE newOperation() {
        return OperationE.start(UUID.randomUUID().toString(), 7, OperationType.START_VM, LocalDateTime.now());
    }

    private boolean acquire(int imageId, int vmId, String holder) {
        return occupancy.tryAcquireForVmStart(imageId, vmId, holder, LocalDateTime.now());
    }

    private void seedTree(int root) {
        jdbc.update("INSERT INTO pool (id) VALUES (?)", root);
        for (int id = root; id <= root + 3; id++) {
            jdbc.update("INSERT INTO img (img_id, img_type, is_delete) VALUES (?, 0, 0)", id);
            jdbc.update("INSERT INTO img_pool (img_id, pool_id) VALUES (?, ?)", id, root);
        }
        jdbc.update("INSERT INTO img_child (img_id, img_child_id) VALUES (?, ?), (?, ?), (?, ?)",
                root, root + 1, root + 1, root + 2, root + 1, root + 3);
    }

    private List<String> rows(int root) {
        return jdbc.queryForList("""
                SELECT CONCAT(image_id, ':', mode) FROM image_occupancy
                WHERE image_id BETWEEN ? AND ? ORDER BY image_id, holder_id
                """, String.class, root, root + 3);
    }
}
