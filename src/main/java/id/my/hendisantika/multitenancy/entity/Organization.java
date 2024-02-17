package id.my.hendisantika.multitenancy.entity;

import jakarta.persistence.Column;
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
 * Time: 13:34
 * To change this template use File | Settings | File Templates.
 */
@Data
@Entity
@Table(name = "organizations")
@EqualsAndHashCode(callSuper = false)
public class Organization extends BaseEntity {
    private static final long serialVersionUID = -6144389355317857388L;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "email")
    private String email;
}
