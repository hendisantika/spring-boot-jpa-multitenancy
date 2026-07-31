package id.my.hendisantika.multitenancy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.05
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.password-reset")
public class PasswordResetProperties {

    /**
     * Short on purpose: a reset link is a way into the account, so it should not
     * sit in a mailbox for days.
     */
    private Duration ttl = Duration.ofHours(1);

    /**
     * Where the reset link points. The token is appended to it.
     */
    private String resetBaseUrl = "http://localhost:3000/reset-password";
}
