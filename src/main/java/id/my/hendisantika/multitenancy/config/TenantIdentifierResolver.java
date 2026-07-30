package id.my.hendisantika.multitenancy.config;

import lombok.RequiredArgsConstructor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:49
 * To change this template use File | Settings | File Templates.
 */
@RequiredArgsConstructor
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    private final String defaultSlug;

    @Override
    public String resolveCurrentTenantIdentifier() {
        // Hibernate requires a non-null identifier, so requests without a tenant
        // resolve to the default, which maps to the central database.
        String tenant = TenantContext.getTenant();
        return tenant != null ? tenant : defaultSlug;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public boolean isRoot(String tenantIdentifier) {
        return defaultSlug.equals(tenantIdentifier);
    }
}
