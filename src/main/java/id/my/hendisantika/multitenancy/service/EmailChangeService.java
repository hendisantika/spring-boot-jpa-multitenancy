package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.EmailChangeProperties;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.EmailChange;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.EmailChangeRepository;
import id.my.hendisantika.multitenancy.service.email.EmailChangeEmail;
import id.my.hendisantika.multitenancy.service.email.EmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Moving an account to a different address.
 * <p>
 * The change is not applied when it is asked for. A link goes to the new
 * address and the account keeps signing in as it did until that link is opened,
 * for two reasons: the email is the credential, so a typo would otherwise lock
 * somebody out of the account and out of resetting its password too; and an
 * address nobody has proved they can read is not worth having.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 03/08/26
 * Time: 08.29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailChangeService {

    private final EmailChangeRepository emailChangeRepository;
    private final AccountRepository accountRepository;
    private final EmailChangeProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;

    /**
     * Asks to move to a different address, which requires the current password:
     * this is the credential you sign in with, so a stolen session alone should
     * not be enough to take an account over.
     *
     * @return the confirmation link when mail delivery is off, so the flow stays
     * followable locally; empty when it was emailed, because once a mailbox has
     * it, handing it back would put a working credential somewhere else
     */
    @Transactional("centralTransactionManager")
    public Optional<String> request(Account account, String newEmail, String password) {
        Account managed = accountRepository.findById(account.getId())
                .orElseThrow(() -> new AuthenticationFailedException("The account no longer exists"));
        if (!passwordEncoder.matches(password, managed.getPassword())) {
            throw new EmailChangeException("That is not the current password");
        }

        String normalized = normalize(newEmail);
        if (normalized.equalsIgnoreCase(managed.getEmail())) {
            throw new EmailChangeException("That is already the address on this account");
        }
        // Signup says the same thing to anybody who asks, so saying it here to
        // somebody who has just proved the password gives nothing else away, and
        // the alternative is a confirmation mail that could never arrive.
        if (accountRepository.existsByEmailIgnoreCase(normalized)) {
            throw new EmailChangeException("An account with that email already exists");
        }

        // Asking again replaces the earlier request rather than adding to it, so
        // there is only ever one address waiting and one link that works.
        cancelPending(managed);

        String token = SecureTokens.newToken();
        EmailChange change = new EmailChange();
        change.setAccount(managed);
        change.setNewEmail(normalized);
        change.setTokenHash(SecureTokens.hash(token));
        change.setCreatedAt(Instant.now());
        change.setExpiresAt(Instant.now().plus(properties.getTtl()));
        emailChangeRepository.save(change);

        String link = SecureTokens.linkTo(properties.getConfirmBaseUrl(), token);
        boolean delivered = emailSender.send(EmailChangeEmail.build(
                normalized, managed.getEmail(), link, properties.getTtl().toHours()));
        log.info("Email change requested for account {}, emailed: {}", managed.getId(), delivered);
        return delivered ? Optional.empty() : Optional.of(link);
    }

    /**
     * @return the address waiting to be confirmed, so a screen can say that one
     * is outstanding rather than looking as though nothing happened
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public Optional<EmailChange> pendingFor(Account account) {
        return emailChangeRepository.findAllByAccountId(account.getId()).stream()
                .filter(EmailChange::isUsable)
                .findFirst();
    }

    /**
     * Drops the outstanding request, for whoever changed their mind or typed the
     * address wrong and would rather not wait a day for the link to lapse.
     */
    @Transactional("centralTransactionManager")
    public void cancel(Account account) {
        cancelPending(account);
    }

    /**
     * @return the address the account now signs in as
     */
    @Transactional("centralTransactionManager")
    public String confirm(String token) {
        EmailChange change = emailChangeRepository
                .findByTokenHash(SecureTokens.hash(token))
                .filter(EmailChange::isUsable)
                // One message for every failure, as with the other link flows.
                .orElseThrow(() -> new EmailChangeException("This link is not valid"));

        // Checked again rather than only when it was asked for: a day may have
        // passed, and somebody else may have registered the address since.
        if (accountRepository.existsByEmailIgnoreCase(change.getNewEmail())) {
            throw new EmailChangeException("An account with that email already exists");
        }

        Account account = change.getAccount();
        String previous = account.getEmail();
        account.setEmail(change.getNewEmail());
        // Opening the link proved the mailbox, which is all verification asks.
        account.setEmailVerifiedAt(Instant.now());
        change.setUsedAt(Instant.now());
        log.info("Account {} moved from {} to {}", account.getId(), previous, account.getEmail());
        return account.getEmail();
    }

    private void cancelPending(Account account) {
        emailChangeRepository.findAllByAccountId(account.getId()).stream()
                .filter(EmailChange::isUsable)
                .forEach(pending -> pending.setUsedAt(Instant.now()));
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
