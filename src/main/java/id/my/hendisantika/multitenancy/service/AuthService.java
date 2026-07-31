package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Signup and the parent login. Both live in the central database, because an
 * account exists before any tenant does and may belong to several.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PHOTO_PREFIX = "accounts";

    private final AccountRepository accountRepository;
    private final UserTenantRepository userTenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final TokenService tokenService;
    private final JwtDecoder jwtDecoder;
    private final EmailVerificationService emailVerificationService;

    /**
     * Registers an owner. The photo is optional so that signup still works when
     * the client uploads it in a later step.
     */
    @Transactional("centralTransactionManager")
    public Account signUp(String email, String phoneNumber, String password, MultipartFile photo) {
        String normalizedEmail = normalize(email);
        if (accountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AccountAlreadyExistsException("An account with that email already exists");
        }
        Account account = new Account();
        account.setEmail(normalizedEmail);
        account.setPhoneNumber(phoneNumber);
        account.setPassword(passwordEncoder.encode(password));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());
        if (photo != null && !photo.isEmpty()) {
            account.setPhotoKey(storageService.store(photo, PHOTO_PREFIX));
        }
        Account saved = accountRepository.save(account);
        emailVerificationService.startFor(saved);
        log.info("Registered account {}", saved.getEmail());
        return saved;
    }

    /**
     * @return the authenticated account, never a hint about which half of the
     * credentials was wrong
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public Account authenticate(String email, String password) {
        Account account = accountRepository.findByEmailIgnoreCase(normalize(email))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }
        if (!account.isActive()) {
            throw new AuthenticationFailedException("This account is disabled");
        }
        return account;
    }

    @Transactional(value = "centralTransactionManager", readOnly = true)
    public List<UserTenant> membershipsOf(Account account) {
        return userTenantRepository.findAllByAccountId(account.getId());
    }

    /**
     * @param subject the {@code sub} claim of a validated token
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public Account accountOf(String subject) {
        return accountRepository.findById(Long.valueOf(subject))
                .orElseThrow(() -> new AuthenticationFailedException("The account no longer exists"));
    }

    /**
     * Exchanges a refresh token for a new pair. Memberships are read from the
     * database at this moment rather than carried over, so a tenant granted or
     * revoked since the last login takes effect on the next refresh.
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public Account accountFromRefreshToken(String refreshToken) {
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(refreshToken);
        } catch (JwtException e) {
            throw new AuthenticationFailedException("The refresh token is not valid");
        }
        if (!TokenService.TYPE_REFRESH.equals(jwt.getClaimAsString(TokenService.CLAIM_TOKEN_TYPE))) {
            throw new AuthenticationFailedException("That token cannot be used to refresh");
        }
        Account account = accountRepository.findById(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new AuthenticationFailedException("The account no longer exists"));
        if (!account.isActive()) {
            throw new AuthenticationFailedException("This account is disabled");
        }
        // Refresh tokens live for two weeks, so a password reset has to disown the
        // ones handed out before it. Without this, resetting a stolen password
        // would leave the thief a fortnight of access.
        if (account.getPasswordChangedAt() != null
                && jwt.getIssuedAt() != null
                && jwt.getIssuedAt().isBefore(account.getPasswordChangedAt())) {
            throw new AuthenticationFailedException("The password changed; please sign in again");
        }
        return account;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
