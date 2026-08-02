package id.my.hendisantika.multitenancy.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises the storage path against a real S3 compatible server rather than a
 * mocked client, so the signing, endpoint and path style settings are covered
 * too.
 * <p>
 * Skips when no endpoint is reachable, which keeps a developer without MinIO
 * running from seeing a failure. CI sets S3_INTEGRATION_REQUIRED so that a MinIO
 * that failed to start is a failure rather than a silent skip.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.28
 */
@SpringBootTest
class S3StorageIntegrationTest {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private StorageService storageService;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private StorageProperties storageProperties;

    @BeforeEach
    void requireStorage() {
        boolean reachable = isReachable(storageProperties.getEndpoint());
        if (!reachable && "true".equalsIgnoreCase(System.getenv("S3_INTEGRATION_REQUIRED"))) {
            fail("S3_INTEGRATION_REQUIRED is set but nothing answers at " + storageProperties.getEndpoint());
        }
        assumeTrue(reachable, "No S3 compatible endpoint at " + storageProperties.getEndpoint() + ", skipping");
        createBucketIfMissing();
    }

    @Test
    void storesAnObjectThatCanBeReadBackAndDeleted() {
        byte[] content = "a tiny pretend png".getBytes();
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "logo.png", MediaType.IMAGE_PNG_VALUE, content);

        String key = storageService.store(photo, "accounts");
        assertThat(key).startsWith("accounts/").endsWith(".png");

        ResponseBytes<GetObjectResponse> stored = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build());
        assertThat(stored.asByteArray()).isEqualTo(content);
        assertThat(stored.response().contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);

        storageService.delete(key);
        assertThatThrownBy(() -> s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(storageProperties.getBucket())
                .key(key)
                .build()))
                .isInstanceOf(NoSuchKeyException.class);
    }

    /**
     * The point of signing: the URL handed to a browser actually fetches the
     * bytes. Asserting its shape instead is what let a plain URL that answers
     * 403 sit in the API for a while.
     */
    @Test
    void aSignedUrlFetchesTheObject() throws Exception {
        byte[] content = "jpeg-ish".getBytes();
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "logo.png", MediaType.IMAGE_JPEG_VALUE, content);
        String key = storageService.store(photo, "organizations");
        try {
            String url = storageService.urlOf(key);
            assertThat(url).contains("X-Amz-Signature");

            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            assertThat(connection.getResponseCode()).isEqualTo(200);
            try (var body = connection.getInputStream()) {
                assertThat(body.readAllBytes()).isEqualTo(content);
            }
            connection.disconnect();
        } finally {
            storageService.delete(key);
        }
    }

    /**
     * And the reason it has to be signed: the bucket is private, so the plain
     * object URL is refused. If this ever passes, the bucket has been opened up
     * and the signing is decoration.
     */
    @Test
    void theUnsignedUrlIsRefused() throws Exception {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "logo.png", MediaType.IMAGE_JPEG_VALUE, "jpeg-ish".getBytes());
        String key = storageService.store(photo, "organizations");
        try {
            String plain = storageProperties.getEndpoint().replaceAll("/$", "")
                    + "/" + storageProperties.getBucket() + "/" + key;
            HttpURLConnection connection = (HttpURLConnection) URI.create(plain).toURL().openConnection();
            assertThat(connection.getResponseCode()).isEqualTo(403);
            connection.disconnect();
        } finally {
            storageService.delete(key);
        }
    }

    /**
     * A signed URL is a bearer token in a query string, so its lifetime is the
     * thing keeping it from being worth passing around.
     */
    @Test
    void theSignedUrlCarriesTheConfiguredLifetime() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "logo.png", MediaType.IMAGE_JPEG_VALUE, "jpeg-ish".getBytes());
        String key = storageService.store(photo, "organizations");
        try {
            assertThat(storageService.urlOf(key))
                    .contains("X-Amz-Expires=" + storageProperties.getSignedUrlTtl().toSeconds());
        } finally {
            storageService.delete(key);
        }
    }

    /**
     * A public bucket or a CDN needs no signature, and adding one would only put
     * a credential in a URL that nothing checks.
     */
    @Test
    void aPublicBaseUrlIsHandedOutPlainly() {
        StorageProperties publicProperties = new StorageProperties();
        publicProperties.setBucket(storageProperties.getBucket());
        publicProperties.setEndpoint(storageProperties.getEndpoint());
        publicProperties.setPublicBaseUrl("https://cdn.example.com/");

        StorageService publicStorage = new S3StorageService(s3Client, s3Presigner, publicProperties);

        assertThat(publicStorage.urlOf("organizations/abc.png"))
                .isEqualTo("https://cdn.example.com/organizations/abc.png");
    }

    @Test
    void nothingIsSignedForAMissingKey() {
        assertThat(storageService.urlOf(null)).isNull();
        assertThat(storageService.urlOf("  ")).isNull();
    }

    private void createBucketIfMissing() {
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .build());
        } catch (BucketAlreadyOwnedByYouException e) {
            // Already there, which is the normal case after the first run.
        }
    }

    private boolean isReachable(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return false;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint + "/minio/health/live")
                    .toURL().openConnection();
            connection.setConnectTimeout(1500);
            connection.setReadTimeout(1500);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            connection.disconnect();
            return status > 0;
        } catch (IOException e) {
            return false;
        }
    }
}
