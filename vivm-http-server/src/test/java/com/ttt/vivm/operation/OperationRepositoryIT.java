package com.ttt.vivm.operation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationType;
import com.ttt.vivm.cycle.domain.operation.repo.OperationRepository;
import com.ttt.vivm.support.MySqlIntegrationSupport;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class OperationRepositoryIT {

    private static final String DATABASE = "vivm_r4_operation_it_"
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

    @Autowired
    private OperationRepository operations;

    @Test
    void createdOperationCanBeLoadedById() {
        String id = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.of(2026, 11, 19, 15, 0);
        OperationE operation = OperationE.start(id, 7, OperationType.START_VM, startedAt);

        operations.create(operation);

        assertThat(operations.findById(id)).contains(operation);
        assertThat(operations.findByIdAndActorId(id, operation.actorId())).contains(operation);
        assertThat(operations.findByIdAndActorId(id, 8)).isEmpty();
    }

    @Test
    void validationFailurePersistsFinishTimeAndVersion() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 11, 19, 15, 0);
        OperationE validating = OperationE.start(UUID.randomUUID().toString(), 7, OperationType.START_VM, startedAt);
        operations.create(validating);

        OperationE failed = validating.failValidation(startedAt.plusSeconds(1));
        assertThat(operations.tryUpdate(validating, failed)).isTrue();
        assertThat(operations.findById(validating.id())).contains(failed);
    }

    @Test
    void staleValidationFailureCannotOverwriteRemoteExecution() {
        String id = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.of(2026, 11, 19, 15, 0);
        OperationE validating = OperationE.start(id, 7, OperationType.START_VM, startedAt);
        operations.create(validating);

        OperationE executing = validating.beginRemoteExecution();
        OperationE failed = validating.failValidation(startedAt.plusSeconds(1));

        assertThat(operations.tryUpdate(validating, executing)).isTrue();
        assertThat(operations.tryUpdate(validating, failed)).isFalse();
        assertThat(operations.findById(id)).contains(executing);
    }
}
