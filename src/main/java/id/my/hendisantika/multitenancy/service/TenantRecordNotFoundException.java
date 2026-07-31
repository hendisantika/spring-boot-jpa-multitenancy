package id.my.hendisantika.multitenancy.service;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 12.44
 */
public class TenantRecordNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 4172830495617283940L;

    public TenantRecordNotFoundException(String message) {
        super(message);
    }
}
