package com.ttt.vivm.image;

import com.ttt.vivm.cycle.domain.identity.model.entity.UserE;
import com.ttt.vivm.cycle.domain.identity.repo.UserRepository;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageActor;
import com.ttt.vivm.cycle.domain.resource.image.query.ImageCatalog;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageStorageKind;
import com.ttt.vivm.cycle.domain.resource.image.model.valueobj.ImageVisibility;
import com.ttt.vivm.model.Img;
import com.ttt.vivm.model.User;
import com.ttt.vivm.service.CustomUserDetailsService;
import com.ttt.vivm.service.ImageService;
import com.ttt.vivm.service.PoolService;
import com.ttt.vivm.service.VersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:vivm-r3-legacy;MODE=MySQL;NON_KEYWORDS=USER")
@ActiveProfiles("test")
class LegacyImageWriteQueryTest {

    @Autowired private UserRepository users;
    @Autowired private CustomUserDetailsService principals;
    @Autowired private ImageService images;
    @Autowired private PoolService pools;
    @Autowired private VersionService versions;
    @Autowired private ImageCatalog catalog;
    @Autowired @Qualifier("jpaTransactionManager") private PlatformTransactionManager jpaTransactions;

    @Test
    void legacyCreationIsImmediatelyVisibleWithoutUsingMigrationDefaults() throws Exception {
        UserE owner = users.save(UserE.registered("r3-writer", "encoded", "r3-writer@example.com"));
        User principal = (User) principals.loadUserByUsername(owner.userName());
        Img base = images.createBaseImage("r3-write-pool", "r3-write-base", 1, principal);
        images.createDeltaImage("r3-write-pool", "r3-write-delta", base.getBackImgPath(), principal);

        var mine = catalog.listVisible(new ImageActor(owner.id(), false), "r3-write-");
        assertThat(mine).hasSize(2);
        var delta = mine.stream().filter(image -> image.name().equals("r3-write-delta")).findFirst().orElseThrow();
        assertThat(delta.storageKind()).isEqualTo(ImageStorageKind.DELTA);
        assertThat(delta.visibility()).isEqualTo(ImageVisibility.PRIVATE);
        assertThat(delta.ownerId()).isEqualTo(owner.id());
        assertThat(delta.parentImageId()).isEqualTo(base.getImgId());
        assertThat(catalog.listVisible(new ImageActor(owner.id() + 1000, false), "r3-write-"))
                .extracting(image -> image.name()).containsExactly("r3-write-base");
    }

    @Test
    void legacyDeletionAndRestoreAreImmediatelyReflectedInQueries() throws Exception {
        UserE owner = users.save(UserE.registered("r3-restore", "encoded", "r3-restore@example.com"));
        User principal = (User) principals.loadUserByUsername(owner.userName());
        Img base = images.createBaseImage("r3-restore-pool", "r3-restore-base", 1, principal);
        images.createDeltaImage("r3-restore-pool", "r3-restore-delta", base.getBackImgPath(), principal);
        ImageActor actor = new ImageActor(owner.id(), false);
        int deltaId = catalog.listVisible(actor, "r3-restore-delta").get(0).id();
        TransactionTemplate transaction = new TransactionTemplate(jpaTransactions);
        transaction.executeWithoutResult(status ->
                versions.createNewBackup("r3-restore-pool", "r3-before-delete", "R3 query regression"));

        transaction.executeWithoutResult(status -> {
            try {
                images.deleteImageByImgId(deltaId);
            } catch (Exception exception) {
                throw new AssertionError("Legacy deletion failed", exception);
            }
        });
        assertThat(catalog.getVisible(actor, deltaId)).isEmpty();
        assertThat(catalog.listVisible(actor, "r3-restore-")).extracting(image -> image.name())
                .containsExactly("r3-restore-base");

        transaction.executeWithoutResult(status -> versions.restoreBackup(
                pools.findByName("r3-restore-pool").orElseThrow(),
                versions.searchVersionsByName("r3-before-delete").get(0)));
        assertThat(catalog.getVisible(actor, deltaId)).isPresent();
        assertThat(catalog.listVisible(actor, "r3-restore-")).extracting(image -> image.name())
                .containsExactlyInAnyOrder("r3-restore-base", "r3-restore-delta");
    }
}
