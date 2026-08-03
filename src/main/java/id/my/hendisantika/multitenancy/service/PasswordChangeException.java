package id.my.hendisantika.multitenancy.service;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 03/08/26
 * Time: 09.42
 */
public class PasswordChangeException extends RuntimeException {

    private static final long serialVersionUID = 6172839405617283940L;

    public PasswordChangeException(String message) {
        super(message);
    }
}
