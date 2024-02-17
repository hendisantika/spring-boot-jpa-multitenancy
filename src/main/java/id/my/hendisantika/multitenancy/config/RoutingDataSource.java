package id.my.hendisantika.multitenancy.config;

import com.zaxxer.hikari.HikariDataSource;
import id.my.hendisantika.multitenancy.entity.Tenant;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:47
 * To change this template use File | Settings | File Templates.
 */
public class RoutingDataSource extends AbstractRoutingDataSource {
    private static final Map<Object, Object> dataSourceMap = new HashMap<>();

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenant();
    }

    void initDataSources(DatabaseConfiguration configuration) {
        for (Tenant tenant : Tenant.values()) {
            dataSourceMap.put(tenant, new HikariDataSource(hikariConfig(tenant, configuration)));
        }
        setDefaultTargetDataSource(getDefaultDataSource());
        setTargetDataSources(dataSourceMap);
    }
}
