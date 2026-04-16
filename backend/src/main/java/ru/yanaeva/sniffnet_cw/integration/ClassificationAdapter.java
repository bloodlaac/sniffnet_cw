package ru.yanaeva.sniffnet_cw.integration;

public interface ClassificationAdapter {
    ClassificationResult classify(ClassificationCommand command);
}
