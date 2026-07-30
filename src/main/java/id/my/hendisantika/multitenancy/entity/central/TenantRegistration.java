package id.my.hendisantika.multitenancy.entity.central;

import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * A provisioned tenant. Replaces the former hardcoded Tenant enum: tenants are
 * now rows, created at runtime when an owner registers an organization.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Data
@Entity
@Table(name = "tenants")
@EqualsAndHashCode(callSuper = false)
public class TenantRegistration extends BaseEntity {

    private static final long serialVersionUID = 7318570391625405431L;

    /**
     * DNS label and lookup key, e.g. "sehat".
     */
    @Column(name = "slug", nullable = false, unique = true, updatable = false)
    private String slug;

    /**
     * MySQL database backing this tenant, e.g. "sehat".
     */
    @Column(name = "database_name", nullable = false, unique = true, updatable = false)
    private String databaseName;

    /**
     * Fully qualified host, e.g. "sehat.jvm.my.id".
     */
    @Column(name = "subdomain", nullable = false, unique = true)
    private String subdomain;

    /**
     * Business name as the owner typed it; the slug is derived from it.
     */
    @Column(name = "display_name")
    private String displayName;

    @Column(name = "business_email")
    private String businessEmail;

    /**
     * Object key of the organization photo, not a URL.
     */
    @Column(name = "photo_key")
    private String photoKey;

    @Column(name = "contact_first_name")
    private String contactFirstName;

    @Column(name = "contact_last_name")
    private String contactLastName;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "org_structure")
    @Enumerated(EnumType.STRING)
    private OrgStructure orgStructure;

    @Column(name = "practice_speciality")
    @Enumerated(EnumType.STRING)
    private PracticeSpeciality practiceSpeciality;

    /**
     * The account that registered this organization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_account_id")
    private Account owner;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean isActive() {
        return TenantStatus.ACTIVE.equals(status);
    }
}
