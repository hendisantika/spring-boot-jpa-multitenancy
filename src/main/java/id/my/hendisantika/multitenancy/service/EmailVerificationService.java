package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.EmailVerificationProperties;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.EmailVerification;
import id.my.hendisantika.multitenancy.repository.central.EmailVerificationRepository;
import id.my.hendisantika.multitenancy.service.email.EmailSender;
import id.my.hendisantika.multitenancy.service.email.EmailVerificationEmail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Proves that whoever signed up can read the address they used.
 * <p>
 * Verification is only enforced when mail can actually be delivered. With no
 * Brevo key configured, nothing could ever arrive, so accounts are marked
 * verified at signup rather than leaving the application unusable.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailVerificationProperties properties;
    private final EmailSender emailSender;

    /**
     * @return the verification link when mail delivery is off, so the flow stays
     * followable locally; empty when it was emailed
     */
    @Transactional("centralTransactionManager")
    public Optional<String> startFor(Account account) {
        if (!emailSender.isEnabled()) {
            // Nothing could arrive, so requiring proof would lock everyone out.
            account.setEmailVerifiedAt(Instant.now());
            log.info("Email delivery is off; marking {} verified at signup", account.getEmail());
            return Optional.empty();
        }
        return issue(account);
    }

    /**
     * Sends a fresh link, invalidating any earlier one.
     */
    @Transactional("centralTransactionManager")
    public Optional<String> resendFor(Account account) {
        if (account.isEmailVerified()) {
            throw new EmailVerificationException("That address is already verified");
        }
        if (!emailSender.isEnabled()) {
            throw new EmailVerificationException("Email delivery is not configured");
        }
        return issue(account);
    }

    /**
     * @return the link only when it could not be delivered, matching how
     * invitations and password resets behave: once a mailbox has it, handing it
     * back would put a working credential somewhere it does not belong
     */
    private Optional<String> issue(Account account) {
        emailVerificationRepository.findAllByAccountId(account.getId()).stream()
                .filter(EmailVerification::isUsable)
                .forEach(previous -> previous.setUsedAt(Instant.now()));

        String token = SecureTokens.newToken();
        EmailVerification verification = new EmailVerification();
        verification.setAccount(account);
        verification.setTokenHash(SecureTokens.hash(token));
        verification.setCreatedAt(Instant.now());
        verification.setExpiresAt(Instant.now().plus(properties.getTtl()));
        emailVerificationRepository.save(verification);

        String link = SecureTokens.linkTo(properties.getVerifyBaseUrl(), token);
        boolean delivered = emailSender.send(EmailVerificationEmail.build(
                account.getEmail(), link, properties.getTtl().toHours()));
        log.info("Verification link issued for {}, emailed: {}", account.getEmail(), delivered);
        return delivered ? Optional.empty() : Optional.of(link);
    }

    /**
     * @return the address that was just proved reachable
     */
    @Transactional("centralTransactionManager")
    public String verify(String token) {
        EmailVerification verification = emailVerificationRepository
                .findByTokenHash(SecureTokens.hash(token))
                .filter(EmailVerification::isUsable)
                // One message for every failure, as with the other link flows.
                .orElseThrow(() -> new EmailVerificationException("This verification link is not valid"));

        Account account = verification.getAccount();
        account.setEmailVerifiedAt(Instant.now());
        verification.setUsedAt(Instant.now());
        log.info("Verified {}", account.getEmail());
        return account.getEmail();
    }

    /**
     * Opening an invitation link proves the same thing a verification mail would,
     * so accepting one counts.
     */
    @Transactional("centralTransactionManager")
    public void markVerifiedByInvitation(Account account) {
        if (!account.isEmailVerified()) {
            account.setEmailVerifiedAt(Instant.now());
        }
    }
}
