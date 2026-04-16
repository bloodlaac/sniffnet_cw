package ru.yanaeva.sniffnet_cw.dto.model;

import java.math.BigDecimal;

public record MetricResponse(
        Long id,
        BigDecimal trainAccuracy,
        BigDecimal trainLoss,
        BigDecimal validationAccuracy,
        BigDecimal validationLoss,
        String detailsJson
) {
}
