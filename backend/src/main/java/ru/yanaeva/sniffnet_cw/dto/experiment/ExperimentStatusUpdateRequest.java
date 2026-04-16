package ru.yanaeva.sniffnet_cw.dto.experiment;

import jakarta.validation.constraints.NotBlank;

public record ExperimentStatusUpdateRequest(@NotBlank String status) {
}
