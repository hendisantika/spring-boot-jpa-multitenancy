package id.my.hendisantika.multitenancy.service;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.05
 */
public class PasswordResetException extends RuntimeException {

    private static final long serialVersionUID = 5610293847561029384L;

    public PasswordResetException(String message) {
        super(message);
    }
}
