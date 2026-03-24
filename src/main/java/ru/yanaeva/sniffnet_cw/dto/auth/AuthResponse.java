package ru.yanaeva.sniffnet_cw.dto.auth;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        String email,
        String role
) {
}
