package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves a tenant slug to a live pool, creating the pool on first use so that a
 * server with many tenants does not open every pool at startup.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Slf4j
@Component
public class TenantDataSourceRegistry {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final TenantDataSourceFactory tenantDataSourceFactory;
    private final DynamicRoutingDataSource routingDataSource;
    private final DataSource centralDataSource;
    private final String defaultSlug;

    private final Map<String, DataSource> pools = new ConcurrentHashMap<>();

    public TenantDataSourceRegistry(TenantRegistrationRepository tenantRegistrationRepository,
                                    TenantDataSourceFactory tenantDataSourceFactory,
                                    DynamicRoutingDataSource routingDataSource,
                                    DataSource centralDataSource,
                                    TenantProperties tenantProperties) {
        this.tenantRegistrationRepository = tenantRegistrationRepository;
        this.tenantDataSourceFactory = tenantDataSourceFactory;
        this.routingDataSource = routingDataSource;
        this.centralDataSource = centralDataSource;
        this.defaultSlug = tenantProperties.getDefaultSlug();
    }

    /**
     * @return the pool for {@code slug}, or the central pool when the slug is the
     * default or unknown to the registry
     */
    public DataSource resolve(String slug) {
        if (slug == null || defaultSlug.equals(slug)) {
            return centralDataSource;
        }
        DataSource existing = pools.get(slug);
        if (existing != null) {
            return existing;
        }
        return tenantRegistrationRepository.findBySlug(slug)
                .filter(TenantRegistration::isActive)
                .map(this::open)
                .orElseThrow(() -> new UnknownTenantException(slug));
    }

    /**
     * Opens and publishes the pool for a tenant. Safe to call repeatedly: the
     * first caller wins and later callers get the same pool.
     */
    public synchronized DataSource open(TenantRegistration tenant) {
        return pools.computeIfAbsent(tenant.getSlug(), slug -> {
            log.info("Opening connection pool for tenant {} on database {}", slug, tenant.getDatabaseName());
            DataSource dataSource = tenantDataSourceFactory.create(tenant.getDatabaseName());
            routingDataSource.register(slug, dataSource);
            return dataSource;
        });
    }

    public Optional<DataSource> lookup(String slug) {
        return Optional.ofNullable(pools.get(slug));
    }

    public DataSource getCentralDataSource() {
        return centralDataSource;
    }
}
