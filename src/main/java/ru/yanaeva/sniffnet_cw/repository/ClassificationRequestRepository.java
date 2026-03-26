package ru.yanaeva.sniffnet_cw.repository;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.ClassificationRequest;

public interface ClassificationRequestRepository extends JpaRepository<ClassificationRequest, Long> {
    Page<ClassificationRequest> findByUserId(Long userId, Pageable pageable);

    Page<ClassificationRequest> findByUserIdAndCreatedAtBetween(
        Long userId,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );

    Page<ClassificationRequest> findByCreatedAtBetween(
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );
}
