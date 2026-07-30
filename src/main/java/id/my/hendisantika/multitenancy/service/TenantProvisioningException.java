package id.my.hendisantika.multitenancy.service;

/**
 * Raised when a tenant cannot be provisioned, for example because the name is
 * invalid, reserved or already taken.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public class TenantProvisioningException extends RuntimeException {

    private static final long serialVersionUID = -3390274319853121847L;

    public TenantProvisioningException(String message) {
        super(message);
    }

    public TenantProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
