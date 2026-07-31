package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.service.storage.StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Checks the database and storage credentials before anything tries to use them,
 * so a misconfigured production deployment stops with an explanation rather than
 * a connection error.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 07.04
 */
@Slf4j
@Component
public class ProductionCredentialsValidator {

    private static final String[] PRODUCTION_PROFILES = {"prod", "production", "staging"};

    private final Environment environment;
    private final DatabaseProperties databaseProperties;
    private final StorageProperties storageProperties;

    public ProductionCredentialsValidator(Environment environment,
                                          DatabaseProperties databaseProperties,
                                          StorageProperties storageProperties) {
        this.environment = environment;
        this.databaseProperties = databaseProperties;
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void validate() {
        if (!environment.matchesProfiles(PRODUCTION_PROFILES)) {
            warnAboutDevelopmentValues();
            return;
        }

        ProductionCredentialsPolicy.require(
                "application.database.user", "APPLICATION_DATABASE_USER",
                databaseProperties.getUser());
        ProductionCredentialsPolicy.require(
                "application.database.password", "APPLICATION_DATABASE_PASSWORD",
                databaseProperties.getPassword(),
                ProductionCredentialsPolicy.DEVELOPMENT_DATABASE_PASSWORD);

        // An empty access key is a legitimate production setup: it means the AWS
        // default credential chain, which is how an IAM role is picked up.
        ProductionCredentialsPolicy.requireIfPresent(
                "application.storage.access-key", "APPLICATION_STORAGE_ACCESS_KEY",
                storageProperties.getAccessKey(),
                ProductionCredentialsPolicy.DEVELOPMENT_STORAGE_KEY);
        ProductionCredentialsPolicy.requireIfPresent(
                "application.storage.secret-key", "APPLICATION_STORAGE_SECRET_KEY",
                storageProperties.getSecretKey(),
                ProductionCredentialsPolicy.DEVELOPMENT_STORAGE_KEY);

        if (StringUtils.hasText(storageProperties.getAccessKey())
                && !StringUtils.hasText(storageProperties.getSecretKey())) {
            throw new IllegalStateException(
                    "application.storage.access-key is set but application.storage.secret-key is not. "
                            + "Supply both, or neither to use the AWS default credential chain.");
        }
        if (!StringUtils.hasText(storageProperties.getAccessKey())) {
            log.info("No storage access key configured; falling back to the AWS default credential chain.");
        }
    }

    private void warnAboutDevelopmentValues() {
        if (ProductionCredentialsPolicy.DEVELOPMENT_DATABASE_PASSWORD.equals(databaseProperties.getPassword())
                || ProductionCredentialsPolicy.DEVELOPMENT_STORAGE_KEY.equals(storageProperties.getAccessKey())) {
            log.warn("Using the development database and storage credentials from application.properties. "
                    + "They are public in version control; run with the prod profile and real credentials "
                    + "before exposing this instance to anyone.");
        }
    }
}
