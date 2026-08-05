package id.my.hendisantika.multitenancy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The title, description and version at the top of /v3/api-docs and the Swagger
 * UI, replacing springdoc's default "OpenAPI definition". The per-controller
 * names come from {@code @Tag}, the per-endpoint ones from {@code @Operation}.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI multitenancyOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Spring Boot JPA Multitenancy API")
                .description("""
                        A parent login that issues JWTs, and per-tenant data served from a database and \
                        subdomain provisioned for each organization at registration. The tenant is resolved \
                        from the host name, or from the X-Tenant header where wildcard DNS is not available.""")
                .version("0.0.1"));
    }
}
