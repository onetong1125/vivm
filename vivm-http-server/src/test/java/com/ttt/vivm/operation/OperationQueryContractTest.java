package com.ttt.vivm.operation;

import com.ttt.vivm.cycle.domain.operation.model.entity.OperationE;
import com.ttt.vivm.cycle.domain.operation.model.valueobj.OperationType;
import com.ttt.vivm.cycle.domain.operation.repo.OperationRepository;
import com.ttt.vivm.model.User;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional("jdbcTransactionManager")
class OperationQueryContractTest {
    private static final String PATH = "/api/operations/{id}";
    private static final int OWNER = 17;

    @Autowired MockMvc mvc;
    @Autowired OperationRepository operations;
    private final String id = UUID.randomUUID().toString();

    @BeforeEach
    void createOperation() {
        OperationE started = OperationE.start(id, OWNER, OperationType.START_VM, LocalDateTime.of(2026, 11, 19, 15, 0));
        operations.create(started);
        operations.tryUpdate(started, started.beginRemoteExecution());
    }

    @Test
    void ownerCanReadCurrentStepAndVersion() throws Exception {
        mvc.perform(get(PATH, id).with(user(principal(OWNER, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId").value(id))
                .andExpect(jsonPath("$.operationType").value("START_VM"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.step").value("REMOTE_EXECUTING"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void anotherActorCannotDiscoverOperationEvenWithAdminRole() throws Exception {
        for (int role : new int[] {0, 1}) {
            mvc.perform(get(PATH, id).param("actorId", String.valueOf(OWNER)).with(user(principal(18, role))))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(""));
        }
    }

    @Test
    void unknownIdReturnsNotFound() throws Exception {
        mvc.perform(get(PATH, UUID.randomUUID()).with(user(principal(OWNER, 0))))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void anonymousRequestRequiresAuthentication() throws Exception {
        mvc.perform(get(PATH, id)).andExpect(status().isUnauthorized());
    }

    private User principal(int id, int role) {
        User principal = new User();
        principal.setUserId(id);
        principal.setUserName("actor-" + id);
        principal.setRole(role);
        return principal;
    }
}
