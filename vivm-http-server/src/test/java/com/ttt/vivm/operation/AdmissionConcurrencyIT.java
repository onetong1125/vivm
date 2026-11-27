package com.ttt.vivm.operation;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
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
