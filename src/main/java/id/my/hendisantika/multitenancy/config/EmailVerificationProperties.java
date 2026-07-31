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
 * Time: 10.18
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.email-verification")
public class EmailVerificationProperties {

    private Duration ttl = Duration.ofHours(24);

    private String verifyBaseUrl = "http://localhost:3000/verify-email";
}
