package ru.yanaeva.sniffnet_cw.dto.user;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        LocalDateTime createdAt
) {
}
