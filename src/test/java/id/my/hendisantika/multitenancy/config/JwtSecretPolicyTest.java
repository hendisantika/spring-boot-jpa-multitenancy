package id.my.hendisantika.multitenancy.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.55
 */
class JwtSecretPolicyTest {

    private static final String REAL_SECRET = "P4wYQ0m2Zt7xK9vB1nR6sL3jH8dF5gA0cE2uI4oT7yW=";

    @Test
    void refusesTheDevelopmentSecretInProduction() {
        assertThatThrownBy(() ->
                JwtSecretPolicy.signingKey(JwtSecretPolicy.DEVELOPMENT_SECRET, true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to start")
                .hasMessageContaining("APPLICATION_JWT_SECRET");
    }

    @Test
    void allowsTheDevelopmentSecretOutsideProduction() {
        assertThatNoException().isThrownBy(() ->
                JwtSecretPolicy.signingKey(JwtSecretPolicy.DEVELOPMENT_SECRET, false));
    }

    @Test
    void acceptsARealSecretInProduction() {
        assertThat(JwtSecretPolicy.signingKey(REAL_SECRET, true).getAlgorithm()).isEqualTo("HmacSHA256");
    }

    @Test
    void rejectsAMissingSecret() {
        assertThatThrownBy(() -> JwtSecretPolicy.signingKey(null, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not set");
        assertThatThrownBy(() -> JwtSecretPolicy.signingKey("   ", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not set");
    }

    /**
     * An unset ${VAR} with no default is handed over as the literal placeholder,
     * so a long enough variable name would otherwise become the signing key.
     */
    @Test
    void rejectsAnUnresolvedPlaceholder() {
        assertThatThrownBy(() -> JwtSecretPolicy.signingKey("${APPLICATION_JWT_SECRET}", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unresolved placeholder");

        // Long enough to slip past the length check on its own.
        assertThatThrownBy(() ->
                JwtSecretPolicy.signingKey("${APPLICATION_JWT_SIGNING_SECRET_VALUE_HERE}", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unresolved placeholder");
    }

    @Test
    void rejectsASecretTooShortForHs256() {
        assertThatThrownBy(() -> JwtSecretPolicy.signingKey("too-short-for-hs256", true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    /**
     * The constant has to keep matching application.properties, otherwise the
     * production guard silently stops recognising the value it is meant to catch.
     */
    @Test
    void developmentSecretConstantMatchesTheOneShippedInApplicationProperties() throws Exception {
        String properties = new String(getClass().getClassLoader()
                .getResourceAsStream("application.properties").readAllBytes());
        assertThat(properties).contains("APPLICATION_JWT_SECRET:" + JwtSecretPolicy.DEVELOPMENT_SECRET);
    }
}
