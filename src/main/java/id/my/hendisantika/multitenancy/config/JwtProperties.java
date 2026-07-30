package id.my.hendisantika.multitenancy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings for the tokens the parent login issues.
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
@ConfigurationProperties(prefix = "application.jwt")
public class JwtProperties {

    /**
     * HMAC signing key. Must be at least 32 bytes for HS256 and must be replaced
     * outside development.
     */
    private String secret;

    private String issuer = "mhdc.co.id";

    private Duration accessTokenTtl = Duration.ofMinutes(30);

    private Duration refreshTokenTtl = Duration.ofDays(14);
}
