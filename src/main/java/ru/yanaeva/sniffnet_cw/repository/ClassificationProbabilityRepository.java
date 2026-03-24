package ru.yanaeva.sniffnet_cw.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.ClassificationProbability;

public interface ClassificationProbabilityRepository extends JpaRepository<ClassificationProbability, Long> {
    List<ClassificationProbability> findByRequestIdOrderByIdAsc(Long requestId);
}
