package id.my.hendisantika.multitenancy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Connection settings shared by the central database and every tenant database.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.database")
public class DatabaseProperties {

    /**
     * JDBC url with a {database} placeholder, substituted per database.
     */
    private String urlTemplate;

    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    private String user;

    private String password;

    /**
     * Database holding the tenant registry, accounts and memberships.
     */
    private String centralDatabase = "db_default";

    /**
     * Per-tenant pool sizing. Every tenant gets its own pool, so the server wide
     * connection count is roughly tenants x maximumPoolSize.
     */
    private int maximumPoolSize = 5;

    private int minimumIdle = 0;

    private Duration idleTimeout = Duration.ofMinutes(1);

    public String urlFor(String databaseName) {
        return urlTemplate.replace("{database}", databaseName);
    }
}
