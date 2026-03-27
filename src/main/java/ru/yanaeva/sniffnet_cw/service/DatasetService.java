package ru.yanaeva.sniffnet_cw.service;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.yanaeva.sniffnet_cw.dto.dataset.DatasetResponse;
import ru.yanaeva.sniffnet_cw.entity.Dataset;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.repository.DatasetRepository;

@Service
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final MapperService mapperService;

    public DatasetService(DatasetRepository datasetRepository, MapperService mapperService) {
        this.datasetRepository = datasetRepository;
        this.mapperService = mapperService;
    }

    @Transactional(readOnly = true)
    public List<DatasetResponse> getAll() {
        return datasetRepository.findAll().stream().map(mapperService::toDatasetResponse).toList();
    }

    @Transactional(readOnly = true)
    public DatasetResponse getById(Long id) {
        return mapperService.toDatasetResponse(getEntity(id));
    }

    public Dataset getEntity(Long id) {
        return datasetRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Dataset not found"));
    }
}
