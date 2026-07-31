package id.my.hendisantika.multitenancy.entity.central;

import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * A person who signs up on the parent domain. An account is global: it logs in
 * once and, through its memberships, reaches every tenant it belongs to.
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
@Table(name = "accounts")
@EqualsAndHashCode(callSuper = false)
public class Account extends BaseEntity {

    private static final long serialVersionUID = 5893012745023189347L;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    /**
     * BCrypt hash. The plain password never leaves the signup request.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * When the password last changed. A refresh token issued before this is
     * refused, so a reset disowns sessions handed out earlier.
     */
    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    /**
     * Object key of the profile photo in the storage bucket, not a URL, so the
     * bucket or endpoint can change without rewriting rows.
     */
    @Column(name = "photo_key")
    private String photoKey;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public boolean isActive() {
        return AccountStatus.ACTIVE.equals(status);
    }
}
