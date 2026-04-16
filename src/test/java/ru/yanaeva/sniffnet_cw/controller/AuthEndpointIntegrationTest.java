package ru.yanaeva.sniffnet_cw.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthEndpointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUserAndReadCurrentUserWithAccessToken() throws Exception {
        String payload = """
                {
                  "username": "integration_user",
                  "email": "integration_user@example.com",
                  "password": "secret123"
                }
                """;

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.username").value("integration_user"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = tokenFrom(response, "token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integration_user"))
                .andExpect(jsonPath("$.email").value("integration_user@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void shouldLoginExistingUserAndReturnJwtPair() throws Exception {
        String payload = """
                {
                  "username": "demo",
                  "password": "demo123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.email").value("demo@sniffnet.local"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void shouldRejectDuplicateRegistration() throws Exception {
        String payload = """
                {
                  "username": "duplicate_user",
                  "email": "duplicate_user@example.com",
                  "password": "secret123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {
        String payload = """
                {
                  "username": "demo",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void shouldRefreshJwtPairAndRejectRefreshTokenAsAccessToken() throws Exception {
        String loginPayload = """
                {
                  "username": "demo",
                  "password": "demo123"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = tokenFrom(loginResponse, "refreshToken");
        String refreshPayload = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        String refreshResponse = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.username").value("demo"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newAccessToken = tokenFrom(refreshResponse, "token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("demo"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        String payload = """
                {
                  "refreshToken": "not-a-jwt"
                }
                """;

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void shouldRejectCurrentUserRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private String tokenFrom(String response, String fieldName) throws Exception {
        JsonNode json = objectMapper.readTree(response);
        return json.get(fieldName).asText();
    }
}
