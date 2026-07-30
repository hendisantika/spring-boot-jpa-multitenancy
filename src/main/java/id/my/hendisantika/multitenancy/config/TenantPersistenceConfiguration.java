package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
import id.my.hendisantika.multitenancy.entity.tenant.Organization;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * The tenant aware persistence unit. Hibernate picks the connection per session
 * from the tenant in {@link TenantContext}, so the business repositories stay
 * completely unaware of tenants.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "id.my.hendisantika.multitenancy.repository.tenant",
        entityManagerFactoryRef = "tenantEntityManagerFactory",
        transactionManagerRef = "tenantTransactionManager"
)
public class TenantPersistenceConfiguration {

    @Bean
    public DynamicRoutingDataSource routingDataSource(DataSource centralDataSource) {
        return new DynamicRoutingDataSource(centralDataSource);
    }

    @Bean
    public TenantIdentifierResolver tenantIdentifierResolver(TenantProperties tenantProperties) {
        return new TenantIdentifierResolver(tenantProperties.getDefaultSlug());
    }

    @Bean
    public MultiTenantConnectionProvider<String> multiTenantConnectionProvider(
            TenantDataSourceRegistry tenantDataSourceRegistry) {
        return new MultitenantConnectionProvider(tenantDataSourceRegistry);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
            DataSource centralDataSource,
            MultiTenantConnectionProvider<String> multiTenantConnectionProvider,
            TenantIdentifierResolver tenantIdentifierResolver) {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPersistenceUnitName("tenant");
        factoryBean.setPackagesToScan(
                Organization.class.getPackageName(),
                BaseEntity.class.getPackageName()
        );
        // Only used to bootstrap metadata; real connections come from the
        // connection provider below.
        factoryBean.setDataSource(centralDataSource);
        factoryBean.setJpaVendorAdapter(HibernateSettings.vendorAdapter());
        factoryBean.setJpaProperties(tenantProperties(multiTenantConnectionProvider, tenantIdentifierResolver));
        factoryBean.setJpaDialect(new HibernateJpaDialect());
        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager tenantTransactionManager(
            @Qualifier("tenantEntityManagerFactory") EntityManagerFactory tenantEntityManagerFactory) {
        return new JpaTransactionManager(tenantEntityManagerFactory);
    }

    private Properties tenantProperties(MultiTenantConnectionProvider<String> multiTenantConnectionProvider,
                                        TenantIdentifierResolver tenantIdentifierResolver) {
        Properties properties = HibernateSettings.baseProperties();
        // Since Hibernate 6 the DATABASE strategy is implied by supplying a
        // MultiTenantConnectionProvider; the hibernate.multiTenancy setting is gone.
        properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        return properties;
    }
}
