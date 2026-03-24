package ru.yanaeva.sniffnet_cw.dto.file;

import java.time.Instant;

public record UploadedImageResponse(
        Long id,
        Long userId,
        String originalFilename,
        String storedFilename,
        String contentType,
        Long sizeBytes,
        String storagePath,
        Instant uploadedAt
) {
}
