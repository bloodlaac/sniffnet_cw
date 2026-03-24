package ru.yanaeva.sniffnet_cw.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;

public interface ModelRepository extends JpaRepository<ModelEntity, Long> {
    Page<ModelEntity> findByDatasetId(Long datasetId, Pageable pageable);

    Optional<ModelEntity> findByExperimentId(Long experimentId);
}
