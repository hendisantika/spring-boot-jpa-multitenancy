package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.service.email.BrevoEmailSender;
import id.my.hendisantika.multitenancy.service.email.DisabledEmailSender;
import id.my.hendisantika.multitenancy.service.email.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Wires email delivery, or deliberately leaves it off.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.55
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(BrevoProperties.class)
public class EmailConfiguration {

    /**
     * No api key means no delivery rather than a broken client: invitations then
     * fall back to the owner passing the link on, which is a supported way to
     * run this.
     */
    @Bean
    public EmailSender emailSender(BrevoProperties brevoProperties) {
        if (!brevoProperties.isConfigured()) {
            log.info("No Brevo api key configured; invitation links are returned to the owner instead "
                    + "of emailed. Set BREVO_API_KEY to send them.");
            return new DisabledEmailSender();
        }
        return new BrevoEmailSender(brevoRestClient(brevoProperties), brevoProperties);
    }

    private RestClient brevoRestClient(BrevoProperties brevoProperties) {
        // Timeouts matter here: an unreachable mail API must not hold an
        // invitation request open.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) brevoProperties.getTimeout().toMillis());
        requestFactory.setReadTimeout((int) brevoProperties.getTimeout().toMillis());

        return RestClient.builder()
                .baseUrl(brevoProperties.getBaseUrl())
                // Brevo authenticates with its own header rather than Authorization.
                .defaultHeader("api-key", brevoProperties.getApiKey())
                .defaultHeader("accept", "application/json")
                .requestFactory(requestFactory)
                .build();
    }
}
