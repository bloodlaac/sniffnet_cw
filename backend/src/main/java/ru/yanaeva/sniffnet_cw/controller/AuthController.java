package ru.yanaeva.sniffnet_cw.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yanaeva.sniffnet_cw.dto.auth.AuthResponse;
import ru.yanaeva.sniffnet_cw.dto.auth.CurrentUserResponse;
import ru.yanaeva.sniffnet_cw.dto.auth.LoginRequest;
import ru.yanaeva.sniffnet_cw.dto.auth.RefreshTokenRequest;
import ru.yanaeva.sniffnet_cw.dto.auth.RegisterRequest;
import ru.yanaeva.sniffnet_cw.security.AppUserPrincipal;
import ru.yanaeva.sniffnet_cw.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@org.springframework.security.core.annotation.AuthenticationPrincipal AppUserPrincipal principal) {
        return authService.me(principal);
    }
}
