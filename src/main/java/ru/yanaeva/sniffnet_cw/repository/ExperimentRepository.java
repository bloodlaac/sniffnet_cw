package ru.yanaeva.sniffnet_cw.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.Experiment;
import ru.yanaeva.sniffnet_cw.entity.ExperimentStatus;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    Page<Experiment> findByUserId(Long userId, Pageable pageable);

    Page<Experiment> findByStatus(ExperimentStatus status, Pageable pageable);

    Page<Experiment> findByUserIdAndStatus(Long userId, ExperimentStatus status, Pageable pageable);
}
