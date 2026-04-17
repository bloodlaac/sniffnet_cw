package ru.yanaeva.sniffnet_cw.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yanaeva.sniffnet_cw.dto.dataset.DatasetResponse;
import ru.yanaeva.sniffnet_cw.service.DatasetService;

@RestController
@RequestMapping("/api/datasets")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @GetMapping
    public List<DatasetResponse> getAll() {
        return datasetService.getAll();
    }

    @GetMapping("/{id}")
    public DatasetResponse getById(@PathVariable Long id) {
        return datasetService.getById(id);
    }
}
