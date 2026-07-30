package id.my.hendisantika.multitenancy.config;

import lombok.RequiredArgsConstructor;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;

import javax.sql.DataSource;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:44
 * To change this template use File | Settings | File Templates.
 */
@RequiredArgsConstructor
public class MultitenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl<String> {

    private static final long serialVersionUID = -8892302751438927341L;

    private final transient TenantDataSourceRegistry tenantDataSourceRegistry;

    @Override
    protected DataSource selectAnyDataSource() {
        // Used for metadata and for sessions opened without a tenant.
        return tenantDataSourceRegistry.getCentralDataSource();
    }

    @Override
    protected DataSource selectDataSource(String tenantIdentifier) {
        return tenantDataSourceRegistry.resolve(tenantIdentifier);
    }
}
