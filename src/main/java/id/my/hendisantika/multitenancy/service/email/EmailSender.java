package id.my.hendisantika.multitenancy.service.email;

/**
 * Sends transactional email. Implementations must not throw for a delivery
 * failure: an invitation that was created should not be lost because the mail
 * did not go out.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.55
 */
public interface EmailSender {

    /**
     * @return whether the message was accepted for delivery
     */
    boolean send(EmailMessage message);

    /**
     * @return whether delivery is configured at all; when it is not, callers fall
     * back to handing the recipient's link to whoever asked for it
     */
    boolean isEnabled();

    record EmailMessage(String to, String subject, String html, String text) {
    }
}
