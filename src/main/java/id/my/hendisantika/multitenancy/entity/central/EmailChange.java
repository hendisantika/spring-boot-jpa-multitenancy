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
 * An address somebody has asked to move to, waiting to be proved reachable.
 * <p>
 * It is held here rather than on the account because the change only takes
 * effect once the link is opened: the email is what you sign in with, so
 * applying it first would mean a typo locks you out of your own account.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 03/08/26
 * Time: 08.29
 */
@Data
@Entity
@Table(name = "email_changes")
@EqualsAndHashCode(callSuper = false)
public class EmailChange extends BaseEntity {

    private static final long serialVersionUID = 5748291036475829103L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    /**
     * Where the link was sent, and the address the account takes on once it is
     * opened.
     */
    @Column(name = "new_email", nullable = false, updatable = false)
    private String newEmail;

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
