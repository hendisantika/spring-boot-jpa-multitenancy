package id.my.hendisantika.multitenancy.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.Properties;

/**
 * Hibernate settings shared by the central and tenant persistence units.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
public final class HibernateSettings {

    public static final String CENTRAL_MIGRATION_LOCATION = "db/migration/default";
    public static final String TENANT_MIGRATION_LOCATION = "db/migration/tenants";

    private HibernateSettings() {
    }

    public static JpaVendorAdapter vendorAdapter() {
        HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
        jpaVendorAdapter.setDatabase(Database.MYSQL);
        return jpaVendorAdapter;
    }

    public static Properties baseProperties() {
        Properties properties = new Properties();
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
        properties.setProperty(AvailableSettings.DIALECT, MySQLDialect.class.getName());
        properties.setProperty(AvailableSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS, "true");
        properties.setProperty("hibernate.jpa.compliance.transaction", "true");
        properties.setProperty("hibernate.jpa.compliance.query", "true");
        properties.setProperty("hibernate.jpa.compliance.list", "true");
        properties.setProperty(AvailableSettings.JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE, "true");
        properties.setProperty(AvailableSettings.JPAQL_STRICT_COMPLIANCE, "true");
        properties.setProperty(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true");
        properties.setProperty(AvailableSettings.SHOW_SQL, "false");
        properties.setProperty(AvailableSettings.FORMAT_SQL, "false");
        properties.setProperty(AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, "true");
        properties.setProperty(AvailableSettings.MAX_FETCH_DEPTH, "4");
        properties.setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "16");
        properties.setProperty(AvailableSettings.ORDER_UPDATES, "true");
        return properties;
    }
}
