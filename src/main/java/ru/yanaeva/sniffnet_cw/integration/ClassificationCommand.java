package ru.yanaeva.sniffnet_cw.integration;

public record ClassificationCommand(
        Long modelId,
        String modelName,
        String imagePath,
        String contentType
) {
}
