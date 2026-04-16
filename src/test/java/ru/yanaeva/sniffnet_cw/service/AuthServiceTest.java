package ru.yanaeva.sniffnet_cw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.yanaeva.sniffnet_cw.dto.auth.AuthResponse;
import ru.yanaeva.sniffnet_cw.dto.auth.LoginRequest;
import ru.yanaeva.sniffnet_cw.dto.auth.RefreshTokenRequest;
import ru.yanaeva.sniffnet_cw.dto.auth.RegisterRequest;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.Role;
import ru.yanaeva.sniffnet_cw.entity.RoleCode;
import ru.yanaeva.sniffnet_cw.repository.RoleRepository;
import ru.yanaeva.sniffnet_cw.repository.UserRepository;
import ru.yanaeva.sniffnet_cw.security.AppUserPrincipal;
import ru.yanaeva.sniffnet_cw.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private MapperService mapperService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldCreateUserWithEncodedPasswordAndReturnJwtPair() {
        Role userRole = role(RoleCode.ROLE_USER);
        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new_user@example.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret");
        when(jwtService.generateAccessToken(any(AppUserPrincipal.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(AppUserPrincipal.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(new RegisterRequest(
            "new_user",
            "new_user@example.com",
            "secret123"
        ));

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();

        assertEquals("new_user", savedUser.getUsername());
        assertEquals("new_user@example.com", savedUser.getEmail());
        assertEquals("encoded-secret", savedUser.getPassword());
        assertEquals(RoleCode.ROLE_USER, savedUser.getRole().getCode());
        assertEquals("access-token", response.token());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("new_user", response.username());
    }

    @Test
    void loginShouldAuthenticateCredentialsAndReturnJwtPair() {
        AppUser demo = user("demo", "demo@sniffnet.local", RoleCode.ROLE_USER);
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(demo));
        when(jwtService.generateAccessToken(any(AppUserPrincipal.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(AppUserPrincipal.class))).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("demo", "demo123"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
            ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        UsernamePasswordAuthenticationToken authentication = authenticationCaptor.getValue();
        assertEquals("demo", authentication.getPrincipal());
        assertEquals("demo123", authentication.getCredentials());
        assertEquals("access-token", response.token());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals("demo", response.username());
        assertEquals("ROLE_USER", response.role());
    }

    @Test
    void refreshShouldValidateRefreshTokenAndReturnNewJwtPair() {
        AppUser demo = user("demo", "demo@sniffnet.local", RoleCode.ROLE_USER);
        when(jwtService.extractUsername("refresh-token")).thenReturn("demo");
        when(userRepository.findByUsername("demo")).thenReturn(Optional.of(demo));
        when(jwtService.isRefreshTokenValid(eq("refresh-token"), any(AppUserPrincipal.class))).thenReturn(true);
        when(jwtService.generateAccessToken(any(AppUserPrincipal.class))).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(AppUserPrincipal.class))).thenReturn("new-refresh-token");

        AuthResponse response = authService.refresh(new RefreshTokenRequest("refresh-token"));

        verify(jwtService).isRefreshTokenValid(eq("refresh-token"), any(AppUserPrincipal.class));
        assertNotNull(response);
        assertEquals("new-access-token", response.token());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals("demo", response.username());
    }

    private AppUser user(String username, String email, RoleCode roleCode) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(role(roleCode));
        return user;
    }

    private Role role(RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        role.setName(roleCode.name());
        return role;
    }
}
