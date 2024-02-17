package id.my.hendisantika.multitenancy.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import id.my.hendisantika.multitenancy.entity.Tenant;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
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

    DataSource getDataSourceByTenant(Tenant tenant) {
        return (DataSource) dataSourceMap.get(tenant);
    }

    DataSource getDefaultDataSource() {
        return getDataSourceByTenant(Tenant.DEFAULT);
    }

    private HikariConfig hikariConfig(Tenant tenant,
                                      DatabaseConfiguration configuration) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl(configuration.getUrl().replace("tenantName", tenant.getName()));
        hikariConfig.setUsername(configuration.getUser());
        hikariConfig.setPassword(configuration.getPassword());
        hikariConfig.setAutoCommit(Boolean.FALSE);
        hikariConfig.addDataSourceProperty("dataSource.cachePrepStmts", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSize", 250);
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", 2048);
        hikariConfig.addDataSourceProperty("dataSource.useServerPrepStmts", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.useLocalSessionState", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.rewriteBatchedStatements", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.cacheResultSetMetadata", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.cacheServerConfiguration", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.maintainTimeStats", Boolean.FALSE);
        return hikariConfig;
    }
}
