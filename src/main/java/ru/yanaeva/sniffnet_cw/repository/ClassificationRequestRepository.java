package ru.yanaeva.sniffnet_cw.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.ClassificationRequest;

public interface ClassificationRequestRepository extends JpaRepository<ClassificationRequest, Long> {
    boolean existsByModelId(Long modelId);
    boolean existsByImageId(Long imageId);
    List<ClassificationRequest> findByModelId(Long modelId);
    void deleteByModelId(Long modelId);

    List<ClassificationRequest> findByUserId(Long userId);

    List<ClassificationRequest> findByUserIdAndCreatedAtBetween(
        Long userId,
        LocalDateTime from,
        LocalDateTime to
    );

    List<ClassificationRequest> findByCreatedAtBetween(
        LocalDateTime from,
        LocalDateTime to
    );
}
