package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantStatus;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Brings every registered tenant database up to the latest migration on startup,
 * so a deployment that adds a migration reaches all tenants.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantMigrationRunner implements ApplicationRunner {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final TenantDataSourceRegistry tenantDataSourceRegistry;

    @Override
    public void run(ApplicationArguments args) {
        List<TenantRegistration> tenants = tenantRegistrationRepository.findAllByStatus(TenantStatus.ACTIVE);
        log.info("Migrating {} registered tenant database(s)", tenants.size());
        for (TenantRegistration tenant : tenants) {
            Flyway.configure()
                    .locations(HibernateSettings.TENANT_MIGRATION_LOCATION)
                    .baselineOnMigrate(Boolean.TRUE)
                    .dataSource(tenantDataSourceRegistry.open(tenant))
                    .schemas(tenant.getDatabaseName())
                    .load()
                    .migrate();
        }
    }
}
