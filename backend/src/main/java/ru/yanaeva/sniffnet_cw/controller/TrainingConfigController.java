package ru.yanaeva.sniffnet_cw.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yanaeva.sniffnet_cw.dto.config.TrainingConfigRequest;
import ru.yanaeva.sniffnet_cw.dto.config.TrainingConfigResponse;
import ru.yanaeva.sniffnet_cw.service.TrainingConfigService;

@RestController
@RequestMapping("/api/configs")
public class TrainingConfigController {

    private final TrainingConfigService trainingConfigService;

    public TrainingConfigController(TrainingConfigService trainingConfigService) {
        this.trainingConfigService = trainingConfigService;
    }

    @GetMapping
    public List<TrainingConfigResponse> getAll() {
        return trainingConfigService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainingConfigResponse create(@Valid @RequestBody TrainingConfigRequest request) {
        return trainingConfigService.create(request);
    }

    @GetMapping("/{id}")
    public TrainingConfigResponse getById(@PathVariable Long id) {
        return trainingConfigService.getById(id);
    }

    @PutMapping("/{id}")
    public TrainingConfigResponse update(
        @PathVariable Long id,
        @Valid @RequestBody TrainingConfigRequest request
    ) {
        return trainingConfigService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        trainingConfigService.delete(id);
    }
}
