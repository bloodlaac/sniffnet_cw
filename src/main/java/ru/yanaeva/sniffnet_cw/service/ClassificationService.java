package ru.yanaeva.sniffnet_cw.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
import ru.yanaeva.sniffnet_cw.entity.ClassificationProbability;
import ru.yanaeva.sniffnet_cw.entity.ClassificationRequest;
import ru.yanaeva.sniffnet_cw.entity.ClassificationStatus;
import ru.yanaeva.sniffnet_cw.entity.ModelEntity;
import ru.yanaeva.sniffnet_cw.entity.UploadedImage;
import ru.yanaeva.sniffnet_cw.exception.BadRequestException;
import ru.yanaeva.sniffnet_cw.exception.NotFoundException;
import ru.yanaeva.sniffnet_cw.integration.ClassificationAdapter;
import ru.yanaeva.sniffnet_cw.integration.ClassificationCommand;
import ru.yanaeva.sniffnet_cw.integration.ClassificationResult;
import ru.yanaeva.sniffnet_cw.repository.ClassificationProbabilityRepository;
import ru.yanaeva.sniffnet_cw.repository.ClassificationRequestRepository;

@Service
public class ClassificationService {

    private final ClassificationRequestRepository classificationRequestRepository;
    private final ClassificationProbabilityRepository classificationProbabilityRepository;
    private final ModelService modelService;
    private final StorageService storageService;
    private final ClassificationAdapter classificationAdapter;
    private final MapperService mapperService;

    public ClassificationService(
        ClassificationRequestRepository classificationRequestRepository,
        ClassificationProbabilityRepository classificationProbabilityRepository,
        ModelService modelService,
        StorageService storageService,
        ClassificationAdapter classificationAdapter,
        MapperService mapperService
    ) {
        this.classificationRequestRepository = classificationRequestRepository;
        this.classificationProbabilityRepository = classificationProbabilityRepository;
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
        savedRequest.setCompletedAt(Instant.now());
        savedRequest.setPredictedClass(result.predictedClass());
        savedRequest.setConfidence(result.confidence());
        savedRequest.setRawResponse(result.message());
        savedRequest = classificationRequestRepository.save(savedRequest);

        List<ClassificationProbability> probabilities = new ArrayList<>();
        for (var entry : result.classProbabilities().entrySet()) {
            ClassificationProbability probability = new ClassificationProbability();
            probability.setRequest(savedRequest);
            probability.setClassName(entry.getKey());
            probability.setProbabilityValue(entry.getValue());
            probabilities.add(probability);
        }
        probabilities = classificationProbabilityRepository.saveAll(probabilities);

        return mapperService.toClassificationResponse(savedRequest, probabilities);
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
        Instant fromInstant = from == null ? null : from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to == null ? null : to
        .plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Page<ClassificationRequest> requestPage;
        if (admin && fromInstant != null && toInstant != null) {
            requestPage = classificationRequestRepository.findByCreatedAtBetween(
                fromInstant,
                toInstant,
                pageable
            );
        } else if (!admin && fromInstant != null && toInstant != null) {
            requestPage = classificationRequestRepository.findByUserIdAndCreatedAtBetween(
                currentUser.getId(),
                fromInstant,
                toInstant,
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
        return mapperService.toClassificationResponse(
            request,
            classificationProbabilityRepository.findByRequestIdOrderByIdAsc(request.getId())
        );
    }
}
