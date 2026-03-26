package ru.yanaeva.sniffnet_cw.dto.auth;

import java.time.LocalDateTime;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        String role,
        Boolean active,
        LocalDateTime createdAt
) {
}
