package ru.yanaeva.sniffnet_cw.dto.model;

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
        MetricResponse metrics
) {
}
