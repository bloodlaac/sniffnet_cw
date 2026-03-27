package ru.yanaeva.sniffnet_cw.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.yanaeva.sniffnet_cw.config.AppProperties;
import ru.yanaeva.sniffnet_cw.dto.file.UploadedImageResponse;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.UploadedImage;
import ru.yanaeva.sniffnet_cw.exception.BadRequestException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.repository.UploadedImageRepository;

@Service
public class StorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png");

    private final AppProperties appProperties;
    private final UploadedImageRepository uploadedImageRepository;
    private final MapperService mapperService;

    public StorageService(
        AppProperties appProperties,
        UploadedImageRepository uploadedImageRepository,
        MapperService mapperService
    ) {
        this.appProperties = appProperties;
        this.uploadedImageRepository = uploadedImageRepository;
        this.mapperService = mapperService;
    }

    @Transactional
    public UploadedImageResponse upload(MultipartFile file, AppUser user) {
        validateFile(file);
        try {
            Path directory = Path.of(
                appProperties.getStorage().getImageDirectory()
            ).toAbsolutePath().normalize();
            
            Files.createDirectories(directory);

            String extension = extractExtension(file.getOriginalFilename());
            String storedName = UUID.randomUUID() + extension;
            Path destination = directory.resolve(storedName).normalize();
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            UploadedImage image = new UploadedImage();
            image.setUser(user);
            image.setOriginalFilename(file.getOriginalFilename());
            image.setStoredFilename(storedName);
            image.setContentType(file.getContentType());
            image.setSizeBytes(file.getSize());
            image.setStoragePath(destination.toString());
            return mapperService.toUploadedImageResponse(uploadedImageRepository.save(image));
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store image");
        }
    }

    @Transactional(readOnly = true)
    public UploadedImageResponse getMetadata(Long id) {
        return mapperService.toUploadedImageResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public UploadedImageResponse getMetadata(Long id, AppUser currentUser, boolean admin) {
        UploadedImage image = getEntity(id);
        if (!admin && !image.getUser().getId().equals(currentUser.getId())) {
            throw new NotFoundException("Image not found");
        }
        return mapperService.toUploadedImageResponse(image);
    }

    @Transactional(readOnly = true)
    public Resource loadFile(Long id, AppUser currentUser, boolean admin) {
        UploadedImage image = getEntity(id);
        if (!admin && !image.getUser().getId().equals(currentUser.getId())) {
            throw new NotFoundException("Image not found");
        }
        return new FileSystemResource(image.getStoragePath());
    }

    public UploadedImage getEntity(Long id) {
        return uploadedImageRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Image not found"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > appProperties.getStorage().getMaxImageSizeBytes()) {
            throw new BadRequestException("Image file is too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported image content type");
        }
        String extension = extractExtension(file.getOriginalFilename()).toLowerCase();
        if (!Set.of(".jpg", ".jpeg", ".png").contains(extension)) {
            throw new BadRequestException("Unsupported image extension");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BadRequestException("Image filename must contain an extension");
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
