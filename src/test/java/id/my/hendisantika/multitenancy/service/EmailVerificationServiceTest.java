package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.EmailVerificationRepository;
import id.my.hendisantika.multitenancy.service.email.EmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Delivery is mocked as enabled, because the interesting behaviour is what
 * happens when mail can be sent: without it the service marks accounts verified
 * straight away.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.18
 */
@SpringBootTest
class EmailVerificationServiceTest {

    private static final String EMAIL = "verify.probe@example.test";

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailSender emailSender;

    private Account account;

    @BeforeEach
    void setUp() {
        given(emailSender.isEnabled()).willReturn(true);
        // Delivery reported as failing on purpose, so the link comes back and the
        // test can follow it. A successful send withholds it by design.
        given(emailSender.send(any())).willReturn(false);

        account = accountRepository.findByEmailIgnoreCase(EMAIL).orElseGet(() -> {
            Account created = new Account();
            created.setEmail(EMAIL);
            created.setPassword(passwordEncoder.encode("a-password-1"));
            created.setStatus(AccountStatus.ACTIVE);
            created.setCreatedAt(Instant.now());
            return accountRepository.save(created);
        });
        account.setEmailVerifiedAt(null);
        accountRepository.save(account);
    }

    @AfterEach
    void cleanUp() {
        accountRepository.findByEmailIgnoreCase(EMAIL).ifPresent(existing -> {
            emailVerificationRepository.deleteAll(
                    emailVerificationRepository.findAllByAccountId(existing.getId()));
            accountRepository.delete(existing);
        });
    }

    private String tokenFrom(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    @Test
    void verifyingMarksTheAccountAndSpendsTheLink() {
        String token = tokenFrom(emailVerificationService.startFor(account).orElseThrow());
        assertThat(accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow().isEmailVerified()).isFalse();

        assertThat(emailVerificationService.verify(token)).isEqualTo(EMAIL);

        assertThat(accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow().isEmailVerified()).isTrue();
        assertThatThrownBy(() -> emailVerificationService.verify(token))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessageContaining("not valid");
    }

    @Test
    void onlyAHashOfTheTokenIsStored() {
        String token = tokenFrom(emailVerificationService.startFor(account).orElseThrow());

        assertThat(emailVerificationRepository.findAllByAccountId(account.getId()))
                .allSatisfy(verification -> {
                    assertThat(verification.getTokenHash()).doesNotContain(token);
                    assertThat(verification.getTokenHash()).hasSize(64);
                });
    }

    @Test
    void resendingInvalidatesTheEarlierLink() {
        String first = tokenFrom(emailVerificationService.startFor(account).orElseThrow());
        String second = tokenFrom(emailVerificationService.resendFor(account).orElseThrow());

        assertThatThrownBy(() -> emailVerificationService.verify(first))
                .isInstanceOf(EmailVerificationException.class);
        assertThat(emailVerificationService.verify(second)).isEqualTo(EMAIL);
    }

    @Test
    void resendingIsRefusedOnceVerified() {
        String token = tokenFrom(emailVerificationService.startFor(account).orElseThrow());
        emailVerificationService.verify(token);

        assertThatThrownBy(() -> emailVerificationService.resendFor(
                accountRepository.findByEmailIgnoreCase(EMAIL).orElseThrow()))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessageContaining("already verified");
    }

    /**
     * Once a mailbox holds the link, handing it back would put a working
     * credential somewhere it does not belong.
     */
    @Test
    void theLinkIsWithheldWhenItWasDelivered() {
        given(emailSender.send(any())).willReturn(true);

        assertThat(emailVerificationService.startFor(account)).isEmpty();
        assertThat(account.isEmailVerified()).isFalse();
        assertThat(emailVerificationRepository.findAllByAccountId(account.getId())).hasSize(1);
    }

    /**
     * With nothing able to arrive, requiring proof would lock everyone out, so
     * the account is verified at signup instead.
     */
    @Test
    void withoutMailDeliveryTheAccountIsVerifiedStraightAway() {
        given(emailSender.isEnabled()).willReturn(false);

        assertThat(emailVerificationService.startFor(account)).isEmpty();
        assertThat(account.isEmailVerified()).isTrue();
    }

    @Test
    void anInvitationCountsAsProofOfTheAddress() {
        assertThat(account.isEmailVerified()).isFalse();

        emailVerificationService.markVerifiedByInvitation(account);

        assertThat(account.isEmailVerified()).isTrue();
    }

    @Test
    void aTokenThatNeverExistedIsRefusedWithTheSameMessage() {
        assertThatThrownBy(() -> emailVerificationService.verify("not-a-real-token"))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessageContaining("not valid");
    }
}
