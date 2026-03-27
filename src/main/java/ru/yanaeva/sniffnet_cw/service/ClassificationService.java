package ru.yanaeva.sniffnet_cw.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.yanaeva.sniffnet_cw.dto.classification.ClassificationResponse;
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
    private final TrainingSyncService trainingSyncService;
    private final MapperService mapperService;

    public ClassificationService(
        ClassificationRequestRepository classificationRequestRepository,
        ModelService modelService,
        StorageService storageService,
        ClassificationAdapter classificationAdapter,
        TrainingSyncService trainingSyncService,
        MapperService mapperService
    ) {
        this.classificationRequestRepository = classificationRequestRepository;
        this.modelService = modelService;
        this.storageService = storageService;
        this.classificationAdapter = classificationAdapter;
        this.trainingSyncService = trainingSyncService;
        this.mapperService = mapperService;
    }

    @Transactional
    public ClassificationResponse classify(
        Long modelId,
        Long imageId,
        MultipartFile file,
        AppUser user
    ) {
        ModelEntity model = modelService.getAccessibleEntity(modelId, user, false);
        trainingSyncService.syncExperiment(model.getExperiment());
        if (!Boolean.TRUE.equals(model.getAvailableForInference())) {
            throw new BadRequestException("Model is not available for inference");
        }
        if (model.getExternalModelId() == null) {
            throw new BadRequestException("Model is not synchronized with Python service yet");
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
                model.getExternalModelId(),
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

    @Transactional(readOnly = true)
    public List<ClassificationResponse> getClassifications(
        AppUser currentUser,
        boolean admin,
        Long userId,
        LocalDate from,
        LocalDate to
    ) {
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay();
        Long effectiveUserId = admin ? userId : currentUser.getId();

        List<ClassificationRequest> requests;
        if (effectiveUserId != null && fromDateTime != null && toDateTime != null) {
            requests = classificationRequestRepository.findByUserIdAndCreatedAtBetween(
                effectiveUserId,
                fromDateTime,
                toDateTime
            );
        } else if (admin && fromDateTime != null && toDateTime != null) {
            requests = classificationRequestRepository.findByCreatedAtBetween(
                fromDateTime,
                toDateTime
            );
        } else if (effectiveUserId != null) {
            requests = classificationRequestRepository.findByUserId(effectiveUserId);
        } else if (admin) {
            requests = classificationRequestRepository.findAll();
        } else {
            requests = classificationRequestRepository.findByUserId(currentUser.getId());
        }

        return requests.stream()
            .sorted(Comparator.comparing(ClassificationRequest::getCreatedAt).reversed())
            .map(this::toClassificationResponse)
            .toList();
    }

    @Transactional(readOnly = true)
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
