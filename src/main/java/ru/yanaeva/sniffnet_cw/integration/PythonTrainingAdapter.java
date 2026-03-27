package ru.yanaeva.sniffnet_cw.integration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.yanaeva.sniffnet_cw.config.AppProperties;
import ru.yanaeva.sniffnet_cw.exception.IntegrationException;

@Component
public class PythonTrainingAdapter implements TrainingAdapter, ClassificationAdapter {

    private final RestClient trainingClient;
    private final RestClient classificationClient;
    private final AppProperties appProperties;

    public PythonTrainingAdapter(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        this.appProperties = appProperties;
        this.trainingClient = restClientBuilder
            .baseUrl(appProperties.getIntegration().getTrainingServiceUrl())
            .build();
        this.classificationClient = restClientBuilder
            .baseUrl(appProperties.getIntegration().getClassificationServiceUrl())
            .build();
    }

    @Override
    public TrainingLaunchResult startTraining(TrainingStartRequest request) {
        try {
            PythonTrainingStartResponse response = trainingClient.post()
                .uri("/api/experiments/train")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PythonTrainingStartRequest(
                    request.experimentId(),
                    request.datasetId(),
                    request.configId(),
                    request.userId(),
                    new PythonTrainingStartRequest.PythonTrainingConfig(
                        request.epochsNum(),
                        request.batchSize(),
                        request.learningRate(),
                        request.optimizer(),
                        request.lossFunction(),
                        request.validationSplit(),
                        request.layersNum(),
                        request.neuronsNum()
                    )
                ))
                .retrieve()
                .body(PythonTrainingStartResponse.class);

            if (response == null || response.experiment_id() == null) {
                throw new IntegrationException("Python training service returned an empty response");
            }

            return new TrainingLaunchResult(response.experiment_id(), response.status());
        } catch (RestClientResponseException ex) {
            throw new IntegrationException(buildErrorMessage("start training", ex));
        } catch (RestClientException ex) {
            throw new IntegrationException("Failed to start training in Python service: " + ex.getMessage());
        }
    }

    @Override
    public TrainingResult fetchExperiment(Long externalExperimentId) {
        try {
            PythonExperimentResponse response = trainingClient.get()
                .uri("/api/experiments/{id}", externalExperimentId)
                .retrieve()
                .body(PythonExperimentResponse.class);

            if (response == null) {
                throw new IntegrationException("Python training service returned an empty experiment response");
            }

            boolean success = response.status() != null
                && ("success".equalsIgnoreCase(response.status())
                || "completed".equalsIgnoreCase(response.status()));
            String modelName = response.model_id() == null ? null : "model" + response.model_id();
            String weightsPath = response.model_id() == null ? null : "model" + response.model_id() + ".pth";
            Long trainingTimeSeconds = response.start_time() != null && response.end_time() != null
                ? Math.max(0L, java.time.Duration.between(response.start_time(), response.end_time()).getSeconds())
                : 0L;

            return new TrainingResult(
                response.experiment_id(),
                success,
                response.status(),
                response.error_message(),
                response.model_id(),
                modelName,
                weightsPath,
                response.params_num(),
                trainingTimeSeconds,
                response.train_accuracy(),
                response.train_loss(),
                response.validation_accuracy(),
                response.validation_loss(),
                success ? buildReportUrl(response.experiment_id()) : null
            );
        } catch (RestClientResponseException ex) {
            throw new IntegrationException(buildErrorMessage("fetch experiment state", ex));
        } catch (RestClientException ex) {
            throw new IntegrationException("Failed to fetch experiment state from Python service: " + ex.getMessage());
        }
    }

    @Override
    public ClassificationResult classify(ClassificationCommand command) {
        if (command.externalModelId() == null) {
            throw new IntegrationException("Python model id is missing for classification");
        }

        Path imagePath = Path.of(command.imagePath());
        if (!Files.exists(imagePath)) {
            throw new IntegrationException("Uploaded image file not found");
        }

        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("model_id", String.valueOf(command.externalModelId()));
            body.add("file", new FileSystemResource(new File(command.imagePath())));

            PythonPredictResponse response = classificationClient.post()
                .uri("/api/predict")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<PythonPredictResponse>() { });

            if (response == null) {
                throw new IntegrationException("Python classification service returned an empty response");
            }

            String predictedClass = response.clazz();
            if (predictedClass == null) {
                predictedClass = "UNKNOWN";
            }

            return new ClassificationResult(
                predictedClass,
                response.confidence(),
                response.probs(),
                "COMPLETED",
                "Python classification completed"
            );
        } catch (RestClientResponseException ex) {
            throw new IntegrationException(buildErrorMessage("classify image", ex));
        } catch (RestClientException ex) {
            throw new IntegrationException("Failed to classify image in Python service: " + ex.getMessage());
        }
    }

    private String buildReportUrl(Long externalExperimentId) {
        return appProperties.getIntegration().getTrainingServiceUrl()
            + "/api/experiments/" + externalExperimentId + "/report";
    }

    private String buildErrorMessage(String action, RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "Failed to " + action + " in Python service: HTTP " + ex.getStatusCode().value();
        }
        return "Failed to " + action + " in Python service: HTTP "
            + ex.getStatusCode().value()
            + " - "
            + body;
    }
}
