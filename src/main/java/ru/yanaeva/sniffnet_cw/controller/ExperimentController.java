package ru.yanaeva.sniffnet_cw.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yanaeva.sniffnet_cw.dto.common.PageResponse;
import ru.yanaeva.sniffnet_cw.dto.experiment.ExperimentCreateRequest;
import ru.yanaeva.sniffnet_cw.dto.experiment.ExperimentResponse;
import ru.yanaeva.sniffnet_cw.dto.experiment.ExperimentStatusUpdateRequest;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.ExperimentStatus;
import ru.yanaeva.sniffnet_cw.security.AppUserPrincipal;
import ru.yanaeva.sniffnet_cw.service.ExperimentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExperimentResponse create(
        @Valid
        @RequestBody
        ExperimentCreateRequest request,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return experimentService.createExperiment(request, principal.getUser());
    }

    @GetMapping
    public PageResponse<ExperimentResponse> getAll(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "startTime") String sort,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return experimentService.getExperiments(
            userId,
            status,
            principal.getUser(),
            isAdmin(principal),
            page,
            size,
            sort
        );
    }

    @GetMapping("/{id}")
    public ExperimentResponse getById(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return experimentService.getExperiment(id, principal.getUser(), isAdmin(principal));
    }

    @GetMapping("/{id}/report")
    public String getReport(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return experimentService.getReport(id, principal.getUser(), isAdmin(principal));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ExperimentResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody ExperimentStatusUpdateRequest request
    ) {
        return experimentService.updateStatus(id, ExperimentStatus.valueOf(request.status()));
    }

    private boolean isAdmin(AppUserPrincipal principal) {
        return "ROLE_ADMIN".equals(principal.getRoleCode());
    }
}
