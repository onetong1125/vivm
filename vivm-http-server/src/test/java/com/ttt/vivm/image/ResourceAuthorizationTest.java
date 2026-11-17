package com.ttt.vivm.image;

import com.ttt.vivm.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional("jpaTransactionManager")
class ResourceAuthorizationTest {
    private static final int ALICE_ID = 90101;
    private static final int BOB_ID = 90102;
    private static final int ADMIN_ID = 90103;
    private static final int PUBLIC_ID = 90201;
    private static final int ALICE_IMAGE_ID = 90202;
    private static final int BOB_IMAGE_ID = 90203;
    private static final int DELETED_IMAGE_ID = 90204;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seedSiblingBranches() {
        jdbc.update("INSERT INTO `user` (user_id, user_name, user_pwd, role, oss_space, email, balance) VALUES (?, ?, ?, ?, 0, ?, 0)", ALICE_ID, "image-alice", "password", 0, "image-alice@example.com");
        jdbc.update("INSERT INTO `user` (user_id, user_name, user_pwd, role, oss_space, email, balance) VALUES (?, ?, ?, ?, 0, ?, 0)", BOB_ID, "image-bob", "password", 0, "image-bob@example.com");
        jdbc.update("INSERT INTO `user` (user_id, user_name, user_pwd, role, oss_space, email, balance) VALUES (?, ?, ?, ?, 0, ?, 0)", ADMIN_ID, "image-admin", "password", 1, "image-admin@example.com");
        jdbc.update("INSERT INTO pool (id, name, dir) VALUES (90301, 'shared-pool', '/tmp/shared-pool')");
        jdbc.update("INSERT INTO img (img_id, img_name, img_type, is_delete, status, img_size, create_date) VALUES (?, ?, 0, 0, 1, 10, CURRENT_TIMESTAMP)", PUBLIC_ID, "public-root");
        jdbc.update("INSERT INTO img (img_id, img_name, img_type, is_delete, status, img_size, create_date) VALUES (?, ?, 1, 0, 1, 10, CURRENT_TIMESTAMP)", ALICE_IMAGE_ID, "Alice-branch");
        jdbc.update("INSERT INTO img (img_id, img_name, img_type, is_delete, status, img_size, create_date) VALUES (?, ?, 1, 0, 1, 10, CURRENT_TIMESTAMP)", BOB_IMAGE_ID, "Bob-secret-branch");
        jdbc.update("INSERT INTO img (img_id, img_name, img_type, is_delete, status, img_size, create_date) VALUES (?, ?, 1, 1, 1, 10, CURRENT_TIMESTAMP)", DELETED_IMAGE_ID, "Alice-deleted");
        for (int id : new int[] {PUBLIC_ID, ALICE_IMAGE_ID, BOB_IMAGE_ID, DELETED_IMAGE_ID}) {
            jdbc.update("INSERT INTO img_pool (img_id, pool_id) VALUES (?, 90301)", id);
        }
        for (int id : new int[] {ALICE_IMAGE_ID, BOB_IMAGE_ID, DELETED_IMAGE_ID}) {
            jdbc.update("INSERT INTO img_child (img_child_id, img_id) VALUES (?, ?)", id, PUBLIC_ID);
        }
        jdbc.update("INSERT INTO user_img (img_id, user_id) VALUES (?, ?)", ALICE_IMAGE_ID, ALICE_ID);
        jdbc.update("INSERT INTO user_img (img_id, user_id) VALUES (?, ?)", BOB_IMAGE_ID, BOB_ID);
        jdbc.update("INSERT INTO user_img (img_id, user_id) VALUES (?, ?)", DELETED_IMAGE_ID, ALICE_ID);
    }

    @Test
    void ordinaryListAndSearchExcludeAnotherPrivateBranchAndDeletedImages() throws Exception {
        mockMvc.perform(get("/api/imgs/list").with(asUser(ALICE_ID, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].imgName", containsInAnyOrder("public-root", "Alice-branch")));

        mockMvc.perform(get("/api/imgs/list").param("search", "branch").with(asUser(ALICE_ID, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].imgName", containsInAnyOrder("Alice-branch")))
                .andExpect(jsonPath("$[0].storageKind").value("DELTA"))
                .andExpect(jsonPath("$[0].visibility").value("PRIVATE"))
                .andExpect(jsonPath("$[0].lifecycle").value("ACTIVE"));

        mockMvc.perform(get("/api/imgs/list").with(asUser(BOB_ID, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].imgName", containsInAnyOrder("public-root", "Bob-secret-branch")));
    }

    @Test
    void privateDetailIsHiddenWhilePublicDetailDoesNotRevealSiblingBranch() throws Exception {
        mockMvc.perform(get("/api/imgs/{id}", BOB_IMAGE_ID).with(asUser(ALICE_ID, false)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/imgs/{id}", ALICE_IMAGE_ID).with(asUser(BOB_ID, false)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/imgs/{id}", PUBLIC_ID).with(asUser(ALICE_ID, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imgName").value("public-root"))
                .andExpect(content().string(not(containsString("Bob-secret-branch"))))
                .andExpect(content().string(not(containsString(Integer.toString(BOB_IMAGE_ID)))));
    }

    @Test
    void administratorCanViewActivePrivateImagesButOrdinaryListStillExcludesDeleted() throws Exception {
        mockMvc.perform(get("/api/imgs/list").with(asUser(ADMIN_ID, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].imgName", containsInAnyOrder("public-root", "Alice-branch", "Bob-secret-branch")));

        mockMvc.perform(get("/api/imgs/{id}", BOB_IMAGE_ID).with(asUser(ADMIN_ID, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imgName").value("Bob-secret-branch"))
                .andExpect(jsonPath("$.storageKind").value("DELTA"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.lifecycle").value("ACTIVE"));
    }

    @Test
    void unauthenticatedRequestsCannotListOrReadDetails() throws Exception {
        mockMvc.perform(get("/api/imgs/list"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/imgs/{id}", PUBLIC_ID))
                .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor asUser(int id, boolean admin) {
        User principal = new User();
        principal.setUserId(id);
        principal.setUserName("image-user-" + id);
        principal.setUserPassword("password");
        principal.setRole(admin ? 1 : 0);
        return SecurityMockMvcRequestPostProcessors.user(principal);
    }
}
