package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.InvitationProperties;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.Invitation;
import id.my.hendisantika.multitenancy.entity.central.InvitationStatus;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.InvitationRepository;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import id.my.hendisantika.multitenancy.service.email.EmailSender;
import id.my.hendisantika.multitenancy.service.email.InvitationEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Invitations: the owner names an email, the recipient sets their own password.
 * <p>
 * The token is returned once, at creation, and only its SHA-256 is stored. It
 * cannot be looked up again, which is the point: a leaked database hands out no
 * working invitations.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 09.14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvitationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final InvitationRepository invitationRepository;
    private final AccountRepository accountRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvitationProperties invitationProperties;
    private final EmailSender emailSender;
    private final EmailVerificationService emailVerificationService;

    /**
     * @return the invitation and the raw token, which is the only time it exists
     * outside the recipient's link
     */
    @Transactional("centralTransactionManager")
    public CreatedInvitation invite(String tenantSlug, String email, TenantRole role, Account invitedBy) {
        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new TenantProvisioningException("'" + tenantSlug + "' is not registered"));
        String normalizedEmail = normalize(email);

        accountRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(account -> {
            if (userTenantRepository.existsByAccountIdAndTenantSlug(account.getId(), tenant.getSlug())) {
                throw new AccountAlreadyExistsException(
                        "'" + normalizedEmail + "' is already a member of '" + tenant.getSlug() + "'");
            }
        });
        // A second invitation would leave the first still usable, so withdraw it.
        invitationRepository
                .findFirstByTenantSlugAndEmailIgnoreCaseAndStatus(
                        tenant.getSlug(), normalizedEmail, InvitationStatus.PENDING)
                .ifPresent(existing -> existing.setStatus(InvitationStatus.REVOKED));

        String token = newToken();
        Invitation invitation = new Invitation();
        invitation.setTenantSlug(tenant.getSlug());
        invitation.setEmail(normalizedEmail);
        invitation.setRole(role == null ? TenantRole.MEMBER : role);
        invitation.setTokenHash(hash(token));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(invitedBy);
        invitation.setCreatedAt(Instant.now());
        invitation.setExpiresAt(Instant.now().plus(invitationProperties.getTtl()));

        Invitation saved = invitationRepository.save(invitation);
        String acceptUrl = acceptUrl(token);
        boolean delivered = emailSender.send(InvitationEmail.build(
                normalizedEmail,
                tenant.getDisplayName() == null ? tenant.getSlug() : tenant.getDisplayName(),
                saved.getRole().name(),
                acceptUrl,
                invitationProperties.getTtl().toDays()));

        log.info("Invited {} to tenant {} as {}, emailed: {}",
                normalizedEmail, tenant.getSlug(), saved.getRole(), delivered);
        return new CreatedInvitation(saved, token, acceptUrl, delivered);
    }

    @Transactional(value = "centralTransactionManager", readOnly = true)
    public List<Invitation> pendingFor(String tenantSlug) {
        return invitationRepository.findAllByTenantSlugAndStatusOrderByCreatedAtDesc(
                tenantSlug, InvitationStatus.PENDING);
    }

    /**
     * One invitation of this tenant's, whatever became of it.
     * <p>
     * Not restricted to pending ones: a screen opened while it was still
     * pending should say that it has since been withdrawn or accepted, rather
     * than turning into "does not exist" and leaving the owner wondering.
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public Invitation oneOf(String tenantSlug, Long invitationId) {
        return invitationRepository.findById(invitationId)
                .filter(candidate -> candidate.getTenantSlug().equals(tenantSlug))
                .orElseThrow(() -> new TenantRecordNotFoundException(
                        "No invitation of '" + tenantSlug + "' with id " + invitationId));
    }

    /**
     * Whether accepting will grant an account that already exists or make a new
     * one. The recipient is told this before committing; the owner who sent it
     * has as much business knowing.
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public boolean accountExistsFor(Invitation invitation) {
        return accountRepository.findByEmailIgnoreCase(invitation.getEmail()).isPresent();
    }

    @Transactional("centralTransactionManager")
    public void revoke(String tenantSlug, Long invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .filter(candidate -> candidate.getTenantSlug().equals(tenantSlug))
                .orElseThrow(() -> new InvitationException("That invitation does not exist"));
        if (!InvitationStatus.PENDING.equals(invitation.getStatus())) {
            throw new InvitationException("That invitation is no longer pending");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
    }

    /**
     * What the accept page shows before anyone commits to anything.
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public InvitationPreview preview(String token) {
        Invitation invitation = usable(token);
        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(invitation.getTenantSlug())
                .orElseThrow(() -> new InvitationException("That organization no longer exists"));
        boolean accountExists = accountRepository.existsByEmailIgnoreCase(invitation.getEmail());
        return new InvitationPreview(
                invitation.getEmail(),
                invitation.getRole(),
                tenant.getSlug(),
                tenant.getDisplayName(),
                accountExists,
                invitation.getExpiresAt());
    }

    /**
     * @param password required only when the recipient has no account yet; an
     *                 existing account keeps the password it already has
     * @return the account that now holds the membership
     */
    @Transactional("centralTransactionManager")
    public Account accept(String token, String password) {
        Invitation invitation = usable(token);

        Account account = accountRepository.findByEmailIgnoreCase(invitation.getEmail())
                .orElseGet(() -> createAccount(invitation.getEmail(), password));

        if (!userTenantRepository.existsByAccountIdAndTenantSlug(account.getId(), invitation.getTenantSlug())) {
            UserTenant membership = new UserTenant();
            membership.setAccount(account);
            membership.setUserName(account.getEmail());
            membership.setTenantSlug(invitation.getTenantSlug());
            membership.setRole(invitation.getRole());
            membership.setCreatedAt(Instant.now());
            userTenantRepository.save(membership);
        }

        // Opening the link proves the address is reachable, which is what a
        // verification mail would have asked for.
        emailVerificationService.markVerifiedByInvitation(account);

        // Single use: the token is spent whether or not the membership was new.
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        log.info("{} accepted the invitation to {}", account.getEmail(), invitation.getTenantSlug());
        return account;
    }

    private Account createAccount(String email, String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            throw new InvitationException("Choose a password of at least 8 characters");
        }
        Account account = new Account();
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(password));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());
        return accountRepository.save(account);
    }

    /**
     * Deliberately one message for every failure: a caller holding a bad token
     * learns nothing about whether it ever existed.
     */
    private Invitation usable(String token) {
        Invitation invitation = invitationRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new InvitationException("This invitation link is not valid"));
        if (!invitation.isUsable()) {
            throw new InvitationException("This invitation link is not valid");
        }
        return invitation;
    }

    private String acceptUrl(String token) {
        String base = invitationProperties.getAcceptBaseUrl();
        return (base.endsWith("/") ? base : base + "/") + token;
    }

    private static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param delivered whether the link was emailed. When it was not, the caller
     *                  is expected to hand the link over instead.
     */
    public record CreatedInvitation(Invitation invitation, String token, String acceptUrl, boolean delivered) {
    }

    public record InvitationPreview(
            String email,
            TenantRole role,
            String tenantSlug,
            String organizationName,
            boolean accountExists,
            Instant expiresAt) {
    }
}
