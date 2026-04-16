package ru.yanaeva.sniffnet_cw.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yanaeva.sniffnet_cw.dto.model.MetricResponse;
import ru.yanaeva.sniffnet_cw.dto.model.ModelResponse;
import ru.yanaeva.sniffnet_cw.security.AppUserPrincipal;
import ru.yanaeva.sniffnet_cw.service.ModelService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public List<ModelResponse> getAll(
        @RequestParam(required = false) Long datasetId,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return modelService.getModels(
            datasetId,
            principal.getUser(),
            "ROLE_ADMIN".equals(principal.getRoleCode())
        );
    }

    @GetMapping("/{id}")
    public ModelResponse getById(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return modelService.getModel(
            id,
            principal.getUser(),
            "ROLE_ADMIN".equals(principal.getRoleCode())
        );
    }

    @GetMapping("/{id}/metrics")
    public MetricResponse getMetrics(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return modelService.getMetrics(
            id,
            principal.getUser(),
            "ROLE_ADMIN".equals(principal.getRoleCode())
        );
    }
}
