package com.ttt.vivm.identity;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;
import com.ttt.vivm.cycle.domain.identity.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional("jdbcTransactionManager")
class IdentityPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void registeredUserCanBeLoadedByUsername() {
        UserE registered = UserE.registered("alice", "encoded-password", "alice@example.com");

        UserE saved = userRepository.save(registered);

        assertThat(saved.id()).isPositive();
        assertThat(userRepository.findByUserName("alice"))
                .contains(saved);
    }

    @Test
    void usernameAndEmailRemainUnique() {
        userRepository.save(UserE.registered("unique-user", "encoded-password", "unique@example.com"));

        assertThatThrownBy(() -> userRepository.save(
                UserE.registered("unique-user", "another-password", "another@example.com")))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void usersCanBePagedAndFilteredByUsername() {
        userRepository.save(UserE.registered("page-alice", "encoded-password", "page-alice@example.com"));
        userRepository.save(UserE.registered("page-bob", "encoded-password", "page-bob@example.com"));

        var firstPage = userRepository.findPage(0, 1, "page-");

        assertThat(firstPage.total()).isEqualTo(2);
        assertThat(firstPage.users()).extracting(UserE::userName).containsExactly("page-alice");
    }
}
