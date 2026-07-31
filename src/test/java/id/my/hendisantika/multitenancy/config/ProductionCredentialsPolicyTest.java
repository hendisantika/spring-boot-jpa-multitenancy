package id.my.hendisantika.multitenancy.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 07.04
 */
class ProductionCredentialsPolicyTest {

    @Test
    void rejectsAMissingValue() {
        assertThatThrownBy(() -> ProductionCredentialsPolicy.require(
                "application.database.password", "APPLICATION_DATABASE_PASSWORD", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not set")
                .hasMessageContaining("APPLICATION_DATABASE_PASSWORD");
    }

    @Test
    void rejectsAnUnresolvedPlaceholder() {
        assertThatThrownBy(() -> ProductionCredentialsPolicy.require(
                "application.database.password", "APPLICATION_DATABASE_PASSWORD",
                "${APPLICATION_DATABASE_PASSWORD}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unresolved placeholder");
    }

    @Test
    void rejectsTheDevelopmentValue() {
        assertThatThrownBy(() -> ProductionCredentialsPolicy.require(
                "application.database.password", "APPLICATION_DATABASE_PASSWORD",
                ProductionCredentialsPolicy.DEVELOPMENT_DATABASE_PASSWORD,
                ProductionCredentialsPolicy.DEVELOPMENT_DATABASE_PASSWORD))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to start")
                .hasMessageContaining("public");

        assertThatThrownBy(() -> ProductionCredentialsPolicy.requireIfPresent(
                "application.storage.access-key", "APPLICATION_STORAGE_ACCESS_KEY",
                ProductionCredentialsPolicy.DEVELOPMENT_STORAGE_KEY,
                ProductionCredentialsPolicy.DEVELOPMENT_STORAGE_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to start");
    }

    @Test
    void acceptsARealValue() {
        assertThatNoException().isThrownBy(() -> ProductionCredentialsPolicy.require(
                "application.database.password", "APPLICATION_DATABASE_PASSWORD",
                "a-real-database-password",
                ProductionCredentialsPolicy.DEVELOPMENT_DATABASE_PASSWORD));
    }

    /**
     * An empty storage key is how a deployment says "use the AWS default
     * credential chain", so it must not be treated as a misconfiguration.
     */
    @Test
    void allowsAnAbsentStorageKeySoIamRolesKeepWorking() {
        assertThatNoException().isThrownBy(() -> ProductionCredentialsPolicy.requireIfPresent(
                "application.storage.access-key", "APPLICATION_STORAGE_ACCESS_KEY", "",
                ProductionCredentialsPolicy.DEVELOPMENT_STORAGE_KEY));
        assertThatNoException().isThrownBy(() -> ProductionCredentialsPolicy.requireIfPresent(
                "application.storage.access-key", "APPLICATION_STORAGE_ACCESS_KEY", null,
                ProductionCredentialsPolicy.DEVELOPMENT_STORAGE_KEY));
    }

    /**
     * The constants have to keep matching application.properties, otherwise the
     * production guard stops recognising the values it exists to catch.
     */
    @Test
    void developmentConstantsMatchWhatIsShipped() throws Exception {
        String properties = new String(getClass().getClassLoader()
                .getResourceAsStream("application.properties").readAllBytes());
        assertThatNoException().isThrownBy(() -> {
            if (!properties.contains("application.database.password="
                    + ProductionCredentialsPolicy.DEVELOPMENT_DATABASE_PASSWORD)) {
                throw new AssertionError("application.properties no longer uses the expected development password");
            }
            if (!properties.contains(ProductionCredentialsPolicy.DEVELOPMENT_STORAGE_KEY)) {
                throw new AssertionError("application.properties no longer uses the expected development storage key");
            }
        });
    }
}
