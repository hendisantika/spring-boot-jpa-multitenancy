package id.my.hendisantika.multitenancy.service;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.14
 */
public class InvitationException extends RuntimeException {

    private static final long serialVersionUID = 7391028475019283746L;

    public InvitationException(String message) {
        super(message);
    }
}
