package ru.yanaeva.sniffnet_cw.integration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class MockClassificationAdapter implements ClassificationAdapter {

    @Override
    public ClassificationResult classify(ClassificationCommand command) {
        int seed = Math.abs((command.modelName() + command.imagePath())
        .getBytes(StandardCharsets.UTF_8)[0]);
        
        BigDecimal fresh = BigDecimal.valueOf(0.55 + (seed % 30) / 100.0)
        .setScale(4, RoundingMode.HALF_UP);
        
        if (fresh.compareTo(BigDecimal.valueOf(0.95)) > 0) {
            fresh = BigDecimal.valueOf(0.95);
        }
        BigDecimal spoiled = BigDecimal.ONE.subtract(fresh)
        .setScale(4, RoundingMode.HALF_UP);

        Map<String, BigDecimal> probabilities = new LinkedHashMap<>();
        probabilities.put("Fresh", fresh);
        probabilities.put("Spoiled", spoiled);

        String predicted = fresh.compareTo(spoiled) >= 0 ? "Fresh" : "Spoiled";
        BigDecimal confidence = probabilities.get(predicted);
        return new ClassificationResult(
            predicted,
            confidence,
            probabilities,
            "COMPLETED",
            "Mock classification completed"
        );
    }
}
