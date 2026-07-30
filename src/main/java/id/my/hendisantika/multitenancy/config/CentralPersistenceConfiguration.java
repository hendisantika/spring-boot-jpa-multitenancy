package id.my.hendisantika.multitenancy.config;

import com.zaxxer.hikari.HikariDataSource;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * The central persistence unit. It is bound directly to the central database and
 * is deliberately not tenant aware: the tenant registry and the memberships that
 * decide which tenant a request may use have to be readable before any tenant is
 * known.
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
@EnableTransactionManagement
@EnableConfigurationProperties({DatabaseProperties.class, TenantProperties.class})
@EnableJpaRepositories(
        basePackages = "id.my.hendisantika.multitenancy.repository.central",
        entityManagerFactoryRef = "centralEntityManagerFactory",
        transactionManagerRef = "centralTransactionManager"
)
public class CentralPersistenceConfiguration {

    /**
     * Primary so that Boot's DataSource auto configuration backs off and so that
     * injection points without a qualifier get the central database.
     */
    @Bean
    @Primary
    public HikariDataSource centralDataSource(TenantDataSourceFactory tenantDataSourceFactory,
                                              DatabaseProperties databaseProperties) {
        return tenantDataSourceFactory.create(databaseProperties.getCentralDatabase());
    }

    /**
     * Migrates the central database before the entity manager touches it.
     */
    @Bean(initMethod = "migrate")
    public Flyway centralFlyway(DataSource centralDataSource, DatabaseProperties databaseProperties) {
        return Flyway.configure()
                .locations(HibernateSettings.CENTRAL_MIGRATION_LOCATION)
                .baselineOnMigrate(Boolean.TRUE)
                .dataSource(centralDataSource)
                .schemas(databaseProperties.getCentralDatabase())
                .load();
    }

    @Bean
    @Primary
    @DependsOn("centralFlyway")
    public LocalContainerEntityManagerFactoryBean centralEntityManagerFactory(DataSource centralDataSource) {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPersistenceUnitName("central");
        factoryBean.setPackagesToScan(
                TenantRegistration.class.getPackageName(),
                BaseEntity.class.getPackageName()
        );
        factoryBean.setDataSource(centralDataSource);
        factoryBean.setJpaVendorAdapter(HibernateSettings.vendorAdapter());
        factoryBean.setJpaProperties(HibernateSettings.baseProperties());
        factoryBean.setJpaDialect(new HibernateJpaDialect());
        return factoryBean;
    }

    @Bean
    @Primary
    public PlatformTransactionManager centralTransactionManager(
            @org.springframework.beans.factory.annotation.Qualifier("centralEntityManagerFactory")
            EntityManagerFactory centralEntityManagerFactory) {
        return new JpaTransactionManager(centralEntityManagerFactory);
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
