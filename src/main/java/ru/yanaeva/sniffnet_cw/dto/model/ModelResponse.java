package ru.yanaeva.sniffnet_cw.dto.model;

import java.time.LocalDateTime;

public record ModelResponse(
        Long id,
        String name,
        Long datasetId,
        String datasetName,
        Long configId,
        Long experimentId,
        Integer paramsNum,
        Long trainingTimeSeconds,
        Boolean availableForInference,
        String weightsPath,
        LocalDateTime createdAt,
        MetricResponse metrics
) {
}
