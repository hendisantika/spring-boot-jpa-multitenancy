package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.BaseEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
@Configuration
@EnableJpaRepositories(basePackages = {"id.my.hendisantika.multitenancy.repository"})
@EnableSpringConfigured
@EnableTransactionManagement
public class RepositoryConfiguration {
    @Bean
    public DatabaseConfiguration databaseConfiguration() {
        return new DatabaseConfiguration();
    }

    @Bean
    @DependsOn(value = "flywayMigrationInitializer")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setPackagesToScan(BaseEntity.class.getPackage().getName());
        entityManagerFactoryBean.setDataSource(routingDataSource().getDefaultDataSource());
        entityManagerFactoryBean.setJpaVendorAdapter(hibernateJpaVendorAdapter());
        entityManagerFactoryBean.setJpaProperties(hibernateProperties());
        entityManagerFactoryBean.setJpaDialect(new HibernateJpaDialect());
        return entityManagerFactoryBean;
    }
}