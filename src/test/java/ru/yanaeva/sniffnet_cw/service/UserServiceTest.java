package ru.yanaeva.sniffnet_cw.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import ru.yanaeva.sniffnet_cw.dto.user.UserResponse;
import ru.yanaeva.sniffnet_cw.dto.user.UserUpdateRequest;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.Role;
import ru.yanaeva.sniffnet_cw.entity.RoleCode;
import ru.yanaeva.sniffnet_cw.exception.ConflictException;
import ru.yanaeva.sniffnet_cw.repository.RoleRepository;
import ru.yanaeva.sniffnet_cw.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MapperService mapperService;

    @InjectMocks
    private UserService userService;

    @Test
    void updateUserShouldChangeMainFieldsAndRole() {
        AppUser user = user("old_user", "old_user@example.com", RoleCode.ROLE_USER);
        Role adminRole = role(RoleCode.ROLE_ADMIN);
        UserUpdateRequest request = new UserUpdateRequest(
            "new_user",
            "new_user@example.com",
            "ROLE_ADMIN"
        );
        UserResponse mappedResponse = new UserResponse(
            1L,
            "new_user",
            "new_user@example.com",
            "ROLE_ADMIN",
            null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new_user@example.com")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapperService.toUserResponse(any(AppUser.class))).thenReturn(mappedResponse);

        UserResponse response = userService.updateUser(1L, request);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();

        assertEquals("new_user", savedUser.getUsername());
        assertEquals("new_user@example.com", savedUser.getEmail());
        assertEquals(RoleCode.ROLE_ADMIN, savedUser.getRole().getCode());
        assertEquals("new_user", response.username());
        assertEquals("ROLE_ADMIN", response.role());
    }

    @Test
    void updateUserShouldRejectExistingUsername() {
        AppUser user = user("old_user", "old_user@example.com", RoleCode.ROLE_USER);
        UserUpdateRequest request = new UserUpdateRequest(
            "busy_user",
            "old_user@example.com",
            "ROLE_USER"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("busy_user")).thenReturn(true);

        ConflictException exception = assertThrows(
            ConflictException.class,
            () -> userService.updateUser(1L, request)
        );

        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void deleteUserShouldDeleteExistingUser() {
        AppUser user = user("deleted_user", "deleted_user@example.com", RoleCode.ROLE_USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(eq(user));
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
