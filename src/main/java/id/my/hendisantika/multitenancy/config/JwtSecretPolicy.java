package id.my.hendisantika.multitenancy.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Decides whether a signing secret is fit to use, and refuses to start rather
 * than sign production tokens with a value that is public knowledge.
 * <p>
 * Anyone holding the secret can mint a token for any account, so the
 * development value committed to this repository must never reach production.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.55
 */
public final class JwtSecretPolicy {

    /**
     * The value shipped in application.properties for local development. It is in
     * version control, so it is not a secret at all.
     */
    public static final String DEVELOPMENT_SECRET = "change-this-development-only-secret-please-32b";

    /**
     * HS256 needs at least 256 bits of key material.
     */
    public static final int MINIMUM_SECRET_BYTES = 32;

    private JwtSecretPolicy() {
    }

    /**
     * @param secret     the configured signing secret
     * @param production whether this instance is running as production
     * @return the signing key
     * @throws IllegalStateException if the secret is missing, too short, or is the
     *                               development value while running as production
     */
    public static SecretKey signingKey(String secret, boolean production) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "application.jwt.secret is not set. In production supply it through the "
                            + "APPLICATION_JWT_SECRET environment variable.");
        }
        // An unset ${VAR} with no default arrives as the literal placeholder rather
        // than as an error. Left alone, a placeholder whose name happens to be long
        // enough would quietly become the signing key.
        if (secret.startsWith("${") && secret.endsWith("}")) {
            throw new IllegalStateException(
                    "application.jwt.secret is still the unresolved placeholder " + secret
                            + ". Set that environment variable, for example: "
                            + "export APPLICATION_JWT_SECRET=$(openssl rand -base64 48)");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "application.jwt.secret must be at least " + MINIMUM_SECRET_BYTES
                            + " bytes long for HS256, but it is "
                            + secret.getBytes(StandardCharsets.UTF_8).length + ".");
        }
        if (production && DEVELOPMENT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "Refusing to start: the production profile is active but application.jwt.secret is still the "
                            + "development value from application.properties, which is public in version control. "
                            + "Set APPLICATION_JWT_SECRET to a private value, for example: "
                            + "openssl rand -base64 48");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public static boolean isDevelopmentSecret(String secret) {
        return DEVELOPMENT_SECRET.equals(secret);
    }
}
