package ru.yanaeva.sniffnet_cw.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.yanaeva.sniffnet_cw.dto.classification.ClassificationResponse;
import ru.yanaeva.sniffnet_cw.security.AppUserPrincipal;
import ru.yanaeva.sniffnet_cw.service.ClassificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/classifications")
public class ClassificationController {

    private final ClassificationService classificationService;

    public ClassificationController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ClassificationResponse classify(
        @RequestParam Long modelId,
        @RequestParam(required = false) Long imageId,
        @RequestParam(required = false) MultipartFile file,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return classificationService.classify(modelId, imageId, file, principal.getUser());
    }

    @GetMapping
    public List<ClassificationResponse> getAll(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return classificationService.getClassifications(
            principal.getUser(),
            "ROLE_ADMIN".equals(principal.getRoleCode()),
            userId,
            from,
            to
        );
    }

    @GetMapping("/{id}")
    public ClassificationResponse getById(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return classificationService.getClassification(
            id,
            principal.getUser(),
            "ROLE_ADMIN".equals(principal.getRoleCode())
        );
    }
}
