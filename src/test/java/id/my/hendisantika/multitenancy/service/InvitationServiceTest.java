package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.InvitationProperties;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.InvitationStatus;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.InvitationRepository;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.14
 */
@SpringBootTest
class InvitationServiceTest {

    private static final String OWNER_EMAIL = "invite.owner@example.test";
    private static final String INVITEE_EMAIL = "invite.guest@example.test";
    private static final String ORGANIZATION = "Invite Probe Clinic";
    private static final String SLUG = "inviteprobeclinic";

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private TenantProvisioningService tenantProvisioningService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private InvitationProperties invitationProperties;

    @Autowired
    private DataSource centralDataSource;

    private Account owner;

    @BeforeEach
    void setUp() {
        owner = accountRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseGet(() -> {
            Account account = new Account();
            account.setEmail(OWNER_EMAIL);
            account.setPassword(passwordEncoder.encode("owner-password"));
            account.setStatus(AccountStatus.ACTIVE);
            account.setCreatedAt(Instant.now());
            return accountRepository.save(account);
        });
        if (tenantRegistrationRepository.findBySlug(SLUG).isEmpty()) {
            tenantProvisioningService.provision(OrganizationProfile.ofName(ORGANIZATION), owner);
        }
    }

    @AfterEach
    void cleanUp() {
        // Invitations reference the tenant and accounts reference the memberships,
        // so unwind in that order before the tenant itself goes.
        invitationRepository.deleteAll(invitationRepository.findAllByTenantSlug(SLUG));
        if (tenantRegistrationRepository.findBySlug(SLUG).isPresent()) {
            tenantProvisioningService.deprovision(SLUG);
        }
        List.of(OWNER_EMAIL, INVITEE_EMAIL, "someone.else@example.test").forEach(email ->
                accountRepository.findByEmailIgnoreCase(email).ifPresent(account -> {
                    userTenantRepository.deleteAll(userTenantRepository.findAllByAccountId(account.getId()));
                    accountRepository.delete(account);
                }));
    }

