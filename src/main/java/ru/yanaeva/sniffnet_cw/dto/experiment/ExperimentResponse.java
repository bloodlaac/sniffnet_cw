package ru.yanaeva.sniffnet_cw.dto.experiment;

import java.time.Instant;
import ru.yanaeva.sniffnet_cw.dto.config.TrainingConfigResponse;
import ru.yanaeva.sniffnet_cw.dto.model.MetricResponse;
import ru.yanaeva.sniffnet_cw.dto.model.ModelResponse;

public record ExperimentResponse(
        Long id,
        String status,
        Instant startTime,
        Instant endTime,
        String reportPath,
        Long datasetId,
        String datasetName,
        Long userId,
        String username,
        TrainingConfigResponse config,
        ModelResponse model,
        MetricResponse metrics
) {
}
