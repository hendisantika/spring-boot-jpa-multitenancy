package id.my.hendisantika.multitenancy.service;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public class AuthenticationFailedException extends RuntimeException {

    private static final long serialVersionUID = 8123094571236789012L;

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
