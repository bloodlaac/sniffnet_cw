package ru.yanaeva.sniffnet_cw.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.TrainingConfig;

public interface TrainingConfigRepository extends JpaRepository<TrainingConfig, Long> {
}
