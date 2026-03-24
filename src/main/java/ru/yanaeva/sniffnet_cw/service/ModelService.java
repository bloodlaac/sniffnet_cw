package ru.yanaeva.sniffnet_cw.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.yanaeva.sniffnet_cw.dto.common.PageResponse;
import ru.yanaeva.sniffnet_cw.dto.model.MetricResponse;
import ru.yanaeva.sniffnet_cw.dto.model.ModelResponse;
import ru.yanaeva.sniffnet_cw.entity.Metric;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.repository.MetricRepository;
import ru.yanaeva.sniffnet_cw.repository.ModelRepository;

@Service
public class ModelService {

    private final ModelRepository modelRepository;
    private final MetricRepository metricRepository;
    private final MapperService mapperService;

    public ModelService(
        ModelRepository modelRepository,
        MetricRepository metricRepository,
        MapperService mapperService
    ) {
        this.modelRepository = modelRepository;
        this.metricRepository = metricRepository;
        this.mapperService = mapperService;
    }

    public PageResponse<ModelResponse> getModels(Long datasetId, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<ModelEntity> modelPage = datasetId == null ? modelRepository.findAll(pageable)
        : modelRepository.findByDatasetId(datasetId, pageable);
        return PageResponse.from(modelPage
            .map(model -> mapperService.toModelResponse(
                model,
                metricRepository.findByModelId(model.getId()).orElse(null)
            )));
    }

    public ModelResponse getModel(Long id) {
        ModelEntity model = getEntity(id);
        return mapperService.toModelResponse(
            model,
            metricRepository.findByModelId(id).orElse(null)
        );
    }

    public MetricResponse getMetrics(Long modelId) {
        Metric metric = metricRepository.findByModelId(modelId)
            .orElseThrow(() -> new NotFoundException("Metrics not found for model"));
        return mapperService.toMetricResponse(metric);
    }

    public ModelEntity getEntity(Long id) {
        return modelRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Model not found"));
    }
}
