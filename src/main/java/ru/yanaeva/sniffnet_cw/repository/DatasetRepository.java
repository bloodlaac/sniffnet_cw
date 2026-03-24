package ru.yanaeva.sniffnet_cw.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.Dataset;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    boolean existsByNameIgnoreCase(String name);
}
