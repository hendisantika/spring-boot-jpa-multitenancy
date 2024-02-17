package id.my.hendisantika.multitenancy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
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

    @Column(name = "tenant")
    @Convert(converter = Tenant.TypeConverter.class)
    private Tenant tenant;
}
