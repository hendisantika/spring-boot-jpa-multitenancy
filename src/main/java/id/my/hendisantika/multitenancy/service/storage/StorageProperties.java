package id.my.hendisantika.multitenancy.service.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
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
     * <p>
     * Setting this says the objects are readable without credentials, so URLs
     * are handed out plainly rather than signed. Leave it empty for a private
     * bucket, which is the default and what MinIO gives you.
     */
    private String publicBaseUrl;

    /**
     * The endpoint a signed URL is signed against, when the browser reaches the
     * bucket by a different address than the application does.
     * <p>
     * This is not a nicety: a signature covers the host, so a URL signed for
     * {@code http://minio:9000} inside a compose network cannot be repointed at
     * a public address afterwards — it has to be signed for the address the
     * browser will use. Empty means sign against {@link #endpoint}.
     */
    private String signedUrlEndpoint;

    /**
     * How long a signed URL stays usable. Anybody holding one can read the
     * object until it expires, without a token, so this is short: long enough
     * to render a page and come back to it, not long enough to be worth
     * passing around.
     */
    private Duration signedUrlTtl = Duration.ofMinutes(15);

    private DataSize maxFileSize = DataSize.ofMegabytes(5);

    private List<String> allowedContentTypes = List.of("image/jpeg", "image/png", "image/webp");
}
