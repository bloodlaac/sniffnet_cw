package ru.yanaeva.sniffnet_cw.integration;

import java.math.BigDecimal;

public record TrainingResult(
        boolean success,
        String modelName,
        String externalReference,
        Integer paramsNum,
        Long trainingTimeSeconds,
        BigDecimal trainAccuracy,
        BigDecimal trainLoss,
        BigDecimal validationAccuracy,
        BigDecimal validationLoss,
        String reportPath
) {
}
