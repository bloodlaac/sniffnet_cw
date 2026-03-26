package ru.yanaeva.sniffnet_cw.service;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yanaeva.sniffnet_cw.dto.common.PageResponse;
import ru.yanaeva.sniffnet_cw.dto.experiment.ExperimentCreateRequest;
import ru.yanaeva.sniffnet_cw.dto.experiment.ExperimentResponse;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.Experiment;
import ru.yanaeva.sniffnet_cw.entity.ExperimentStatus;
import ru.yanaeva.sniffnet_cw.entity.Metric;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;
import ru.yanaeva.sniffnet_cw.entity.TrainingConfig;
import ru.yanaeva.sniffnet_cw.exception.BadRequestException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.integration.TrainingAdapter;
import ru.yanaeva.sniffnet_cw.integration.TrainingResult;
import ru.yanaeva.sniffnet_cw.integration.TrainingStartRequest;
import ru.yanaeva.sniffnet_cw.repository.ExperimentRepository;
import ru.yanaeva.sniffnet_cw.repository.MetricRepository;
import ru.yanaeva.sniffnet_cw.repository.ModelRepository;

@Service
public class ExperimentService {

    private final ExperimentRepository experimentRepository;
    private final ModelRepository modelRepository;
    private final MetricRepository metricRepository;
    private final DatasetService datasetService;
    private final TrainingConfigService trainingConfigService;
    private final TrainingAdapter trainingAdapter;
    private final MapperService mapperService;

    public ExperimentService(
        ExperimentRepository experimentRepository,
        ModelRepository modelRepository,
        MetricRepository metricRepository,
        DatasetService datasetService,
        TrainingConfigService trainingConfigService,
        TrainingAdapter trainingAdapter,
        MapperService mapperService
    ) {
        this.experimentRepository = experimentRepository;
        this.modelRepository = modelRepository;
        this.metricRepository = metricRepository;
        this.datasetService = datasetService;
        this.trainingConfigService = trainingConfigService;
        this.trainingAdapter = trainingAdapter;
        this.mapperService = mapperService;
    }

    @Transactional
    public ExperimentResponse createExperiment(ExperimentCreateRequest request, AppUser user) {
        TrainingConfig config = resolveConfig(request);

        Experiment experiment = new Experiment();
        experiment.setDataset(datasetService.getEntity(request.datasetId()));
        experiment.setConfig(config);
        experiment.setUser(user);
        experiment.setStatus(ExperimentStatus.RUNNING);
        experiment = experimentRepository.save(experiment);

        TrainingResult result = trainingAdapter.startTraining(new TrainingStartRequest(
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

        return finalizeExperiment(experiment, result);
    }

    public PageResponse<ExperimentResponse> getExperiments(
        Long userId,
        String status,
        AppUser currentUser,
        boolean admin,
        int page,
        int size,
        String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        Long effectiveUserId = admin ? userId : currentUser.getId();
        ExperimentStatus experimentStatus = status == null
        || status.isBlank() ? null : ExperimentStatus.valueOf(status);

        Page<Experiment> result;
        if (effectiveUserId != null && experimentStatus != null) {
            result = experimentRepository.findByUserIdAndStatus(
                effectiveUserId,
                experimentStatus,
                pageable
            );
        } else if (effectiveUserId != null) {
            result = experimentRepository.findByUserId(effectiveUserId, pageable);
        } else if (experimentStatus != null) {
            result = experimentRepository.findByStatus(experimentStatus, pageable);
        } else {
            result = experimentRepository.findAll(pageable);
        }

        return PageResponse.from(result.map(this::toExperimentResponse));
    }

    public ExperimentResponse getExperiment(Long id, AppUser currentUser, boolean admin) {
        Experiment experiment = getEntity(id);
        ensureAccess(experiment, currentUser, admin);
        return toExperimentResponse(experiment);
    }

    @Transactional
    public ExperimentResponse updateStatus(Long id, ExperimentStatus status) {
        Experiment experiment = getEntity(id);
        experiment.setStatus(status);
        if (status == ExperimentStatus.COMPLETED || status == ExperimentStatus.FAILED) {
            experiment.setEndTime(LocalDateTime.now());
        }
        return toExperimentResponse(experimentRepository.save(experiment));
    }

    public String getReport(Long id, AppUser currentUser, boolean admin) {
        Experiment experiment = getEntity(id);
        ensureAccess(experiment, currentUser, admin);
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

    private ExperimentResponse finalizeExperiment(Experiment experiment, TrainingResult result) {
        experiment.setStatus(result.success() ? ExperimentStatus.COMPLETED : ExperimentStatus.FAILED);
        experiment.setEndTime(LocalDateTime.now());
        experiment.setReportPath(result.reportPath());
        experiment = experimentRepository.save(experiment);

        if (!result.success()) {
            return mapperService.toExperimentResponse(experiment, null, null);
        }

        ModelEntity model = new ModelEntity();
        model.setName(result.modelName());
        model.setDataset(experiment.getDataset());
        model.setConfig(experiment.getConfig());
        model.setExperiment(experiment);
        model.setParamsNum(result.paramsNum());
        model.setTrainingTimeSeconds(result.trainingTimeSeconds());
        model.setAvailableForInference(true);
        model.setWeightsPath(result.weightsPath());
        model = modelRepository.save(model);

        Metric metric = new Metric();
        metric.setDataset(experiment.getDataset());
        metric.setConfig(experiment.getConfig());
        metric.setTrainAccuracy(result.trainAccuracy());
        metric.setTrainLoss(result.trainLoss());
        metric.setValidationAccuracy(result.validationAccuracy());
        metric.setValidationLoss(result.validationLoss());
        metric.setDetailsJson("{\"source\":\"mock-training\"}");
        metric = metricRepository.save(metric);

        return mapperService.toExperimentResponse(experiment, model, metric);
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
}
