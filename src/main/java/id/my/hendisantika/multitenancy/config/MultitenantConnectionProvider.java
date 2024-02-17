package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.Tenant;
import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;

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
public class MultitenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {
    @Autowired
    private RoutingDataSource routingDataSource;

    @Override
    protected DataSource selectAnyDataSource() {
        return routingDataSource.getDefaultDataSource();
    }

    @Override
    protected DataSource selectDataSource(String tenantName) {
        return routingDataSource.getDataSourceByTenant(Tenant.findByName(tenantName));
    }
}
