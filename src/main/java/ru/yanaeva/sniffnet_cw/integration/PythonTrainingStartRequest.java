package ru.yanaeva.sniffnet_cw.integration;

import java.math.BigDecimal;

public record PythonTrainingStartRequest(
        Long experiment_id,
        Long dataset_id,
        Long config_id,
        Long user_id,
        PythonTrainingConfig config
) {
    public record PythonTrainingConfig(
            Integer epochs_num,
            Integer batch_size,
            BigDecimal learning_rate,
            String optimizer,
            String loss_function,
            BigDecimal val_split,
            Integer layers_num,
            Integer neurons_num
    ) {
    }
}
