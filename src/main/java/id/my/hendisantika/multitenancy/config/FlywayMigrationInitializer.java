package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.Tenant;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;

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
public class FlywayMigrationInitializer {
    private static final String DEFAULT_LOCATION = "db/migration/default";
    private static final String TENANT_LOCATION = "db/migration/tenants";
    private static final String DB_PREFIX = "db_";

    @Autowired
    private RoutingDataSource routingDataSource;

    public void migrate() {
        for (Tenant tenant : Tenant.values()) {
            String dbName = DB_PREFIX + tenant.getName();
            String scriptLocation = Tenant.DEFAULT.equals(tenant) ? DEFAULT_LOCATION : TENANT_LOCATION;

            Flyway flyway = Flyway.configure()
                    .locations(scriptLocation)
                    .baselineOnMigrate(Boolean.TRUE)
                    .dataSource(routingDataSource.getDataSourceByTenant(tenant))
                    .schemas(dbName)
                    .load();

            flyway.migrate();
        }
    }
}
