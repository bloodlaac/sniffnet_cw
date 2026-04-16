package ru.yanaeva.sniffnet_cw.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.yanaeva.sniffnet_cw.dto.model.MetricResponse;
import ru.yanaeva.sniffnet_cw.dto.model.ModelResponse;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.Metric;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.repository.MetricRepository;
import ru.yanaeva.sniffnet_cw.repository.ModelRepository;

@Service
public class ModelService {

    private final ModelRepository modelRepository;
    private final MetricRepository metricRepository;
    private final TrainingSyncService trainingSyncService;
    private final MapperService mapperService;

    public ModelService(
        ModelRepository modelRepository,
        MetricRepository metricRepository,
        TrainingSyncService trainingSyncService,
        MapperService mapperService
    ) {
        this.modelRepository = modelRepository;
        this.metricRepository = metricRepository;
        this.trainingSyncService = trainingSyncService;
        this.mapperService = mapperService;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<ModelResponse> getModels(
        Long datasetId,
        AppUser currentUser,
        boolean admin
    ) {
        trainingSyncService.syncPendingExperiments();
        List<ModelEntity> models;
        if (admin) {
            models = datasetId == null ? modelRepository.findAll()
                : modelRepository.findByDatasetId(datasetId);
        } else {
            models = datasetId == null
                ? modelRepository.findByExperimentUserId(currentUser.getId())
                : modelRepository.findByDatasetIdAndExperimentUserId(
                    datasetId,
                    currentUser.getId()
                );
        }
        return models.stream()
            .sorted(Comparator.comparing(ModelEntity::getCreatedAt).reversed())
            .map(model -> mapperService.toModelResponse(
                model,
                metricRepository.findTopByDatasetIdAndConfigIdOrderByIdDesc(
                    model.getDataset().getId(),
                    model.getConfig().getId()
                ).orElse(null)
            ))
            .toList();
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ModelResponse getModel(Long id, AppUser currentUser, boolean admin) {
        ModelEntity model = getAccessibleEntity(id, currentUser, admin);
        trainingSyncService.syncExperiment(model.getExperiment());
        return mapperService.toModelResponse(
            model,
            metricRepository.findTopByDatasetIdAndConfigIdOrderByIdDesc(
                model.getDataset().getId(),
                model.getConfig().getId()
            ).orElse(null)
        );
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public MetricResponse getMetrics(Long modelId, AppUser currentUser, boolean admin) {
        ModelEntity model = getAccessibleEntity(modelId, currentUser, admin);
        trainingSyncService.syncExperiment(model.getExperiment());
        Metric metric = metricRepository.findTopByDatasetIdAndConfigIdOrderByIdDesc(
                model.getDataset().getId(),
                model.getConfig().getId()
            )
            .orElseThrow(() -> new NotFoundException("Metrics not found for model"));
        return mapperService.toMetricResponse(metric);
    }

    public ModelEntity getEntity(Long id) {
        return modelRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Model not found"));
    }

    public ModelEntity getAccessibleEntity(Long id, AppUser currentUser, boolean admin) {
        ModelEntity model = getEntity(id);
        if (!admin && !model.getExperiment().getUser().getId().equals(currentUser.getId())) {
            throw new NotFoundException("Model not found");
        }
        return model;
    }
}
