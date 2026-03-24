package ru.yanaeva.sniffnet_cw.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.Dataset;
import ru.yanaeva.sniffnet_cw.entity.Role;
import ru.yanaeva.sniffnet_cw.entity.RoleCode;
import ru.yanaeva.sniffnet_cw.repository.DatasetRepository;
import ru.yanaeva.sniffnet_cw.repository.RoleRepository;
import ru.yanaeva.sniffnet_cw.repository.UserRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final DatasetRepository datasetRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
        RoleRepository roleRepository,
        UserRepository userRepository,
        DatasetRepository datasetRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.datasetRepository = datasetRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role userRole = roleRepository.findByCode(RoleCode.ROLE_USER)
        .orElseGet(() -> createRole(RoleCode.ROLE_USER, "User"));
        
        Role adminRole = roleRepository.findByCode(RoleCode.ROLE_ADMIN)
        .orElseGet(() -> createRole(RoleCode.ROLE_ADMIN, "Administrator"));

        if (!userRepository.existsByUsernameIgnoreCase("admin")) {
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setEmail("admin@sniffnet.local");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(adminRole);
            admin.setActive(true);
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsernameIgnoreCase("demo")) {
            AppUser demo = new AppUser();
            demo.setUsername("demo");
            demo.setEmail("demo@sniffnet.local");
            demo.setPassword(passwordEncoder.encode("demo123"));
            demo.setRole(userRole);
            demo.setActive(true);
            userRepository.save(demo);
        }

        seedDataset("Freshness Fruits", 2, "kaggle://freshness-fruits");
        seedDataset("Vegetable Shelf Life", 3, "mock://vegetable-shelf-life");
    }

    private Role createRole(RoleCode code, String name) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        return roleRepository.save(role);
    }

    private void seedDataset(String name, int classesNum, String source) {
        if (!datasetRepository.existsByNameIgnoreCase(name)) {
            Dataset dataset = new Dataset();
            dataset.setName(name);
            dataset.setClassesNum(classesNum);
            dataset.setSource(source);
            datasetRepository.save(dataset);
        }
    }
}
