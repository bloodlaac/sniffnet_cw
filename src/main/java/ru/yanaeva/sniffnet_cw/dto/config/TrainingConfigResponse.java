package ru.yanaeva.sniffnet_cw.dto.config;

import java.math.BigDecimal;

public record TrainingConfigResponse(
        Long id,
        Integer epochsNum,
        Integer batchSize,
        BigDecimal learningRate,
        String optimizer,
        String lossFunction,
        BigDecimal validationSplit,
        Integer layersNum,
        Integer neuronsNum
) {
}
