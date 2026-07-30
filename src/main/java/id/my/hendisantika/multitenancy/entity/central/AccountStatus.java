package id.my.hendisantika.multitenancy.entity.central;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public enum AccountStatus {

    ACTIVE,

    /**
     * Kept for the audit trail but refused at login.
     */
    DISABLED
}
