package ru.yanaeva.sniffnet_cw.integration;

public record PythonTrainingStartResponse(
        Long experiment_id,
        String status
) {
}
