package ru.yanaeva.sniffnet_cw.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.Metric;

public interface MetricRepository extends JpaRepository<Metric, Long> {
    Optional<Metric> findByModelId(Long modelId);
}
