package ru.yanaeva.sniffnet_cw;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import ru.yanaeva.sniffnet_cw.integration.ClassificationAdapter;
import ru.yanaeva.sniffnet_cw.integration.ClassificationCommand;
import ru.yanaeva.sniffnet_cw.integration.ClassificationResult;
import ru.yanaeva.sniffnet_cw.integration.TrainingAdapter;
import ru.yanaeva.sniffnet_cw.integration.TrainingLaunchResult;
import ru.yanaeva.sniffnet_cw.integration.TrainingResult;

@SpringBootTest
@AutoConfigureMockMvc
class SniffnetApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestAdaptersConfiguration {

        @Bean
        @Primary
        TrainingAdapter trainingAdapter() {
            return new TrainingAdapter() {
                @Override
                public TrainingLaunchResult startTraining(ru.yanaeva.sniffnet_cw.integration.TrainingStartRequest request) {
                    return new TrainingLaunchResult(1L, "completed");
                }

                @Override
                public TrainingResult fetchExperiment(Long externalExperimentId) {
                    return new TrainingResult(
                        externalExperimentId,
                        true,
                        "success",
                        null,
                        1L,
                        "model1",
                        "model1.pth",
                        1000,
                        42L,
                        BigDecimal.valueOf(0.91),
                        BigDecimal.valueOf(0.12),
                        BigDecimal.valueOf(0.88),
                        BigDecimal.valueOf(0.17),
                        "http://localhost:8000/api/experiments/1/report"
                    );
                }
            };
        }

        @Bean
        @Primary
        ClassificationAdapter classificationAdapter() {
            return new ClassificationAdapter() {
                @Override
                public ClassificationResult classify(ClassificationCommand command) {
                    return new ClassificationResult(
                        "Fresh",
                        BigDecimal.valueOf(0.87),
                        Map.of("Fresh", BigDecimal.valueOf(0.87), "Spoiled", BigDecimal.valueOf(0.13)),
                        "COMPLETED",
                        "ok"
                    );
                }
            };
        }
    }

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
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
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
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.predictedClass", notNullValue()))
                .andExpect(jsonPath("$.probabilities.Fresh", notNullValue()));
    }
}
