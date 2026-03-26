package ru.yanaeva.sniffnet_cw.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.yanaeva.sniffnet_cw.dto.classification.ClassificationResponse;
import ru.yanaeva.sniffnet_cw.dto.common.PageResponse;
import ru.yanaeva.sniffnet_cw.dto.file.UploadedImageResponse;
import ru.yanaeva.sniffnet_cw.entity.AppUser;
import ru.yanaeva.sniffnet_cw.entity.ClassificationRequest;
import ru.yanaeva.sniffnet_cw.entity.ClassificationStatus;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;
import ru.yanaeva.sniffnet_cw.entity.UploadedImage;
import ru.yanaeva.sniffnet_cw.exception.BadRequestException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.integration.ClassificationAdapter;
import ru.yanaeva.sniffnet_cw.integration.ClassificationCommand;
import ru.yanaeva.sniffnet_cw.integration.ClassificationResult;
import ru.yanaeva.sniffnet_cw.repository.ClassificationRequestRepository;

@Service
public class ClassificationService {

    private final ClassificationRequestRepository classificationRequestRepository;
    private final ModelService modelService;
    private final StorageService storageService;
    private final ClassificationAdapter classificationAdapter;
    private final MapperService mapperService;

    public ClassificationService(
        ClassificationRequestRepository classificationRequestRepository,
        ModelService modelService,
        StorageService storageService,
        ClassificationAdapter classificationAdapter,
        MapperService mapperService
    ) {
        this.classificationRequestRepository = classificationRequestRepository;
        this.modelService = modelService;
        this.storageService = storageService;
        this.classificationAdapter = classificationAdapter;
        this.mapperService = mapperService;
    }

    @Transactional
    public ClassificationResponse classify(
        Long modelId,
        Long imageId,
        MultipartFile file,
        AppUser user
    ) {
        ModelEntity model = modelService.getEntity(modelId);
        if (!Boolean.TRUE.equals(model.getAvailableForInference())) {
            throw new BadRequestException("Model is not available for inference");
        }

        UploadedImage image = resolveImage(imageId, file, user);
        ClassificationRequest savedRequest = new ClassificationRequest();
        savedRequest.setUser(user);
        savedRequest.setModel(model);
        savedRequest.setImage(image);
        savedRequest.setStatus(ClassificationStatus.CREATED);
        savedRequest = classificationRequestRepository.save(savedRequest);

        ClassificationResult result = classificationAdapter.classify(
            new ClassificationCommand(
                model.getId(),
                model.getName(),
                image.getStoragePath(),
                image.getContentType()
            )
        );

        savedRequest.setStatus(ClassificationStatus.COMPLETED);
        savedRequest.setCompletedAt(LocalDateTime.now());
        savedRequest.setPredictedClass(result.predictedClass());
        savedRequest.setConfidence(result.confidence());
        savedRequest.setProbabilitiesJson(
            mapperService.writeProbabilities(result.classProbabilities())
        );
        savedRequest = classificationRequestRepository.save(savedRequest);

        return mapperService.toClassificationResponse(savedRequest);
    }

    public PageResponse<ClassificationResponse> getClassifications(
        AppUser currentUser,
        boolean admin,
        LocalDate from,
        LocalDate to,
        int page,
        int size,
        String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay();

        Page<ClassificationRequest> requestPage;
        if (admin && fromDateTime != null && toDateTime != null) {
            requestPage = classificationRequestRepository.findByCreatedAtBetween(
                fromDateTime,
                toDateTime,
                pageable
            );
        } else if (!admin && fromDateTime != null && toDateTime != null) {
            requestPage = classificationRequestRepository.findByUserIdAndCreatedAtBetween(
                currentUser.getId(),
                fromDateTime,
                toDateTime,
                pageable
            );
        } else if (admin) {
            requestPage = classificationRequestRepository.findAll(pageable);
        } else {
            requestPage = classificationRequestRepository.findByUserId(currentUser.getId(), pageable);
        }

        return PageResponse.from(requestPage.map(this::toClassificationResponse));
    }

    public ClassificationResponse getClassification(Long id, AppUser currentUser, boolean admin) {
        ClassificationRequest request = classificationRequestRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Classification request not found"));
        if (!admin && !request.getUser().getId().equals(currentUser.getId())) {
            throw new NotFoundException("Classification request not found");
        }
        return toClassificationResponse(request);
    }

    private UploadedImage resolveImage(Long imageId, MultipartFile file, AppUser user) {
        if (imageId != null) {
            UploadedImage image = storageService.getEntity(imageId);
            if (!image.getUser().getId().equals(user.getId())) {
                throw new NotFoundException("Image not found");
            }
            return image;
        }
        if (file != null && !file.isEmpty()) {
            UploadedImageResponse uploaded = storageService.upload(file, user);
            return storageService.getEntity(uploaded.id());
        }
        throw new BadRequestException("Either imageId or file must be provided");
    }

    private ClassificationResponse toClassificationResponse(ClassificationRequest request) {
        return mapperService.toClassificationResponse(request);
    }
}
