package ru.yanaeva.sniffnet_cw.dto.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TrainingConfigRequest(
        @NotNull
        @Positive
        Integer epochsNum,

        @NotNull
        @Positive
        Integer batchSize,

        @NotNull
        @DecimalMin("0.000001")
        BigDecimal learningRate,

        @NotBlank
        String optimizer,

        @NotBlank
        String lossFunction,

        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("0.99")
        BigDecimal validationSplit,

        @Positive
        Integer layersNum,

        @Positive
        Integer neuronsNum
) {
}
