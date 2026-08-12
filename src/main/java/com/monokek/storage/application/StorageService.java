package com.monokek.storage.application;

import com.monokek.common.ApiException;
import com.monokek.storage.web.dto.StorageObjectDto;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Generic upload use case: validate, then stream straight to MinIO — no
 * temp file on disk. {@code folder} is caller-chosen (e.g. {@code "products"})
 * purely for readability in the bucket; it has no meaning to any other module.
 */
@Service
public class StorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 Mo — même limite que MAX_UPLOAD_SIZE côté frontend

    private final MinioClient minioClient;
    private final String bucket;
    private final String publicEndpoint;

    public StorageService(
            MinioClient minioClient,
            @Value("${app.minio.bucket}") String bucket,
            @Value("${app.minio.public-endpoint}") String publicEndpoint) {
        this.minioClient = minioClient;
        this.bucket = bucket;
        this.publicEndpoint = publicEndpoint.endsWith("/") ? publicEndpoint.substring(0, publicEndpoint.length() - 1) : publicEndpoint;
    }

    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Aucun fichier fourni.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Type de fichier non autorisé (jpeg, png, webp, gif uniquement).");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw ApiException.badRequest("Fichier trop volumineux (5 Mo maximum).");
        }

        String key = "%s/%s%s".formatted(sanitizeFolder(folder), UUID.randomUUID(), extensionFor(contentType));

        try (InputStream stream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(stream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            // Couvre toutes les exceptions checked du SDK MinIO (réseau, signature, XML...) —
            // remonte au handler générique de GlobalExceptionHandler comme toute autre panne d'infra.
            throw new RuntimeException("Échec de l'envoi du fichier vers le stockage.", e);
        }

        return "%s/%s/%s".formatted(publicEndpoint, bucket, key);
    }

    /** Lists objects under {@code folder} (default {@code "uploads"}), most recent first. */
    public List<StorageObjectDto> list(String folder) {
        String prefix = sanitizeFolder(folder) + "/";
        List<StorageObjectDto> objects = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .recursive(true)
                    .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir()) {
                    continue;
                }
                objects.add(new StorageObjectDto(
                        item.objectName(),
                        "%s/%s/%s".formatted(publicEndpoint, bucket, item.objectName()),
                        item.size(),
                        item.lastModified() != null ? item.lastModified().toInstant() : null));
            }
        } catch (Exception e) {
            throw new RuntimeException("Échec de la récupération des fichiers depuis le stockage.", e);
        }
        objects.sort(Comparator.comparing(StorageObjectDto::lastModified, Comparator.nullsLast(Comparator.reverseOrder())));
        return objects;
    }

    private String sanitizeFolder(String folder) {
        String value = (folder == null || folder.isBlank()) ? "uploads" : folder.trim().toLowerCase();
        String cleaned = value.replaceAll("[^a-z0-9/_-]", "");
        return cleaned.isEmpty() ? "uploads" : cleaned;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> "";
        };
    }
}
