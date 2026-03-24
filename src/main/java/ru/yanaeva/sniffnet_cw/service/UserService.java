package ru.yanaeva.sniffnet_cw.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yanaeva.sniffnet_cw.dto.common.PageResponse;
import ru.yanaeva.sniffnet_cw.dto.user.UserResponse;
import ru.yanaeva.sniffnet_cw.dto.user.UserUpdateRequest;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.Role;
import ru.yanaeva.sniffnet_cw.entity.RoleCode;
import ru.yanaeva.sniffnet_cw.exception.ConflictException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.repository.RoleRepository;
import ru.yanaeva.sniffnet_cw.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MapperService mapperService;

    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        MapperService mapperService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.mapperService = mapperService;
    }

    public PageResponse<UserResponse> getUsers(String search, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return PageResponse.from((search == null || search.isBlank()
            ? userRepository.findAll(pageable)
            : userRepository.findByUsernameContainingIgnoreCase(search, pageable))
            .map(mapperService::toUserResponse));
    }

    public UserResponse getUser(Long id) {
        return mapperService.toUserResponse(getEntity(id));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        AppUser user = getEntity(id);
        if (!user.getUsername().equalsIgnoreCase(request.username())
            && userRepository.existsByUsernameIgnoreCase(request.username())
        ) {
            throw new ConflictException("Username already exists");
        }
        if (!user.getEmail().equalsIgnoreCase(request.email())
            && userRepository.existsByEmailIgnoreCase(request.email())
        ) {
            throw new ConflictException("Email already exists");
        }

        Role role = roleRepository.findByCode(RoleCode.valueOf(request.role()))
            .orElseThrow(() -> new NotFoundException("Role not found"));

        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setRole(role);
        user.setActive(request.active());
        return mapperService.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(getEntity(id));
    }

    public AppUser getEntity(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
