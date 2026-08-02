package id.my.hendisantika.multitenancy.service.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Spy
    private StorageProperties storageProperties = new StorageProperties();

    @InjectMocks
    private S3StorageService storageService;

    private MockMultipartFile photo(String contentType, byte[] content) {
        return new MockMultipartFile("photo", "whatever.jpg", contentType, content);
    }

    @Test
    void storesUnderThePrefixWithAnExtensionDerivedFromTheContentType() {
        String key = storageService.store(photo(MediaType.IMAGE_PNG_VALUE, "png-bytes".getBytes()), "accounts");

        assertThat(key).startsWith("accounts/").endsWith(".png");
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("jvm-uploads");
        assertThat(request.getValue().key()).isEqualTo(key);
        assertThat(request.getValue().contentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
    }

    @Test
    void keyIgnoresTheSubmittedFileNameWhichIsAttackerControlled() {
        MockMultipartFile evil = new MockMultipartFile(
                "photo", "../../etc/passwd.png", MediaType.IMAGE_PNG_VALUE, "x".getBytes());

        String key = storageService.store(evil, "accounts");

        assertThat(key).doesNotContain("..").doesNotContain("passwd");
    }

    @Test
    void rejectsAFileTypeThatIsNotAnAllowedImage() {
        assertThatThrownBy(() -> storageService.store(
                photo("application/pdf", "%PDF".getBytes()), "accounts"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("Unsupported file type");
        verifyNoInteractions(s3Client);
    }

    @Test
    void rejectsAFileLargerThanTheLimit() {
        byte[] tooBig = new byte[(int) storageProperties.getMaxFileSize().toBytes() + 1];

        assertThatThrownBy(() -> storageService.store(photo(MediaType.IMAGE_JPEG_VALUE, tooBig), "accounts"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("larger than");
        verifyNoInteractions(s3Client);
    }

    /**
     * The bucket is private, so a read URL is a signed one, asked for with the
     * key being read and the configured lifetime. That it actually fetches the
     * bytes is {@code S3StorageIntegrationTest}'s job; this one covers what is
     * asked for.
     */
    @Test
    void aPrivateBucketIsReadThroughASignedUrl() throws Exception {
        storageProperties.setEndpoint("http://localhost:9000/");
        storageProperties.setSignedUrlTtl(Duration.ofMinutes(3));

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("http://localhost:9000/jvm-uploads/accounts/a.png?X-Amz-Signature=abc").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        assertThat(storageService.urlOf("accounts/a.png"))
                .isEqualTo("http://localhost:9000/jvm-uploads/accounts/a.png?X-Amz-Signature=abc");

        ArgumentCaptor<GetObjectPresignRequest> request = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(request.capture());
        assertThat(request.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(3));
        assertThat(request.getValue().getObjectRequest().bucket()).isEqualTo("jvm-uploads");
        assertThat(request.getValue().getObjectRequest().key()).isEqualTo("accounts/a.png");
    }

    /**
     * A public bucket or a CDN needs no signature, and adding one would only put
     * a credential in a URL that nothing checks.
     */
    @Test
    void aPublicBaseUrlIsHandedOutPlainly() {
        storageProperties.setPublicBaseUrl("https://cdn.example.test/");

        assertThat(storageService.urlOf("accounts/a.png"))
                .isEqualTo("https://cdn.example.test/accounts/a.png");
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void nothingIsSignedForAMissingKey() {
        assertThat(storageService.urlOf(null)).isNull();
        assertThat(storageService.urlOf("  ")).isNull();
        verifyNoInteractions(s3Presigner);
    }
}
