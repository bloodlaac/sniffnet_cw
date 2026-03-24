package ru.yanaeva.sniffnet_cw.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yanaeva.sniffnet_cw.entity.UploadedImage;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {
}
