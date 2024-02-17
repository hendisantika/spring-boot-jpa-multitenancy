package id.my.hendisantika.multitenancy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:35
 * To change this template use File | Settings | File Templates.
 */
@Data
@Entity
@Table(name = "persons")
@EqualsAndHashCode(callSuper = false)
public class Person extends BaseEntity {
    private static final long serialVersionUID = -4277100454311602070L;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "social_security_number")
    private String socialSecurityNumber;

    @Column(name = "home_phone")
    private String homePhone;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "birth_date")
    @Temporal(TemporalType.DATE)
    private Date birthDate;

    @Column(name = "email")
    private String email;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
