package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.Tenant;

import java.util.Objects;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:48
 * To change this template use File | Settings | File Templates.
 */
public class TenantContext {
    private static final ThreadLocal<Tenant> tenantHolder = new ThreadLocal<>();

    public static Tenant getTenant() {
        Tenant tenant = tenantHolder.get();
        return Objects.isNull(tenant) ? Tenant.DEFAULT : tenant;
    }

    public static void setTenant(Tenant tenant) {
        tenantHolder.set(tenant);
    }

    public static void clearTenant() {
        tenantHolder.remove();
    }
}
