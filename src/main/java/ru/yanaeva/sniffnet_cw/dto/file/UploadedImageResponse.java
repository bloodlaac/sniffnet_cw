package ru.yanaeva.sniffnet_cw.dto.file;

import java.time.LocalDateTime;

public record UploadedImageResponse(
        Long id,
        Long userId,
        String originalFilename,
        String storedFilename,
        String contentType,
        Long sizeBytes,
        String storagePath,
        LocalDateTime uploadedAt
) {
}
