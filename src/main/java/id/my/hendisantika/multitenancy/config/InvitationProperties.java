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
 * Time: 09.14
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.invitation")
public class InvitationProperties {

    /**
     * How long an invitation stays usable.
     */
    private Duration ttl = Duration.ofDays(7);

    /**
     * Where the accept link points. The token is appended to it.
     */
    private String acceptBaseUrl = "http://localhost:3000/invitations";
}
