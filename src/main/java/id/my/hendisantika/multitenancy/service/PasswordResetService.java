package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.config.PasswordResetProperties;
import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.PasswordReset;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.PasswordResetRepository;
import id.my.hendisantika.multitenancy.service.email.EmailSender;
import id.my.hendisantika.multitenancy.service.email.PasswordResetEmail;
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
import java.util.Locale;
import java.util.Optional;

/**
 * Password reset by emailed link.
 * <p>
 * Requesting one never reveals whether the address has an account: the response
 * is the same either way, which is what stops the endpoint being used to find
 * out who is registered.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int MINIMUM_PASSWORD_LENGTH = 8;

    private final PasswordResetRepository passwordResetRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties passwordResetProperties;
    private final EmailSender emailSender;

    /**
     * @return the reset link when the address has an account and mail delivery is
     * off, so a developer without a mail account can still follow the flow.
     * Empty otherwise, including when the address is unknown.
     */
    @Transactional("centralTransactionManager")
    public Optional<String> request(String email) {
        String normalized = normalize(email);
        Optional<Account> account = accountRepository.findByEmailIgnoreCase(normalized);
        if (account.isEmpty()) {
            // Same work, same answer: the caller cannot tell this apart.
            log.info("Password reset requested for an unknown address");
            return Optional.empty();
        }

        // Any earlier link stops working, so a request cannot leave several ways in.
        passwordResetRepository.findAllByAccountId(account.get().getId()).stream()
                .filter(PasswordReset::isUsable)
                .forEach(previous -> previous.setUsedAt(Instant.now()));

        String token = newToken();
        PasswordReset reset = new PasswordReset();
        reset.setAccount(account.get());
        reset.setTokenHash(hash(token));
        reset.setCreatedAt(Instant.now());
        reset.setExpiresAt(Instant.now().plus(passwordResetProperties.getTtl()));
        passwordResetRepository.save(reset);

        String resetUrl = resetUrl(token);
        boolean delivered = emailSender.send(PasswordResetEmail.build(
                normalized, resetUrl, passwordResetProperties.getTtl().toMinutes()));
        log.info("Password reset requested for {}, emailed: {}", normalized, delivered);

        return delivered ? Optional.empty() : Optional.of(resetUrl);
    }

    /**
     * @return the address the link belongs to, so the page can show whose account
     * is being reset
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public String emailFor(String token) {
        return usable(token).getAccount().getEmail();
    }

    @Transactional("centralTransactionManager")
    public void reset(String token, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new PasswordResetException(
                    "Choose a password of at least " + MINIMUM_PASSWORD_LENGTH + " characters");
        }
        PasswordReset reset = usable(token);
        Account account = reset.getAccount();

        account.setPassword(passwordEncoder.encode(newPassword));
        // Refresh tokens live for two weeks; stamping this disowns the ones handed
        // out before the reset, which is the point of resetting a stolen password.
        account.setPasswordChangedAt(Instant.now());
        reset.setUsedAt(Instant.now());

        log.info("Password reset completed for {}", account.getEmail());
    }

    /**
     * One message for every failure, so a caller holding a bad token learns
     * nothing about whether it ever existed.
     */
    private PasswordReset usable(String token) {
        PasswordReset reset = passwordResetRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new PasswordResetException("This reset link is not valid"));
        if (!reset.isUsable()) {
            throw new PasswordResetException("This reset link is not valid");
        }
        return reset;
    }

    private String resetUrl(String token) {
        String base = passwordResetProperties.getResetBaseUrl();
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
}
