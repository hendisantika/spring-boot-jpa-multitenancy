package id.my.hendisantika.multitenancy.entity.tenant;

import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
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

    /**
     * A {@code code} from the {@code GENDER} list in reference_data, not its
     * label: renaming a label should not rewrite anybody's record.
     */
    @Column(name = "gender", length = 40)
    private String gender;

    /** A {@code code} from {@code MARITAL_STATUS}. */
    @Column(name = "marital_status", length = 40)
    private String maritalStatus;

    /** A {@code code} from {@code BLOOD_TYPE}. */
    @Column(name = "blood_type", length = 40)
    private String bloodType;

    /** A {@code code} from {@code IDENTITY_DOCUMENT}, saying what the number is. */
    @Column(name = "identity_document_type", length = 40)
    private String identityDocumentType;

    /** The number on that document: a KTP, a passport or a KITAS. */
    @Column(name = "identity_number")
    private String identityNumber;

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
