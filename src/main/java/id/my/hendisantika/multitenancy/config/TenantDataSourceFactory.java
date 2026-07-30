package id.my.hendisantika.multitenancy.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds a connection pool for one database.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Component
@RequiredArgsConstructor
public class TenantDataSourceFactory {

    private final DatabaseProperties databaseProperties;

    public HikariDataSource create(String databaseName) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("hikari-" + databaseName);
        hikariConfig.setDriverClassName(databaseProperties.getDriverClassName());
        hikariConfig.setJdbcUrl(databaseProperties.urlFor(databaseName));
        hikariConfig.setUsername(databaseProperties.getUser());
        hikariConfig.setPassword(databaseProperties.getPassword());
        hikariConfig.setAutoCommit(Boolean.FALSE);
        // One pool per tenant adds up, so keep each one small and let idle
        // connections go back to the server.
        hikariConfig.setMaximumPoolSize(databaseProperties.getMaximumPoolSize());
        hikariConfig.setMinimumIdle(databaseProperties.getMinimumIdle());
        hikariConfig.setIdleTimeout(databaseProperties.getIdleTimeout().toMillis());
        hikariConfig.addDataSourceProperty("dataSource.cachePrepStmts", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSize", 250);
        hikariConfig.addDataSourceProperty("dataSource.prepStmtCacheSqlLimit", 2048);
        hikariConfig.addDataSourceProperty("dataSource.useServerPrepStmts", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.useLocalSessionState", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.rewriteBatchedStatements", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.cacheResultSetMetadata", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.cacheServerConfiguration", Boolean.TRUE);
        hikariConfig.addDataSourceProperty("dataSource.maintainTimeStats", Boolean.FALSE);
        return new HikariDataSource(hikariConfig);
    }
}
