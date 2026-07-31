package id.my.hendisantika.multitenancy.service.email;

import lombok.extern.slf4j.Slf4j;

/**
 * Used when no Brevo api key is configured. Reports that it is not enabled, so
 * callers hand the link to whoever asked for it instead of assuming it was
 * delivered.
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
public class DisabledEmailSender implements EmailSender {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean send(EmailMessage message) {
        log.info("Email delivery is not configured; not sending \"{}\" to {}",
                message.subject(), message.to());
        return false;
    }
}
