package com.monokek.storage.web;

import com.monokek.storage.application.StorageService;
import com.monokek.storage.web.dto.StorageObjectDto;
import com.monokek.storage.web.dto.UploadResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Generic upload endpoint, gated by the existing {@code /api/admin/**} →
 * {@code hasAnyRole("ADMIN", "MANAGER")} rule in {@code SecurityConfig} — no
 * security config change needed. Returns a bare DTO, matching the other
 * simple "new functionality" endpoints ({@code CategoryController},
 * {@code CompanyController}, ...), not the {@code ApiResponse} envelope
 * {@code identity}/{@code ordering} use for some of theirs.
 */
@RestController
/** Defense in depth: {@code SecurityConfig} already gates {@code /api/admin/**} to ADMIN/MANAGER — this is a second, method-level check that survives even if the path-based rule is ever misconfigured. */
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class StorageController {

    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/api/admin/storage/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponseDto upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {
        return new UploadResponseDto(storageService.store(file, folder));
    }

    @GetMapping("/api/admin/storage/images")
    public List<StorageObjectDto> listImages(@RequestParam(value = "folder", required = false) String folder) {
        return storageService.list(folder);
    }
}
