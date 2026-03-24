package ru.yanaeva.sniffnet_cw;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SniffnetApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegisterAndReadCurrentUser() throws Exception {
        String payload = """
                {
                  "username": "tester",
                  "email": "tester@example.com",
                  "password": "secret123"
                }
                """;

        String token = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void shouldRejectProtectedEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/datasets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateExperimentAndClassifyImage() throws Exception {
        String loginPayload = """
                {
                  "username": "demo",
                  "password": "demo123"
                }
                """;

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        String experimentPayload = """
                {
                  "datasetId": 1,
                  "config": {
                    "epochsNum": 4,
                    "batchSize": 16,
                    "learningRate": 0.001,
                    "optimizer": "Adam",
                    "lossFunction": "CrossEntropy",
                    "validationSplit": 0.2,
                    "layersNum": 3,
                    "neuronsNum": 128
                  }
                }
                """;

        mockMvc.perform(post("/api/experiments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(experimentPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.model.id", notNullValue()));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banana.jpg",
                "image/jpeg",
                "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/classifications")
                        .file(file)
                        .param("modelId", "1")
                        .with(csrf().asHeader())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.predictedClass", notNullValue()))
                .andExpect(jsonPath("$.probabilities.Fresh", notNullValue()));
    }
}
