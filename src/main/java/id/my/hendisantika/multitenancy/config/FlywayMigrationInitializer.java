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
    @Autowired
    private RoutingDataSource routingDataSource;

    public void migrate() {
        String scriptLocation = "db/migration";
        String dbPrefix = "db_";

        for (Tenant tenant : Tenant.values()) {
            String dbName = dbPrefix + tenant.getName();

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
