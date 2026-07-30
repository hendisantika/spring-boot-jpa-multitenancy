package id.my.hendisantika.multitenancy.config;

/**
 * Raised when a request resolves to a tenant that is not registered or not active.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public class UnknownTenantException extends RuntimeException {

    private static final long serialVersionUID = 4592301867432567890L;

    public UnknownTenantException(String slug) {
        super("No active tenant registered for '" + slug + "'");
    }
}
