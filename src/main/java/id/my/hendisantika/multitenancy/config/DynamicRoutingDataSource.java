package id.my.hendisantika.multitenancy.config;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes to the pool of the tenant in {@link TenantContext}, and unlike the
 * previous enum-driven version accepts new tenants after startup.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<Object, Object> tenantDataSources = new ConcurrentHashMap<>();

    public DynamicRoutingDataSource(DataSource centralDataSource) {
        setDefaultTargetDataSource(centralDataSource);
        setTargetDataSources(new HashMap<>(tenantDataSources));
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        // Null falls back to the default target, which is the central database.
        return TenantContext.getTenant();
    }

    /**
     * Publishes a newly provisioned tenant so that subsequent requests can route
     * to it without a restart.
     */
    public synchronized void register(String slug, DataSource dataSource) {
        tenantDataSources.put(slug, dataSource);
        setTargetDataSources(new HashMap<>(tenantDataSources));
        // Rebuilds the resolved map; the field itself is volatile inside
        // AbstractRoutingDataSource, so in-flight lookups see either map.
        afterPropertiesSet();
    }

    public boolean isRegistered(String slug) {
        return tenantDataSources.containsKey(slug);
    }
}