    /**
     * Reaches past the TTL rather than waiting it out. Straight SQL on purpose:
     * expires_at is mapped updatable = false, so an invitation's deadline cannot
     * be moved through the application. The commit is explicit because the pools
     * run with autoCommit disabled, so the update would otherwise be discarded
     * when the connection goes back.
     */
    private void expire(Long invitationId) {
        try (Connection connection = centralDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE invitations SET expires_at = ? WHERE id = ?")) {
            statement.setTimestamp(1, Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)));
            statement.setLong(2, invitationId);
            int updated = statement.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            // Guards against a silent no-op making the test look like it proved
            // something about expiry when it never reached the row.
            assertThat(updated).as("rows expired").isEqualTo(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not expire the invitation", e);
        }
    }

    @Test
    void acceptingCreatesTheAccountWithAPasswordTheOwnerNeverSees() {
        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);

        assertThat(created.acceptUrl()).endsWith(created.token());
        assertThat(accountRepository.existsByEmailIgnoreCase(INVITEE_EMAIL)).isFalse();

        Account invitee = invitationService.accept(created.token(), "chosen-by-the-invitee");

        assertThat(invitee.getEmail()).isEqualTo(INVITEE_EMAIL);
        assertThat(passwordEncoder.matches("chosen-by-the-invitee", invitee.getPassword())).isTrue();
        assertThat(userTenantRepository.existsByAccountIdAndTenantSlug(invitee.getId(), SLUG)).isTrue();
    }

    /**
     * The raw token is handed out once. Only its hash is kept, so a leaked
     * database yields no working invitations.
     */
    @Test
    void onlyAHashOfTheTokenIsStored() {
        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);

        assertThat(invitationRepository.findAllByTenantSlug(SLUG))
                .allSatisfy(invitation -> {
                    assertThat(invitation.getTokenHash()).doesNotContain(created.token());
                    assertThat(invitation.getTokenHash()).hasSize(64);
                });
    }

    @Test
    void tokensAreSingleUse() {
        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);
        invitationService.accept(created.token(), "chosen-by-the-invitee");

        assertThatThrownBy(() -> invitationService.accept(created.token(), "another-password"))
                .isInstanceOf(InvitationException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void anExpiredInvitationIsRefused() {
        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);

        expire(created.invitation().getId());

        assertThatThrownBy(() -> invitationService.preview(created.token()))
                .isInstanceOf(InvitationException.class);
        assertThat(accountRepository.existsByEmailIgnoreCase(INVITEE_EMAIL)).isFalse();
    }

    @Test
    void aRevokedInvitationIsRefused() {
        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);
        invitationService.revoke(SLUG, created.invitation().getId());

        assertThatThrownBy(() -> invitationService.accept(created.token(), "chosen-by-the-invitee"))
                .isInstanceOf(InvitationException.class);
    }

    /**
     * Re-inviting must not leave two usable links for the same person.
     */
    @Test
    void invitingAgainWithdrawsThePreviousLink() {
        InvitationService.CreatedInvitation first =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);
        InvitationService.CreatedInvitation second =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);

        assertThatThrownBy(() -> invitationService.accept(first.token(), "chosen-by-the-invitee"))
                .isInstanceOf(InvitationException.class);
        assertThat(invitationService.pendingFor(SLUG)).hasSize(1);

        Account invitee = invitationService.accept(second.token(), "chosen-by-the-invitee");
        assertThat(invitee.getEmail()).isEqualTo(INVITEE_EMAIL);
    }

    @Test
    void anExistingAccountKeepsItsPasswordAndJustGainsTheMembership() {
        Account existing = new Account();
        existing.setEmail(INVITEE_EMAIL);
        existing.setPassword(passwordEncoder.encode("already-my-password"));
        existing.setStatus(AccountStatus.ACTIVE);
        existing.setCreatedAt(Instant.now());
        accountRepository.save(existing);

        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);
        assertThat(invitationService.preview(created.token()).accountExists()).isTrue();

        Account accepted = invitationService.accept(created.token(), null);

        assertThat(passwordEncoder.matches("already-my-password", accepted.getPassword())).isTrue();
        assertThat(userTenantRepository.existsByAccountIdAndTenantSlug(accepted.getId(), SLUG)).isTrue();
    }

    @Test
    void invitingSomeoneWhoIsAlreadyAMemberIsRefused() {
        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);
        invitationService.accept(created.token(), "chosen-by-the-invitee");

        assertThatThrownBy(() -> invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner))
                .isInstanceOf(AccountAlreadyExistsException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void aNewAccountNeedsAPasswordLongEnough() {
        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);

        assertThatThrownBy(() -> invitationService.accept(created.token(), "short"))
                .isInstanceOf(InvitationException.class)
                .hasMessageContaining("at least 8");
        // The token survives a rejected attempt, so they can try again.
        assertThat(invitationService.preview(created.token()).email()).isEqualTo(INVITEE_EMAIL);
    }

    @Test
    void previewShowsTheOrganizationBeforeAnyoneCommits() {
        InvitationService.CreatedInvitation created =
                invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.OWNER, owner);

        InvitationService.InvitationPreview preview = invitationService.preview(created.token());

        assertThat(preview.organizationName()).isEqualTo(ORGANIZATION);
        assertThat(preview.tenantSlug()).isEqualTo(SLUG);
        assertThat(preview.role()).isEqualTo(TenantRole.OWNER);
        assertThat(preview.accountExists()).isFalse();
        assertThat(preview.expiresAt()).isAfter(Instant.now());
        assertThat(preview.expiresAt()).isBefore(Instant.now().plus(invitationProperties.getTtl()).plusSeconds(60));
    }

    @Test
    void aTokenThatNeverExistedIsRefusedWithTheSameMessage() {
        assertThatThrownBy(() -> invitationService.preview("not-a-real-token"))
                .isInstanceOf(InvitationException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void pendingListsOnlyWhatIsStillUsable() {
        invitationService.invite(SLUG, INVITEE_EMAIL, TenantRole.MEMBER, owner);
        InvitationService.CreatedInvitation other =
                invitationService.invite(SLUG, "someone.else@example.test", TenantRole.MEMBER, owner);
        assertThat(invitationService.pendingFor(SLUG)).hasSize(2);

        invitationService.revoke(SLUG, other.invitation().getId());
        assertThat(invitationService.pendingFor(SLUG))
                .extracting(invitation -> invitation.getStatus())
                .containsOnly(InvitationStatus.PENDING);
        assertThat(invitationService.pendingFor(SLUG)).hasSize(1);
    }
}
