package id.my.hendisantika.multitenancy.service;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.18
 */
public class EmailVerificationException extends RuntimeException {

    private static final long serialVersionUID = 9182736450918273645L;

    public EmailVerificationException(String message) {
        super(message);
    }
}
