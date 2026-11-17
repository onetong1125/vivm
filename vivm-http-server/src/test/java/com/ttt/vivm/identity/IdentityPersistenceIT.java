package com.ttt.vivm.identity;

import com.ttt.vivm.support.MySqlIntegrationSupport;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;
import com.ttt.vivm.cycle.domain.identity.repo.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class IdentityPersistenceIT {

    private static final String DATABASE = "vivm_r2_identity_it_"
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
    private UserRepository userRepository;

    @Test
    void repositoryPersistsAndPagesAccountsOnMySql() {
        UserE alice = userRepository.save(UserE.registered(
                "mysql-alice", "encoded-password", "mysql-alice@example.com"));
        userRepository.save(UserE.registered(
                "mysql-bob", "encoded-password", "mysql-bob@example.com"));

        assertThat(userRepository.findByUserName("mysql-alice")).contains(alice);
        assertThat(userRepository.findPage(0, 1, "mysql-").total()).isEqualTo(2);
        assertThatThrownBy(() -> userRepository.save(UserE.registered(
                "mysql-alice", "another-password", "mysql-another@example.com")))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }
}
