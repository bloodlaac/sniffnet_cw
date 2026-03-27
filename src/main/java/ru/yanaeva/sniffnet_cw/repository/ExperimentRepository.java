package ru.yanaeva.sniffnet_cw.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import ru.yanaeva.sniffnet_cw.entity.Experiment;
import ru.yanaeva.sniffnet_cw.entity.ExperimentStatus;

public interface ExperimentRepository extends JpaRepository<Experiment, Long> {
    long countByConfigId(Long configId);

    List<Experiment> findByUserId(Long userId);

    List<Experiment> findByStatus(ExperimentStatus status);

    List<Experiment> findByUserIdAndStatus(Long userId, ExperimentStatus status);

    List<Experiment> findByStatusIn(Collection<ExperimentStatus> statuses);
}
