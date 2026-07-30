package id.my.hendisantika.multitenancy.service.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;

/**
 * Settings for the S3 compatible bucket that holds uploaded photos.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.storage")
public class StorageProperties {

    private String bucket = "jvm-uploads";

    private String region = "us-east-1";

    /**
     * Override for MinIO or another S3 compatible endpoint. Empty means AWS S3.
     */
    private String endpoint;

    private String accessKey;

    private String secretKey;

    /**
     * MinIO needs path style access; AWS S3 does not.
     */
    private boolean pathStyleAccess = true;

    /**
     * Base URL objects are served from, when a CDN or public bucket URL differs
     * from the API endpoint.
     */
    private String publicBaseUrl;

    private DataSize maxFileSize = DataSize.ofMegabytes(5);

    private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
}
