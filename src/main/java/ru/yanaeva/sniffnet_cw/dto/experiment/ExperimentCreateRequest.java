package ru.yanaeva.sniffnet_cw.dto.experiment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import ru.yanaeva.sniffnet_cw.dto.config.TrainingConfigRequest;

public record ExperimentCreateRequest(
        @NotNull
        Long
        datasetId,

        Long configId,
        
        @Valid
        TrainingConfigRequest config
) {
}
