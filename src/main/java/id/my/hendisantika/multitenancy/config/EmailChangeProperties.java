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
 * Date: 03/08/26
 * Time: 08.29
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.email-change")
public class EmailChangeProperties {

    /**
     * As long as a signup confirmation, because it asks the same thing: go and
     * read a mailbox. The link alone changes nothing without the password that
     * was already given to get it issued.
     */
    private Duration ttl = Duration.ofHours(24);

    private String confirmBaseUrl = "http://localhost:3000/confirm-email";
}
