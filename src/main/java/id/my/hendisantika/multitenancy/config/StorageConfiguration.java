package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.service.storage.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Builds the S3 client. Pointing it at MinIO only takes an endpoint and a key
 * pair; leaving those empty falls back to the AWS default credential chain.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

    @Bean
    @DependsOn("productionCredentialsValidator")
    public S3Client s3Client(StorageProperties storageProperties) {
        var builder = S3Client.builder()
                .region(Region.of(storageProperties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storageProperties.isPathStyleAccess())
                        .build());

        if (StringUtils.hasText(storageProperties.getEndpoint())) {
            builder.endpointOverride(URI.create(storageProperties.getEndpoint()));
        }
        if (StringUtils.hasText(storageProperties.getAccessKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(storageProperties.getAccessKey(), storageProperties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    /**
     * Signs the read URLs handed to a browser.
     * <p>
     * Built separately from the client because it may have to sign against a
     * different address: a signature covers the host, so inside a compose
     * network the application reaches MinIO at {@code minio:9000} while the
     * browser needs the published one, and the URL has to be signed for the
     * latter.
     */
    @Bean
    @DependsOn("productionCredentialsValidator")
    public S3Presigner s3Presigner(StorageProperties storageProperties) {
        var builder = S3Presigner.builder()
                .region(Region.of(storageProperties.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storageProperties.isPathStyleAccess())
                        .build());

        String endpoint = StringUtils.hasText(storageProperties.getSignedUrlEndpoint())
                ? storageProperties.getSignedUrlEndpoint()
                : storageProperties.getEndpoint();
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        if (StringUtils.hasText(storageProperties.getAccessKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(storageProperties.getAccessKey(), storageProperties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
