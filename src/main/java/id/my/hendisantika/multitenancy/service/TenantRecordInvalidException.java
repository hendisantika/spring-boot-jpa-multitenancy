package id.my.hendisantika.multitenancy.service;

/**
 * A record inside a tenant's database was rejected for what it said rather than
 * for who asked. Answered as a 400, with a message a form can show.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 20.31
 */
public class TenantRecordInvalidException extends RuntimeException {

    public TenantRecordInvalidException(String message) {
        super(message);
    }
}
