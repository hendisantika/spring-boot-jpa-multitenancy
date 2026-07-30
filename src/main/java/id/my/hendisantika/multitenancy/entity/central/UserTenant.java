package id.my.hendisantika.multitenancy.entity.central;

import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Grants an account access to a tenant. Lives in the central database so that the
 * parent login can decide which tenants a user may reach before any tenant
 * database is selected.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:38
 * To change this template use File | Settings | File Templates.
 */
@Data
@Entity
@Table(name = "user_tenants")
@EqualsAndHashCode(callSuper = false)
public class UserTenant extends BaseEntity {

    private static final long serialVersionUID = 1287583108972033641L;

    @Column(name = "user_name")
    private String userName;

    /**
     * Slug of the tenant this membership grants, matching {@link TenantRegistration#getSlug()}.
     */
    @Column(name = "tenant_slug")
    private String tenantSlug;
}
