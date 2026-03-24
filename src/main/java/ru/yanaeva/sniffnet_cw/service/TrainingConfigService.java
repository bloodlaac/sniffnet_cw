package ru.yanaeva.sniffnet_cw.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yanaeva.sniffnet_cw.dto.config.TrainingConfigRequest;
import ru.yanaeva.sniffnet_cw.dto.config.TrainingConfigResponse;
import ru.yanaeva.sniffnet_cw.entity.TrainingConfig;
import ru.yanaeva.sniffnet_cw.exception.BadRequestException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.repository.ExperimentRepository;
import ru.yanaeva.sniffnet_cw.repository.TrainingConfigRepository;

@Service
public class TrainingConfigService {

    private final TrainingConfigRepository trainingConfigRepository;
    private final ExperimentRepository experimentRepository;
    private final MapperService mapperService;

    public TrainingConfigService(
        TrainingConfigRepository trainingConfigRepository,
        ExperimentRepository experimentRepository,
        MapperService mapperService
    ) {
        this.trainingConfigRepository = trainingConfigRepository;
        this.experimentRepository = experimentRepository;
        this.mapperService = mapperService;
    }

    @Transactional
    public TrainingConfigResponse create(TrainingConfigRequest request) {
        return mapperService.toTrainingConfigResponse(
            trainingConfigRepository.save(toEntity(new TrainingConfig(), request))
        );
    }

    public TrainingConfigResponse getById(Long id) {
        return mapperService.toTrainingConfigResponse(getEntity(id));
    }

    @Transactional
    public TrainingConfigResponse update(Long id, TrainingConfigRequest request) {
        TrainingConfig config = getEntity(id);
        return mapperService.toTrainingConfigResponse(
            trainingConfigRepository.save(toEntity(config, request))
        );
    }

    @Transactional
    public void delete(Long id) {
        boolean used = experimentRepository.findAll().stream().anyMatch(
            experiment -> experiment.getConfig().getId().equals(id)
        );
        if (used) {
            throw new BadRequestException("Training config is already used by an experiment");
        }
        trainingConfigRepository.delete(getEntity(id));
    }

    public TrainingConfig getEntity(Long id) {
        return trainingConfigRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Training config not found"));
    }

    public TrainingConfig createEntity(TrainingConfigRequest request) {
        return trainingConfigRepository.save(toEntity(new TrainingConfig(), request));
    }

    private TrainingConfig toEntity(TrainingConfig config, TrainingConfigRequest request) {
        config.setEpochsNum(request.epochsNum());
        config.setBatchSize(request.batchSize());
        config.setLearningRate(request.learningRate());
        config.setOptimizer(request.optimizer());
        config.setLossFunction(request.lossFunction());
        config.setValidationSplit(request.validationSplit());
        config.setLayersNum(request.layersNum());
        config.setNeuronsNum(request.neuronsNum());
        return config;
    }
}
