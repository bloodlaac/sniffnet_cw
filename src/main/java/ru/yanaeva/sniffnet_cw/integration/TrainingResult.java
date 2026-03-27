package ru.yanaeva.sniffnet_cw.integration;

import java.math.BigDecimal;

public record TrainingResult(
        Long externalExperimentId,
        boolean success,
        String status,
        String errorMessage,
        Long externalModelId,
        String modelName,
        String weightsPath,
        Integer paramsNum,
        Long trainingTimeSeconds,
        BigDecimal trainAccuracy,
        BigDecimal trainLoss,
        BigDecimal validationAccuracy,
        BigDecimal validationLoss,
        String reportPath
) {
}
