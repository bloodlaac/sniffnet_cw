package ru.yanaeva.sniffnet_cw.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import ru.yanaeva.sniffnet_cw.dto.auth.CurrentUserResponse;
import ru.yanaeva.sniffnet_cw.dto.classification.ClassificationResponse;
import ru.yanaeva.sniffnet_cw.dto.config.TrainingConfigResponse;
import ru.yanaeva.sniffnet_cw.dto.dataset.DatasetResponse;
import ru.yanaeva.sniffnet_cw.dto.experiment.ExperimentResponse;
import ru.yanaeva.sniffnet_cw.dto.file.UploadedImageResponse;
import ru.yanaeva.sniffnet_cw.dto.model.MetricResponse;
import ru.yanaeva.sniffnet_cw.dto.model.ModelResponse;
import ru.yanaeva.sniffnet_cw.dto.user.UserResponse;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.ClassificationRequest;
import ru.yanaeva.sniffnet_cw.entity.Dataset;
import ru.yanaeva.sniffnet_cw.entity.Experiment;
import ru.yanaeva.sniffnet_cw.entity.Metric;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;
import ru.yanaeva.sniffnet_cw.entity.TrainingConfig;
import ru.yanaeva.sniffnet_cw.entity.UploadedImage;

@Service
public class MapperService {

    private final ObjectMapper objectMapper;

    public MapperService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public UserResponse toUserResponse(AppUser user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().getCode().name(),
            user.getCreatedAt()
        );
    }

    public CurrentUserResponse toCurrentUserResponse(AppUser user) {
        return new CurrentUserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().getCode().name(),
            user.getCreatedAt()
        );
    }

    public DatasetResponse toDatasetResponse(Dataset dataset) {
        return new DatasetResponse(
            dataset.getId(),
            dataset.getName(),
            dataset.getClassesNum(),
            dataset.getSource()
        );
    }

    public TrainingConfigResponse toTrainingConfigResponse(TrainingConfig config) {
        return new TrainingConfigResponse(
            config.getId(),
            config.getEpochsNum(),
            config.getBatchSize(),
            config.getLearningRate(),
            config.getOptimizer(),
            config.getLossFunction(),
            config.getValidationSplit(),
            config.getLayersNum(),
            config.getNeuronsNum()
        );
    }

    public MetricResponse toMetricResponse(Metric metric) {
        if (metric == null) {
            return null;
        }
        return new MetricResponse(
            metric.getId(),
            metric.getTrainAccuracy(),
            metric.getTrainLoss(),
            metric.getValidationAccuracy(),
            metric.getValidationLoss(),
            metric.getDetailsJson()
        );
    }

    public ModelResponse toModelResponse(ModelEntity model, Metric metric) {
        return new ModelResponse(
            model.getId(),
            model.getName(),
            model.getDataset().getId(),
            model.getDataset().getName(),
            model.getConfig().getId(),
            model.getExperiment().getId(),
            model.getParamsNum(),
            model.getTrainingTimeSeconds(),
            model.getAvailableForInference(),
            model.getWeightsPath(),
            model.getCreatedAt(),
            toMetricResponse(metric)
        );
    }

    public ExperimentResponse toExperimentResponse(
        Experiment experiment,
        ModelEntity model,
        Metric metric
    ) {
        ModelResponse modelResponse = model == null ? null : toModelResponse(model, metric);
        return new ExperimentResponse(
            experiment.getId(),
            experiment.getStatus().name(),
            experiment.getStartTime(),
            experiment.getEndTime(),
            experiment.getReportPath(),
            experiment.getErrorMessage(),
            experiment.getDataset().getId(),
            experiment.getDataset().getName(),
            experiment.getUser().getId(),
            experiment.getUser().getUsername(),
            toTrainingConfigResponse(experiment.getConfig()),
            modelResponse,
            toMetricResponse(metric)
        );
    }

    public UploadedImageResponse toUploadedImageResponse(UploadedImage image) {
        return new UploadedImageResponse(
            image.getId(),
            image.getUser().getId(),
            image.getOriginalFilename(),
            image.getStoredFilename(),
            image.getContentType(),
            image.getSizeBytes(),
            image.getStoragePath(),
            image.getUploadedAt()
        );
    }

    public ClassificationResponse toClassificationResponse(ClassificationRequest request) {
        return new ClassificationResponse(
            request.getId(),
            request.getStatus().name(),
            request.getCreatedAt(),
            request.getCompletedAt(),
            request.getPredictedClass(),
            request.getConfidence(),
            request.getModel().getId(),
            request.getModel().getName(),
            request.getImage().getId(),
            request.getImage().getStoragePath(),
            readProbabilities(request.getProbabilitiesJson())
        );
    }

    public String writeProbabilities(Map<String, java.math.BigDecimal> probabilities) {
        try {
            return objectMapper.writeValueAsString(probabilities);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize probabilities", ex);
        }
    }

    private Map<String, java.math.BigDecimal> readProbabilities(String probabilitiesJson) {
        if (probabilitiesJson == null || probabilitiesJson.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            return objectMapper.readValue(
                probabilitiesJson,
                new TypeReference<LinkedHashMap<String, java.math.BigDecimal>>() { }
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize probabilities", ex);
        }
    }
}
