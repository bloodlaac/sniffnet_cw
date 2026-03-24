package ru.yanaeva.sniffnet_cw.dto.dataset;

public record DatasetResponse(
        Long id,
        String name,
        Integer classesNum,
        String source
) {
}
