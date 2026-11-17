package com.ttt.vivm.image;

import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageActor;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageLifecycle;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageCatalog;
import com.ttt.vivm.cycle.domain.resource.image.repo.ImageRepository;
import com.ttt.vivm.support.MySqlIntegrationSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ImageQueryIT {

    private static final String DATABASE = "vivm_r3_image_it_"
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
    private JdbcTemplate jdbc;
    @Autowired
    private ImageCatalog catalog;
    @Autowired
    private ImageRepository repository;

    @BeforeEach
    void clearImages() {
        jdbc.update("DELETE FROM user_img");
        jdbc.update("DELETE FROM img_child");
        jdbc.update("DELETE FROM img_pool");
        jdbc.update("DELETE FROM img");
        jdbc.update("DELETE FROM pool");
        jdbc.update("DELETE FROM `user`");
    }

    @Test
    void publicRootIsVisibleFromLegacyFieldsWhenBackfilledColumnsAreStale() {
        jdbc.update("INSERT INTO pool (id, name) VALUES (1, 'public-pool')");
        jdbc.update("INSERT INTO img (img_id, img_name, img_type, is_delete, "
                        + "storage_kind, visibility, lifecycle, pool_id, parent_image_id) "
                        + "VALUES (1, 'public-root', 0, 0, 'DELTA', 'PRIVATE', 'DELETED', 999, 999)");
        jdbc.update("INSERT INTO img_pool (img_id, pool_id) VALUES (1, 1)");

        var result = catalog.listVisible(new ImageActor(123, false), null);

        assertThat(result).singleElement().satisfies(image -> {
            assertThat(image.name()).isEqualTo("public-root");
            assertThat(image.storageKind()).isEqualTo(ImageStorageKind.BASE);
            assertThat(image.visibility()).isEqualTo(ImageVisibility.PUBLIC);
            assertThat(image.lifecycle()).isEqualTo(ImageLifecycle.ACTIVE);
            assertThat(image.poolId()).isEqualTo(1);
            assertThat(image.parentImageId()).isNull();
            assertThat(image.ownerId()).isNull();
        });
    }

    @Test
    void privateSiblingsAreIsolatedInListSearchAndDetailsWhileAdminCanViewBoth() {
        user(1, "alice");
        user(2, "bob");
        jdbc.update("INSERT INTO pool (id, name) VALUES (1, 'shared-pool')");
        image(1, "public-root", 0, 0);
        image(2, "alice-branch", 1, 0);
        image(3, "bob-branch", 1, 0);
        jdbc.update("INSERT INTO img_pool (img_id, pool_id) VALUES (1, 1), (2, 1), (3, 1)");
        jdbc.update("INSERT INTO img_child (img_child_id, img_id) VALUES (2, 1), (3, 1)");
        jdbc.update("INSERT INTO user_img (img_id, user_id) VALUES (2, 1), (3, 2)");

        ImageActor alice = new ImageActor(1, false);
        ImageActor bob = new ImageActor(2, false);
        ImageActor admin = new ImageActor(9, true);
        assertThat(catalog.listVisible(alice, null)).extracting(image -> image.name())
                .containsExactly("public-root", "alice-branch");
        assertThat(catalog.listVisible(bob, null)).extracting(image -> image.name())
                .containsExactly("public-root", "bob-branch");
        assertThat(catalog.listVisible(admin, null)).extracting(image -> image.name())
                .containsExactly("public-root", "alice-branch", "bob-branch");
        assertThat(catalog.listVisible(alice, "bob-branch")).isEmpty();
        assertThat(catalog.getVisible(alice, 3)).isEmpty();
        assertThat(catalog.getVisible(bob, 2)).isEmpty();
        assertThat(catalog.getVisible(admin, 3)).hasValueSatisfying(image -> {
            assertThat(image.ownerId()).isEqualTo(2);
            assertThat(image.storageKind()).isEqualTo(ImageStorageKind.DELTA);
            assertThat(image.parentImageId()).isEqualTo(1);
        });
        assertThat(catalog.getVisible(alice, 1)).hasValueSatisfying(image -> {
            assertThat(image.name()).isEqualTo("public-root");
            assertThat(image.ownerId()).isNull();
        });
    }

    @Test
    void deletedAncestorSupportsAnActiveDescendantWithoutAppearingInOrdinaryList() {
        user(1, "alice");
        jdbc.update("INSERT INTO pool (id, name) VALUES (1, 'shared-pool')");
        image(1, "public-root", 0, 0);
        image(2, "old-parent", 1, 1);
        image(3, "current-child", 1, 0);
        jdbc.update("INSERT INTO img_pool (img_id, pool_id) VALUES (1, 1), (2, 1), (3, 1)");
        jdbc.update("INSERT INTO img_child (img_child_id, img_id) VALUES (2, 1), (3, 2)");
        jdbc.update("INSERT INTO user_img (img_id, user_id) VALUES (2, 1), (3, 1)");

        ImageActor alice = new ImageActor(1, false);
        assertThat(catalog.listVisible(alice, null)).extracting(image -> image.name())
                .containsExactly("public-root", "current-child");
        assertThat(catalog.getVisible(alice, 2)).isEmpty();
        assertThat(catalog.getVisible(alice, 3)).isPresent();
        assertThat(repository.findAncestorsIncludingDeleted(3)).extracting(image -> image.name())
                .containsExactly("old-parent", "public-root");
        assertThat(repository.findAncestorsIncludingDeleted(3).get(0).lifecycle())
                .isEqualTo(ImageLifecycle.DELETED);
    }

    @Test
    void privateImageWithoutExactlyOneOwnerFailsExplicitly() {
        user(1, "alice");
        user(2, "bob");
        jdbc.update("INSERT INTO pool (id, name) VALUES (1, 'shared-pool')");
        image(1, "ownerless", 1, 0);
        jdbc.update("INSERT INTO img_pool (img_id, pool_id) VALUES (1, 1)");

        assertThatThrownBy(() -> catalog.getVisible(new ImageActor(1, true), 1))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalStateException.class).hasMessageContaining("0 owners");
        assertThat(catalog.listVisible(new ImageActor(1, false), null)).isEmpty();
        assertThatThrownBy(() -> catalog.listVisible(new ImageActor(1, true), null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalStateException.class).hasMessageContaining("0 owners");

        jdbc.update("INSERT INTO user_img (img_id, user_id) VALUES (1, 1), (1, 2)");
        assertThatThrownBy(() -> catalog.getVisible(new ImageActor(1, false), 1))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalStateException.class).hasMessageContaining("2 owners");
        assertThat(catalog.getVisible(new ImageActor(3, false), 1)).isEmpty();
        assertThat(catalog.listVisible(new ImageActor(3, false), null)).isEmpty();
    }

    @Test
    void publicLinkedUserIsOnlyAUniqueCreatorHintAndNeverAnOwner() {
        user(1, "alice");
        user(2, "bob");
        jdbc.update("INSERT INTO pool (id, name) VALUES (1, 'shared-pool')");
        image(1, "public-root", 0, 0);
        jdbc.update("INSERT INTO img_pool (img_id, pool_id) VALUES (1, 1)");
        jdbc.update("INSERT INTO user_img (img_id, user_id) VALUES (1, 1)");

        assertThat(catalog.getVisible(new ImageActor(2, false), 1)).hasValueSatisfying(image -> {
            assertThat(image.ownerId()).isNull();
            assertThat(image.createdBy()).isEqualTo(1);
        });

        jdbc.update("INSERT INTO user_img (img_id, user_id) VALUES (1, 2)");
        assertThat(catalog.getVisible(new ImageActor(2, false), 1)).hasValueSatisfying(image -> {
            assertThat(image.ownerId()).isNull();
            assertThat(image.createdBy()).isNull();
        });
    }

    private void user(int id, String name) {
        jdbc.update("INSERT INTO `user` "
                        + "(user_id, user_name, user_pwd, role, oss_space, email, balance) "
                        + "VALUES (?, ?, 'encoded', 0, 0, ?, 0)",
                id, name, name + "@example.com");
    }

    private void image(int id, String name, int type, int deleted) {
        jdbc.update("INSERT INTO img "
                        + "(img_id, img_name, img_type, is_delete, img_size, status, "
                        + "storage_kind, visibility, lifecycle, pool_id, parent_image_id) "
                        + "VALUES (?, ?, ?, ?, 10, 0, 'BASE', 'PUBLIC', 'ACTIVE', 999, 999)",
                id, name, type, deleted);
    }
}
