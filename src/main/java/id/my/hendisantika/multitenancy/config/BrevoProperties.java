package id.my.hendisantika.multitenancy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Brevo transactional email. Leaving the api key empty disables delivery, and
 * invitations fall back to the owner passing the link on.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.55
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.brevo")
public class BrevoProperties {

    /**
     * Never commit this. Supply it through BREVO_API_KEY.
     */
    private String apiKey;

    private String baseUrl = "https://api.brevo.com/v3";

    /**
     * Must be a sender Brevo has verified for the account, or it refuses to send.
     */
    private String senderEmail = "no-reply@jvm.my.id";

    private String senderName = "Multitenancy";

    private Duration timeout = Duration.ofSeconds(10);

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }
}
