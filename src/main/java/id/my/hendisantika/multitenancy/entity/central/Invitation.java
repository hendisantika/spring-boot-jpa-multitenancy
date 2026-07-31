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
 * An invitation to join a tenant. The owner creates one, the recipient accepts
 * it and chooses their own password, so nobody else ever knows it.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.14
 */
@Data
@Entity
@Table(name = "invitations")
@EqualsAndHashCode(callSuper = false)
public class Invitation extends BaseEntity {

    private static final long serialVersionUID = 3920184756203918475L;

    @Column(name = "tenant_slug", nullable = false, updatable = false)
    private String tenantSlug;

    @Column(name = "email", nullable = false, updatable = false)
    private String email;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private TenantRole role = TenantRole.MEMBER;

    /**
     * SHA-256 of the token, never the token itself: a leaked database must not
     * hand out working invitations.
     */
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false)
    private String tokenHash;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private InvitationStatus status = InvitationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_account_id")
    private Account invitedBy;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", updatable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * @return whether the token can still be exchanged for a membership
     */
    public boolean isUsable() {
        return InvitationStatus.PENDING.equals(status) && !isExpired();
    }
}
