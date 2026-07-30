package id.my.hendisantika.multitenancy.config;

/**
 * Holds the tenant slug resolved for the current thread.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:48
 * To change this template use File | Settings | File Templates.
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    /**
     * @return the current tenant slug, or {@code null} when the request carries no tenant
     */
    public static String getTenant() {
        return TENANT_HOLDER.get();
    }

    public static void setTenant(String tenant) {
        TENANT_HOLDER.set(tenant);
    }

    public static void clearTenant() {
        TENANT_HOLDER.remove();
    }
}
