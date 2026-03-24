package ru.yanaeva.sniffnet_cw.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yanaeva.sniffnet_cw.dto.auth.AuthResponse;
import ru.yanaeva.sniffnet_cw.dto.auth.CurrentUserResponse;
import ru.yanaeva.sniffnet_cw.dto.auth.LoginRequest;
import ru.yanaeva.sniffnet_cw.dto.auth.RegisterRequest;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.Role;
import ru.yanaeva.sniffnet_cw.entity.RoleCode;
import ru.yanaeva.sniffnet_cw.exception.ConflictException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.repository.RoleRepository;
import ru.yanaeva.sniffnet_cw.repository.UserRepository;
import ru.yanaeva.sniffnet_cw.security.AppUserPrincipal;
import ru.yanaeva.sniffnet_cw.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MapperService mapperService;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        MapperService mapperService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mapperService = mapperService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already exists");
        }

        Role role = roleRepository.findByCode(RoleCode.ROLE_USER)
            .orElseThrow(() -> new NotFoundException("Default user role not found"));

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        AppUser user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new NotFoundException("User not found"));
        return buildAuthResponse(user);
    }

    public CurrentUserResponse me(AppUserPrincipal principal) {
        AppUser user = userRepository.findById(principal.getId())
            .orElseThrow(() -> new NotFoundException("User not found"));
        return mapperService.toCurrentUserResponse(user);
    }

    private AuthResponse buildAuthResponse(AppUser user) {
        AppUserPrincipal principal = new AppUserPrincipal(user);
        return new AuthResponse(
            jwtService.generateToken(principal),
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().getCode().name()
        );
    }
}
