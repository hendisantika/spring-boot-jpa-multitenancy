package id.my.hendisantika.multitenancy.entity.central;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.14
 */
public enum InvitationStatus {

    /**
     * Sent, still usable until it expires.
     */
    PENDING,

    /**
     * Used. Tokens are single use, so this one is spent.
     */
    ACCEPTED,

    /**
     * Withdrawn by the owner before it was used.
     */
    REVOKED
}
