package ru.yanaeva.sniffnet_cw.dto.classification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ClassificationResponse(
        Long id,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String predictedClass,
        BigDecimal confidence,
        Long modelId,
        String modelName,
        Long imageId,
        String imagePath,
        Map<String, BigDecimal> probabilities
) {
}
