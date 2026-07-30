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

    @Test
    void urlPointsAtTheStoredObject() {
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "logo.png", MediaType.IMAGE_JPEG_VALUE, "jpeg-ish".getBytes());
        String key = storageService.store(photo, "organizations");
        try {
            assertThat(storageService.urlOf(key))
                    .isEqualTo(storageProperties.getEndpoint().replaceAll("/$", "")
                            + "/" + storageProperties.getBucket() + "/" + key);
        } finally {
            storageService.delete(key);
        }
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
