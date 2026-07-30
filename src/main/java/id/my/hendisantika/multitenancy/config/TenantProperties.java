package id.my.hendisantika.multitenancy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Set;

/**
 * Tenant naming and resolution settings.
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
@ConfigurationProperties(prefix = "application.tenant")
public class TenantProperties {

    /**
     * Subdomains are built as {slug}.{baseDomain}.
     */
    private String baseDomain = "mhdc.co.id";

    /**
     * Slug of the central database, used when a request carries no tenant.
     */
    private String defaultSlug = "default";

    /**
     * Host names that never carry a tenant subdomain, so the default applies.
     */
    private Set<String> neutralHosts = Set.of("localhost", "127.0.0.1", "0.0.0.0", "[::1]");

    /**
     * Slugs that must never become a database name or a subdomain.
     */
    private List<String> reservedSlugs = List.of(
            "mysql", "sys", "information_schema", "performance_schema", "sakila", "world",
            "www", "api", "admin", "app", "mail", "ftp", "test", "staging", "default"
    );
}
