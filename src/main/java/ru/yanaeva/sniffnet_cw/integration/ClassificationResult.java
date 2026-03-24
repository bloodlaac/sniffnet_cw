package ru.yanaeva.sniffnet_cw.integration;

import java.math.BigDecimal;
import java.util.Map;

public record ClassificationResult(
        String predictedClass,
        BigDecimal confidence,
        Map<String, BigDecimal> classProbabilities,
        String serviceStatus,
        String message
) {
}
