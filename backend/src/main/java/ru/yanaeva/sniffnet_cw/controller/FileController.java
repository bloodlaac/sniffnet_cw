package ru.yanaeva.sniffnet_cw.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.yanaeva.sniffnet_cw.dto.file.UploadedImageResponse;
import ru.yanaeva.sniffnet_cw.security.AppUserPrincipal;
import ru.yanaeva.sniffnet_cw.service.StorageService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/files/images")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadedImageResponse upload(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return storageService.upload(file, principal.getUser());
    }

    @GetMapping("/{id}")
    public UploadedImageResponse getMetadata(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return storageService.getMetadata(
            id,
            principal.getUser(),
            "ROLE_ADMIN".equals(principal.getRoleCode())
        );
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> getContent(
        @PathVariable Long id,
        @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        Resource resource = storageService.loadFile(
            id,
            principal.getUser(),
            "ROLE_ADMIN".equals(principal.getRoleCode())
        );
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }
}
