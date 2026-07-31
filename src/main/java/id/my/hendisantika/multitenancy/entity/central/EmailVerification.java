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
 * Proof that whoever signed up can read the address they used.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.18
 */
@Data
@Entity
@Table(name = "email_verifications")
@EqualsAndHashCode(callSuper = false)
public class EmailVerification extends BaseEntity {

    private static final long serialVersionUID = 2039485716203948571L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private String tokenHash;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public boolean isUsable() {
        return usedAt == null && expiresAt != null && Instant.now().isBefore(expiresAt);
    }
}
