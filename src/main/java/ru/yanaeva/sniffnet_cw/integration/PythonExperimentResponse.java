package ru.yanaeva.sniffnet_cw.integration;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PythonExperimentResponse(
        Long experiment_id,
        Long dataset_id,
        Long config_id,
        Long user_id,
        Long model_id,
        OffsetDateTime start_time,
        OffsetDateTime end_time,
        String status,
        String error_message,
        Integer batch_size,
        Integer epochs_num,
        String loss_function,
        BigDecimal learning_rate,
        String optimizer,
        BigDecimal val_split,
        BigDecimal train_accuracy,
        BigDecimal train_loss,
        BigDecimal validation_accuracy,
        BigDecimal validation_loss,
        Integer params_num
) {
}
