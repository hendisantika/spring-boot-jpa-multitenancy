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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
        assertThat(request.getValue().bucket()).isEqualTo("mhdc-uploads");
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

    @Test
    void buildsAUrlFromTheEndpointOrTheConfiguredPublicBase() {
        storageProperties.setEndpoint("http://localhost:9000/");
        assertThat(storageService.urlOf("accounts/a.png"))
                .isEqualTo("http://localhost:9000/mhdc-uploads/accounts/a.png");

        storageProperties.setPublicBaseUrl("https://cdn.example.test/");
        assertThat(storageService.urlOf("accounts/a.png"))
                .isEqualTo("https://cdn.example.test/accounts/a.png");

        assertThat(storageService.urlOf(null)).isNull();
    }
}
