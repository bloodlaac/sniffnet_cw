package ru.yanaeva.sniffnet_cw.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yanaeva.sniffnet_cw.dto.common.PageResponse;
import ru.yanaeva.sniffnet_cw.dto.model.MetricResponse;
import ru.yanaeva.sniffnet_cw.dto.model.ModelResponse;
import ru.yanaeva.sniffnet_cw.service.ModelService;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public PageResponse<ModelResponse> getAll(
        @RequestParam(required = false) Long datasetId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sort
    ) {
        return modelService.getModels(datasetId, page, size, sort);
    }

    @GetMapping("/{id}")
    public ModelResponse getById(@PathVariable Long id) {
        return modelService.getModel(id);
    }

    @GetMapping("/{id}/metrics")
    public MetricResponse getMetrics(@PathVariable Long id) {
        return modelService.getMetrics(id);
    }
}
