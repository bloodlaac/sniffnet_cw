package ru.yanaeva.sniffnet_cw.dto.user;

import java.time.Instant;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        Boolean active,
        LocalDateTime createdAt
) {
}
