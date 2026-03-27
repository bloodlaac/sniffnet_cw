package ru.yanaeva.sniffnet_cw.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yanaeva.sniffnet_cw.dto.dataset.DatasetResponse;
import ru.yanaeva.sniffnet_cw.dto.experiment.ExperimentCreateRequest;
import ru.yanaeva.sniffnet_cw.dto.experiment.ExperimentResponse;
import ru.yanaeva.sniffnet_cw.dto.config.TrainingConfigResponse;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.ClassificationRequest;
import ru.yanaeva.sniffnet_cw.entity.Experiment;
import ru.yanaeva.sniffnet_cw.entity.ExperimentStatus;
import ru.yanaeva.sniffnet_cw.entity.Metric;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;
import ru.yanaeva.sniffnet_cw.entity.TrainingConfig;
import ru.yanaeva.sniffnet_cw.exception.BadRequestException;
import ru.yanaeva.sniffnet_cw.exception.IntegrationException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.integration.TrainingAdapter;
import ru.yanaeva.sniffnet_cw.integration.TrainingLaunchResult;
import ru.yanaeva.sniffnet_cw.integration.TrainingStartRequest;
import ru.yanaeva.sniffnet_cw.repository.ExperimentRepository;
import ru.yanaeva.sniffnet_cw.repository.MetricRepository;
import ru.yanaeva.sniffnet_cw.repository.ModelRepository;
import ru.yanaeva.sniffnet_cw.repository.ClassificationRequestRepository;
import ru.yanaeva.sniffnet_cw.repository.TrainingConfigRepository;
import ru.yanaeva.sniffnet_cw.repository.UploadedImageRepository;

