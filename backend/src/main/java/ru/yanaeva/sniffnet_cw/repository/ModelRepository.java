package ru.yanaeva.sniffnet_cw.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;

public interface ModelRepository extends JpaRepository<ModelEntity, Long> {
    List<ModelEntity> findByDatasetId(Long datasetId);

    List<ModelEntity> findByExperimentUserId(Long userId);

    List<ModelEntity> findByDatasetIdAndExperimentUserId(Long datasetId, Long userId);

    Optional<ModelEntity> findByExperimentId(Long experimentId);
}
