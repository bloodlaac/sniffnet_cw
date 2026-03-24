package ru.yanaeva.sniffnet_cw.integration;

import java.math.BigDecimal;

public record TrainingStartRequest(
        Long experimentId,
        Long datasetId,
        String datasetName,
        Integer classesNum,
        Long configId,
        Integer epochsNum,
        Integer batchSize,
        BigDecimal learningRate,
        String optimizer,
        String lossFunction,
        BigDecimal validationSplit,
        Integer layersNum,
        Integer neuronsNum,
        Long userId
) {
}
