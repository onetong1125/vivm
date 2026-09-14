package com.ttt.vivm.identity;

import com.ttt.vivm.cycle.domain.identity.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthContractTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;

    @Test
    void registrationRequiresCsrfAndCreatesLoginAccount() throws Exception {
        String request = """
                {
                  "userName": "login-user",
                  "email": "login-user@example.com",
                  "userPassword": "secret-123",
                  "confirmPassword": "secret-123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(request))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType("application/json")
                        .content(request))
                .andExpect(status().isCreated());

        assertThat(userRepository.findByUserName("login-user")).isPresent();

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .param("username", "login-user")
                        .param("password", "secret-123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void csrfEndpointRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andExpect(jsonPath("$.parameterName").isNotEmpty())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
