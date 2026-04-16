package ru.yanaeva.sniffnet_cw.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.Map;

public record PythonPredictResponse(
        @JsonProperty("class")
        String clazz,
        BigDecimal confidence,
        Map<String, BigDecimal> probs
) {
}
