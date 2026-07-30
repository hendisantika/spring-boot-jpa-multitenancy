package id.my.hendisantika.multitenancy.entity.central;

/**
 * What an account may do inside one tenant.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public enum TenantRole {

    /**
     * Registered the organization; may create users inside it.
     */
    OWNER,

    /**
     * Created by the owner.
     */
    MEMBER
}
