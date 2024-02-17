package id.my.hendisantika.multitenancy.config;

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
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {
    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getTenant().getName();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
