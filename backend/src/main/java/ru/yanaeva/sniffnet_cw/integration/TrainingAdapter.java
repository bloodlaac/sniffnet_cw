package ru.yanaeva.sniffnet_cw.integration;

public interface TrainingAdapter {
    TrainingLaunchResult startTraining(TrainingStartRequest request);

    TrainingResult fetchExperiment(Long externalExperimentId);
}
