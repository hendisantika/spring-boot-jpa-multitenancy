package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.PasswordReset;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.PasswordResetRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.05
 */
@SpringBootTest
class PasswordResetServiceTest {

    private static final String EMAIL = "reset.probe@example.test";
    private static final String OLD_PASSWORD = "old-password-1";
    private static final String NEW_PASSWORD = "new-password-1";

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DataSource centralDataSource;

    private Account account;

    @BeforeEach
    void setUp() {
        account = accountRepository.findByEmailIgnoreCase(EMAIL).orElseGet(() -> {
            Account created = new Account();
            created.setEmail(EMAIL);
            created.setPassword(passwordEncoder.encode(OLD_PASSWORD));
            created.setStatus(AccountStatus.ACTIVE);
            created.setCreatedAt(Instant.now());
            return accountRepository.save(created);
        });
    }

    @AfterEach
    void cleanUp() {
        accountRepository.findByEmailIgnoreCase(EMAIL).ifPresent(existing -> {
            passwordResetRepository.deleteAll(passwordResetRepository.findAllByAccountId(existing.getId()));
            accountRepository.delete(existing);
        });
    }

    private String tokenFrom(String resetUrl) {
        return resetUrl.substring(resetUrl.lastIndexOf('/') + 1);
    }

    @Test
    void resettingChangesThePasswordAndSpendsTheLink() {
        String url = passwordResetService.request(EMAIL).orElseThrow();
        String token = tokenFrom(url);

        assertThat(passwordResetService.emailFor(token)).isEqualTo(EMAIL);
        passwordResetService.reset(token, NEW_PASSWORD);

        Account updated = accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, updated.getPassword())).isTrue();
        assertThat(passwordEncoder.matches(OLD_PASSWORD, updated.getPassword())).isFalse();

        assertThatThrownBy(() -> passwordResetService.reset(token, "another-password-1"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessageContaining("not valid");
    }

    /**
     * Only a hash is kept, so a leaked database cannot be used to take over an
     * account.
     */
    @Test
    void onlyAHashOfTheTokenIsStored() {
        String token = tokenFrom(passwordResetService.request(EMAIL).orElseThrow());

        assertThat(passwordResetRepository.findAllByAccountId(account.getId()))
                .allSatisfy(reset -> {
                    assertThat(reset.getTokenHash()).doesNotContain(token);
                    assertThat(reset.getTokenHash()).hasSize(64);
                });
    }

    /**
     * The endpoint must not become a way to find out who is registered, so an
     * unknown address is answered exactly like a known one.
     */
    @Test
    void requestingForAnUnknownAddressRevealsNothingAndCreatesNothing() {
        assertThat(passwordResetService.request("nobody.here@example.test")).isEmpty();
        assertThat(passwordResetRepository.findAll())
                .noneSatisfy(reset -> assertThat(reset.getAccount().getEmail())
                        .isEqualTo("nobody.here@example.test"));
    }

    @Test
    void askingAgainInvalidatesTheEarlierLink() {
        String first = tokenFrom(passwordResetService.request(EMAIL).orElseThrow());
        String second = tokenFrom(passwordResetService.request(EMAIL).orElseThrow());

        assertThatThrownBy(() -> passwordResetService.emailFor(first))
                .isInstanceOf(PasswordResetException.class);
        assertThat(passwordResetService.emailFor(second)).isEqualTo(EMAIL);
    }

    @Test
    void anExpiredLinkIsRefused() {
        String url = passwordResetService.request(EMAIL).orElseThrow();
        String token = tokenFrom(url);
        expireAll();

        assertThatThrownBy(() -> passwordResetService.emailFor(token))
                .isInstanceOf(PasswordResetException.class);
        // The old password still works, so nothing was changed by the attempt.
        assertThat(passwordEncoder.matches(OLD_PASSWORD,
                accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow().getPassword())).isTrue();
    }

    @Test
    void aTooShortPasswordIsRefusedAndTheLinkSurvives() {
        String token = tokenFrom(passwordResetService.request(EMAIL).orElseThrow());

        assertThatThrownBy(() -> passwordResetService.reset(token, "short"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessageContaining("at least 8");
        assertThat(passwordResetService.emailFor(token)).isEqualTo(EMAIL);
    }

    /**
     * The point of resetting a stolen password: refresh tokens handed out before
     * it stop working, rather than lasting their full two weeks.
     */
    @Test
    void refreshTokensIssuedBeforeTheResetStopWorking() {
        String refreshToken = tokenService.issueRefreshToken(account);
        assertThat(authService.accountFromRefreshToken(refreshToken).getEmail()).isEqualTo(EMAIL);

        // Tokens carry a second-resolution issued-at, so step past it before
        // resetting or the comparison is a coin flip.
        sleepPastTokenSecond();
        passwordResetService.reset(tokenFrom(passwordResetService.request(EMAIL).orElseThrow()), NEW_PASSWORD);

        assertThatThrownBy(() -> authService.accountFromRefreshToken(refreshToken))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("sign in again");
    }

    @Test
    void aRefreshTokenIssuedAfterTheResetKeepsWorking() {
        passwordResetService.reset(tokenFrom(passwordResetService.request(EMAIL).orElseThrow()), NEW_PASSWORD);
        sleepPastTokenSecond();

        Account updated = accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        String refreshToken = tokenService.issueRefreshToken(updated);

        assertThat(authService.accountFromRefreshToken(refreshToken).getEmail()).isEqualTo(EMAIL);
    }

    private void sleepPastTokenSecond() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Straight SQL with an explicit commit: expires_at is mapped updatable = false
     * and the pools run with autoCommit disabled.
     */
    private void expireAll() {
        try (Connection connection = centralDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE password_resets SET expires_at = ? WHERE account_id = ?")) {
            statement.setTimestamp(1, Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)));
            statement.setLong(2, account.getId());
            int updated = statement.executeUpdate();
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            assertThat(updated).as("rows expired").isPositive();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not expire the reset", e);
        }
    }

    @Test
    void aTokenThatNeverExistedIsRefusedWithTheSameMessage() {
        assertThatThrownBy(() -> passwordResetService.emailFor("not-a-real-token"))
                .isInstanceOf(PasswordResetException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void theLinkIsReturnedOnlyBecauseMailDeliveryIsOff() {
        // With Brevo configured this would be empty and the link would be emailed.
        assertThat(passwordResetService.request(EMAIL)).isPresent();
        assertThat(passwordResetRepository.findAllByAccountId(account.getId()))
                .extracting(PasswordReset::getUsedAt)
                .containsOnlyNulls();
    }
}
