package ru.yanaeva.sniffnet_cw.integration;

public record ClassificationCommand(
        Long modelId,
        Long externalModelId,
        String modelName,
        String imagePath,
        String contentType
) {
}
