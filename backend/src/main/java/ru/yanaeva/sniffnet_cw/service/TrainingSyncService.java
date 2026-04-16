package ru.yanaeva.sniffnet_cw.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yanaeva.sniffnet_cw.entity.Experiment;
import ru.yanaeva.sniffnet_cw.entity.ExperimentStatus;
import ru.yanaeva.sniffnet_cw.entity.Metric;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;
import ru.yanaeva.sniffnet_cw.exception.IntegrationException;
import ru.yanaeva.sniffnet_cw.integration.TrainingAdapter;
import ru.yanaeva.sniffnet_cw.integration.TrainingResult;
import ru.yanaeva.sniffnet_cw.repository.ExperimentRepository;
import ru.yanaeva.sniffnet_cw.repository.MetricRepository;
import ru.yanaeva.sniffnet_cw.repository.ModelRepository;

@Service
public class TrainingSyncService {

    private static final Logger log = LoggerFactory.getLogger(TrainingSyncService.class);
    private static final Duration UNSYNCED_GRACE_PERIOD = Duration.ofSeconds(30);

    private final TrainingAdapter trainingAdapter;
    private final ExperimentRepository experimentRepository;
    private final ModelRepository modelRepository;
    private final MetricRepository metricRepository;

    public TrainingSyncService(
        TrainingAdapter trainingAdapter,
        ExperimentRepository experimentRepository,
        ModelRepository modelRepository,
        MetricRepository metricRepository
    ) {
        this.trainingAdapter = trainingAdapter;
        this.experimentRepository = experimentRepository;
        this.modelRepository = modelRepository;
        this.metricRepository = metricRepository;
    }

    @Transactional
    public void syncPendingExperiments() {
        List<Experiment> experiments = experimentRepository.findByStatusIn(
            List.of(ExperimentStatus.CREATED, ExperimentStatus.RUNNING)
        );
        for (Experiment experiment : experiments) {
            if (experiment.getExternalExperimentId() == null) {
                failIfStaleWithoutExternalId(experiment);
                continue;
            }
            try {
                syncExperiment(experiment);
            } catch (IntegrationException ex) {
                if (isNotFoundInPython(ex)) {
                    markFailed(experiment, "Experiment not found in Python service");
                } else {
                    log.warn("Failed to sync experiment {}: {}", experiment.getId(), ex.getMessage());
                }
            } catch (Exception ex) {
                log.warn("Failed to sync experiment {}: {}", experiment.getId(), ex.getMessage());
            }
        }
    }

    @Transactional
    public Experiment syncExperiment(Experiment experiment) {
        if (experiment.getExternalExperimentId() == null) {
            return experiment;
        }

        TrainingResult result = trainingAdapter.fetchExperiment(experiment.getExternalExperimentId());
        ExperimentStatus mappedStatus = mapStatus(result.status());
        experiment.setStatus(mappedStatus);
        experiment.setErrorMessage(result.errorMessage());

        if (mappedStatus == ExperimentStatus.COMPLETED || mappedStatus == ExperimentStatus.FAILED) {
            if (experiment.getEndTime() == null) {
                experiment.setEndTime(LocalDateTime.now());
            }
        } else {
            experiment.setEndTime(null);
        }

        experiment.setReportPath(result.reportPath());
        experiment = experimentRepository.save(experiment);

        if (result.success() && result.externalModelId() != null) {
            upsertModel(experiment, result);
        }
        if (result.trainAccuracy() != null && result.trainLoss() != null) {
            upsertMetric(experiment, result);
        }

        return experiment;
    }

    private void upsertModel(Experiment experiment, TrainingResult result) {
        ModelEntity model = modelRepository.findByExperimentId(experiment.getId()).orElseGet(ModelEntity::new);
        model.setExperiment(experiment);
        model.setDataset(experiment.getDataset());
        model.setConfig(experiment.getConfig());
        model.setExternalModelId(result.externalModelId());
        model.setName(result.modelName() != null ? result.modelName() : "model" + result.externalModelId());
        model.setParamsNum(result.paramsNum());
        model.setTrainingTimeSeconds(result.trainingTimeSeconds() != null ? result.trainingTimeSeconds() : 0L);
        model.setAvailableForInference(Boolean.TRUE);
        model.setWeightsPath(result.weightsPath());
        modelRepository.save(model);
    }

    private void upsertMetric(Experiment experiment, TrainingResult result) {
        Metric metric = metricRepository.findTopByDatasetIdAndConfigIdOrderByIdDesc(
            experiment.getDataset().getId(),
            experiment.getConfig().getId()
        ).orElseGet(Metric::new);

        metric.setDataset(experiment.getDataset());
        metric.setConfig(experiment.getConfig());
        metric.setTrainAccuracy(result.trainAccuracy());
        metric.setTrainLoss(result.trainLoss());
        metric.setValidationAccuracy(result.validationAccuracy());
        metric.setValidationLoss(result.validationLoss());
        metric.setDetailsJson(buildDetailsJson(result));
        metricRepository.save(metric);
    }

    private String buildDetailsJson(TrainingResult result) {
        return "{\"source\":\"python-training\",\"externalExperimentId\":"
            + result.externalExperimentId()
            + ",\"externalModelId\":"
            + (result.externalModelId() == null ? "null" : result.externalModelId())
            + ",\"status\":\""
            + escapeJson(result.status())
            + "\",\"errorMessage\":"
            + (result.errorMessage() == null ? "null" : "\"" + escapeJson(result.errorMessage()) + "\"")
            + "}";
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ExperimentStatus mapStatus(String externalStatus) {
        if (externalStatus == null) {
            return ExperimentStatus.CREATED;
        }

        return switch (externalStatus.toLowerCase()) {
            case "queued", "created" -> ExperimentStatus.CREATED;
            case "running", "started" -> ExperimentStatus.RUNNING;
            case "success", "completed" -> ExperimentStatus.COMPLETED;
            case "failed", "error" -> ExperimentStatus.FAILED;
            default -> ExperimentStatus.CREATED;
        };
    }

    private void failIfStaleWithoutExternalId(Experiment experiment) {
        if (experiment.getStartTime() == null) {
            return;
        }
        if (Duration.between(experiment.getStartTime(), LocalDateTime.now()).compareTo(UNSYNCED_GRACE_PERIOD) < 0) {
            return;
        }
        markFailed(experiment, "Training was not started in Python service");
    }

    private boolean isNotFoundInPython(IntegrationException ex) {
        return ex.getMessage() != null && ex.getMessage().contains("HTTP 404");
    }

    private void markFailed(Experiment experiment, String message) {
        experiment.setStatus(ExperimentStatus.FAILED);
        experiment.setErrorMessage(trimErrorMessage(message));
        if (experiment.getEndTime() == null) {
            experiment.setEndTime(LocalDateTime.now());
        }
        experimentRepository.save(experiment);
    }

    private String trimErrorMessage(String message) {
        if (message == null || message.length() <= 255) {
            return message;
        }
        return message.substring(0, 252) + "...";
    }
}
