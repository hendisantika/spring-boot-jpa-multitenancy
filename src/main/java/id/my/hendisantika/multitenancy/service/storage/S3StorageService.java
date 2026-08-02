package id.my.hendisantika.multitenancy.service.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores uploads on any S3 compatible endpoint.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties storageProperties;

    @Override
    public String store(MultipartFile file, String prefix) {
        validate(file);
        String key = "%s/%s%s".formatted(prefix, UUID.randomUUID(), extensionOf(file));
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new StorageException("Could not read the uploaded file", e);
        }
        log.debug("Stored upload at {}", key);
        return key;
    }

    /**
     * A URL a browser can actually fetch.
     * <p>
     * The bucket is private, so the plain object URL answers 403. This signs a
     * short-lived GET instead, which is why the stored key is the thing kept in
     * the database and the URL is built fresh on every read.
     * <p>
     * A signed URL is a bearer token in a query string: anybody holding it can
     * read that one object until it expires, with no session. That is the whole
     * mechanism, and the reason the lifetime is short.
     * <p>
     * When {@code publicBaseUrl} is set the objects are readable without
     * credentials anyway — a CDN or a public bucket — so signing would add a
     * signature nothing checks, and the plain URL is returned.
     */
    @Override
    public String urlOf(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        if (StringUtils.hasText(storageProperties.getPublicBaseUrl())) {
            return "%s/%s".formatted(trimTrailingSlash(storageProperties.getPublicBaseUrl()), key);
        }
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build();
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(storageProperties.getSignedUrlTtl())
                        .getObjectRequest(get)
                        .build())
                .url()
                .toExternalForm();
    }

    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("The uploaded file is empty");
        }
        if (file.getSize() > storageProperties.getMaxFileSize().toBytes()) {
            throw new StorageException("The uploaded file is larger than "
                    + storageProperties.getMaxFileSize().toMegabytes() + " MB");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!storageProperties.getAllowedContentTypes().contains(contentType)) {
            throw new StorageException("Unsupported file type '" + contentType + "', allowed: "
                    + String.join(", ", storageProperties.getAllowedContentTypes()));
        }
    }

    /**
     * Derived from the content type rather than the submitted file name, which is
     * attacker controlled.
     */
    private String extensionOf(MultipartFile file) {
        return switch (file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
