package id.my.hendisantika.multitenancy.entity.central;

/**
 * Lifecycle of a provisioned tenant.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
public enum TenantStatus {

    /**
     * Database created and migrated, requests are served.
     */
    ACTIVE,

    /**
     * Registered but the database is not ready yet.
     */
    PROVISIONING,

    /**
     * Database still exists, requests are refused.
     */
    SUSPENDED
}
