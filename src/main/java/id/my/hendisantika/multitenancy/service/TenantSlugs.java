package id.my.hendisantika.multitenancy.service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Turns an organization name into a slug that is safe to use both as a MySQL
 * database name and as a DNS label.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public final class TenantSlugs {

    /**
     * Must start with a letter so it is a legal DNS label and an unquoted MySQL
     * identifier, and stay short enough for MySQL's 64 character limit.
     */
    public static final Pattern VALID_SLUG = Pattern.compile("^[a-z][a-z0-9]{2,29}$");

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private TenantSlugs() {
    }

    /**
     * "Sehat Clinic 2" becomes "sehatclinic2".
     */
    public static String slugify(String name) {
        if (name == null) {
            return "";
        }
        return NON_ALPHANUMERIC.matcher(name.toLowerCase(Locale.ROOT).trim()).replaceAll("");
    }

    public static boolean isValid(String slug) {
        return slug != null && VALID_SLUG.matcher(slug).matches();
    }
}
