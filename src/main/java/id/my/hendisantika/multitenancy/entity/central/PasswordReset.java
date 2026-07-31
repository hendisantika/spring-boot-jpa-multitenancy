package id.my.hendisantika.multitenancy.entity.central;

import id.my.hendisantika.multitenancy.entity.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * A password reset request. Same shape as an invitation: only the hash of the
 * token is stored, it is single use, and it expires.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.05
 */
@Data
@Entity
@Table(name = "password_resets")
@EqualsAndHashCode(callSuper = false)
public class PasswordReset extends BaseEntity {

    private static final long serialVersionUID = 8471029385712049586L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    /**
     * SHA-256 of the token. A leaked database must not let anyone take over an
     * account.
     */
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private String tokenHash;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;

    /**
     * Set the moment it is spent, which is what makes it single use.
     */
    @Column(name = "used_at")
    private Instant usedAt;

    public boolean isUsable() {
        return usedAt == null && expiresAt != null && Instant.now().isBefore(expiresAt);
    }
}