@Service
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final ModelRepository modelRepository;
    private final MetricRepository metricRepository;
    private final ClassificationRequestRepository classificationRequestRepository;
    private final TrainingConfigRepository trainingConfigRepository;
    private final UploadedImageRepository uploadedImageRepository;
    private final DatasetService datasetService;
    private final TrainingConfigService trainingConfigService;
    private final TrainingAdapter trainingAdapter;
    private final TrainingSyncService trainingSyncService;
    private final MapperService mapperService;

    public ExperimentService(
        ExperimentRepository experimentRepository,
        ModelRepository modelRepository,
        MetricRepository metricRepository,
        ClassificationRequestRepository classificationRequestRepository,
        TrainingConfigRepository trainingConfigRepository,
        UploadedImageRepository uploadedImageRepository,
        DatasetService datasetService,
        TrainingConfigService trainingConfigService,
        TrainingAdapter trainingAdapter,
        TrainingSyncService trainingSyncService,
        MapperService mapperService
    ) {
        this.experimentRepository = experimentRepository;
        this.modelRepository = modelRepository;
        this.metricRepository = metricRepository;
        this.classificationRequestRepository = classificationRequestRepository;
        this.trainingConfigRepository = trainingConfigRepository;
        this.uploadedImageRepository = uploadedImageRepository;
        this.datasetService = datasetService;
        this.trainingConfigService = trainingConfigService;
        this.trainingAdapter = trainingAdapter;
        this.trainingSyncService = trainingSyncService;
        this.mapperService = mapperService;
    }

    public ExperimentResponse createExperiment(ExperimentCreateRequest request, AppUser user) {
        if (user.getRole().getCode().name().equals("ROLE_ADMIN")) {
            throw new BadRequestException("Administrators cannot start training");
        }
        TrainingConfig config = resolveConfig(request);

        Experiment experiment = new Experiment();
        experiment.setDataset(datasetService.getEntity(request.datasetId()));
        experiment.setConfig(config);
        experiment.setUser(user);
        experiment.setStatus(ExperimentStatus.CREATED);
        experiment = experimentRepository.save(experiment);

        TrainingLaunchResult result;
        try {
            result = trainingAdapter.startTraining(new TrainingStartRequest(
                experiment.getId(),
                experiment.getDataset().getId(),
                experiment.getDataset().getName(),
                experiment.getDataset().getClassesNum(),
                config.getId(),
                config.getEpochsNum(),
                config.getBatchSize(),
                config.getLearningRate(),
                config.getOptimizer(),
                config.getLossFunction(),
                config.getValidationSplit(),
                config.getLayersNum(),
                config.getNeuronsNum(),
                user.getId()
            ));
        } catch (IntegrationException ex) {
            experiment.setStatus(ExperimentStatus.FAILED);
            experiment.setErrorMessage(trimErrorMessage(ex.getMessage()));
            experiment.setEndTime(LocalDateTime.now());
            experimentRepository.save(experiment);
            throw ex;
        }

        experiment.setExternalExperimentId(result.externalExperimentId());
        experiment.setStatus(mapLaunchStatus(result.status()));
        experiment.setReportPath(null);
        experiment.setErrorMessage(null);
        experiment = experimentRepository.save(experiment);

        DatasetResponse dataset = datasetService.getById(experiment.getDataset().getId());
        TrainingConfigResponse configResponse = mapperService.toTrainingConfigResponse(config);
        return new ExperimentResponse(
            experiment.getId(),
            experiment.getStatus().name(),
            experiment.getStartTime(),
            experiment.getEndTime(),
            experiment.getReportPath(),
            experiment.getErrorMessage(),
            dataset.id(),
            dataset.name(),
            user.getId(),
            user.getUsername(),
            configResponse,
            null,
            null
        );
    }

    @Transactional(readOnly = true)
    public List<ExperimentResponse> getExperiments(
        Long userId,
        String status,
        AppUser currentUser,
        boolean admin
    ) {
        trainingSyncService.syncPendingExperiments();
        Long effectiveUserId = admin ? userId : currentUser.getId();
        ExperimentStatus experimentStatus = status == null
        || status.isBlank() ? null : ExperimentStatus.valueOf(status);

        List<Experiment> result;
        if (effectiveUserId != null && experimentStatus != null) {
            result = experimentRepository.findByUserIdAndStatus(
                effectiveUserId,
                experimentStatus
            );
        } else if (effectiveUserId != null) {
            result = experimentRepository.findByUserId(effectiveUserId);
        } else if (experimentStatus != null) {
            result = experimentRepository.findByStatus(experimentStatus);
        } else {
            result = experimentRepository.findAll();
        }

        return result.stream()
            .sorted(Comparator.comparing(Experiment::getStartTime).reversed())
            .map(this::toExperimentResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ExperimentResponse getExperiment(Long id, AppUser currentUser, boolean admin) {
        Experiment experiment = getEntity(id);
        ensureAccess(experiment, currentUser, admin);
        experiment = trainingSyncService.syncExperiment(experiment);
        return toExperimentResponse(experiment);
    }

    @Transactional
    public ExperimentResponse updateStatus(Long id, ExperimentStatus status) {
        Experiment experiment = getEntity(id);
        experiment.setStatus(status);
        if (status == ExperimentStatus.COMPLETED || status == ExperimentStatus.FAILED) {
            experiment.setEndTime(LocalDateTime.now());
        }
        if (status != ExperimentStatus.FAILED) {
            experiment.setErrorMessage(null);
        }
        return toExperimentResponse(experimentRepository.save(experiment));
    }

    @Transactional
    public void deleteExperiment(Long id) {
        Experiment experiment = getEntity(id);
        Long configId = experiment.getConfig().getId();
        modelRepository.findByExperimentId(id).ifPresent(model -> {
            List<ClassificationRequest> classifications = classificationRequestRepository.findByModelId(model.getId());
            classificationRequestRepository.deleteByModelId(model.getId());
            for (ClassificationRequest classification : classifications) {
                Long imageId = classification.getImage().getId();
                if (!classificationRequestRepository.existsByImageId(imageId)) {
                    uploadedImageRepository.deleteById(imageId);
                }
            }
            modelRepository.delete(model);
        });
        metricRepository.deleteByConfigId(configId);
        experimentRepository.delete(experiment);
        if (experimentRepository.countByConfigId(configId) == 0) {
            trainingConfigRepository.deleteById(configId);
        }
    }

    @Transactional(readOnly = true)
    public String getReport(Long id, AppUser currentUser, boolean admin) {
        Experiment experiment = getEntity(id);
        ensureAccess(experiment, currentUser, admin);
        experiment = trainingSyncService.syncExperiment(experiment);
        return experiment.getReportPath();
    }

    public Experiment getEntity(Long id) {
        return experimentRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Experiment not found"));
    }

    private TrainingConfig resolveConfig(ExperimentCreateRequest request) {
        if (request.configId() != null) {
            return trainingConfigService.getEntity(request.configId());
        }
        if (request.config() != null) {
            return trainingConfigService.createEntity(request.config());
        }
        throw new BadRequestException("configId or config payload is required");
    }

    private ExperimentResponse toExperimentResponse(Experiment experiment) {
        ModelEntity model = modelRepository.findByExperimentId(experiment.getId())
        .orElse(null);
        Metric metric = metricRepository.findTopByDatasetIdAndConfigIdOrderByIdDesc(
            experiment.getDataset().getId(),
            experiment.getConfig().getId()
        ).orElse(null);
        return mapperService.toExperimentResponse(experiment, model, metric);
    }

    private void ensureAccess(Experiment experiment, AppUser currentUser, boolean admin) {
        if (!admin && !experiment.getUser().getId().equals(currentUser.getId())) {
            throw new NotFoundException("Experiment not found");
        }
    }

    private ExperimentStatus mapLaunchStatus(String status) {
        if (status == null) {
            return ExperimentStatus.CREATED;
        }
        return switch (status.toLowerCase()) {
            case "running", "started" -> ExperimentStatus.RUNNING;
            case "success", "completed" -> ExperimentStatus.COMPLETED;
            case "failed", "error" -> ExperimentStatus.FAILED;
            default -> ExperimentStatus.CREATED;
        };
    }

    private String trimErrorMessage(String message) {
        if (message == null || message.length() <= 255) {
            return message;
        }
        return message.substring(0, 252) + "...";
    }
}
