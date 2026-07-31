package id.my.hendisantika.multitenancy.config;

import java.util.Arrays;
import java.util.List;

/**
 * Refuses to start production with credentials that are published in this
 * repository, or that were never configured at all.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 07.04
 */
public final class ProductionCredentialsPolicy {

    /**
     * Values committed to application.properties for local development.
     */
    public static final String DEVELOPMENT_DATABASE_PASSWORD = "root";
    public static final String DEVELOPMENT_STORAGE_KEY = "minioadmin";

    private ProductionCredentialsPolicy() {
    }

    /**
     * @param property        the property the value came from, for the message
     * @param variable        the environment variable that should supply it
     * @param value           the configured value
     * @param developmentValues values that mean "still the committed default"
     */
    public static void require(String property, String variable, String value, String... developmentValues) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    property + " is not set. In production supply it through the " + variable
                            + " environment variable.");
        }
        rejectPlaceholder(property, value);
        rejectDevelopmentValue(property, variable, value, developmentValues);
    }

    /**
     * For values that may legitimately be absent, such as the storage keys: an
     * empty access key means the AWS default credential chain, which is how an
     * instance using an IAM role is configured.
     */
    public static void requireIfPresent(String property, String variable, String value,
                                        String... developmentValues) {
        if (value == null || value.isBlank()) {
            return;
        }
        rejectPlaceholder(property, value);
        rejectDevelopmentValue(property, variable, value, developmentValues);
    }

    /**
     * An unset ${VAR} without a default arrives as the literal placeholder rather
     * than as an error, so it has to be caught by hand.
     */
    private static void rejectPlaceholder(String property, String value) {
        if (value.startsWith("${") && value.endsWith("}")) {
            throw new IllegalStateException(
                    property + " is still the unresolved placeholder " + value
                            + ". Set that environment variable.");
        }
    }

    private static void rejectDevelopmentValue(String property, String variable, String value,
                                               String... developmentValues) {
        List<String> development = Arrays.asList(developmentValues);
        if (development.contains(value)) {
            throw new IllegalStateException(
                    "Refusing to start: the production profile is active but " + property
                            + " is still the development value committed to this repository, so it is public. "
                            + "Set " + variable + " to a private value.");
        }
    }
}
