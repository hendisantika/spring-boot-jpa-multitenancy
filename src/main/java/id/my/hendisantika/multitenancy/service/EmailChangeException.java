package id.my.hendisantika.multitenancy.service;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 03/08/26
 * Time: 08.29
 */
public class EmailChangeException extends RuntimeException {

    private static final long serialVersionUID = 8273645019283746501L;

    public EmailChangeException(String message) {
        super(message);
    }
}
