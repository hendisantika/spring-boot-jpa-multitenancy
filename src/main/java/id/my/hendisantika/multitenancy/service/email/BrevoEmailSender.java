package id.my.hendisantika.multitenancy.service.email;

import id.my.hendisantika.multitenancy.config.BrevoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends through Brevo's transactional email API.
 * <p>
 * Delivery failures are logged and reported, never thrown: an invitation that
 * was created should not be lost because the mail did not go out.
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
public class BrevoEmailSender implements EmailSender {

    private final RestClient restClient;
    private final BrevoProperties brevoProperties;

    public BrevoEmailSender(RestClient restClient, BrevoProperties brevoProperties) {
        this.restClient = restClient;
        this.brevoProperties = brevoProperties;
    }

    @Override
    public boolean isEnabled() {
        return brevoProperties.isConfigured();
    }

    @Override
    public boolean send(EmailMessage message) {
        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", brevoProperties.getSenderName(),
                        "email", brevoProperties.getSenderEmail()),
                "to", List.of(Map.of("email", message.to())),
                "subject", message.subject(),
                "htmlContent", message.html(),
                "textContent", message.text());

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent email to {}", message.to());
            return true;
        } catch (RestClientException e) {
            // The recipient address is logged, the api key never is.
            log.warn("Could not send email to {}: {}", message.to(), e.getMessage());
            return false;
        }
    }
}
