package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.EmailChange;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.EmailChangeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules behind moving an account to a different address. The one they all
 * serve: nothing changes until the link sent to the new mailbox is opened,
 * because the address is the credential and a typo would otherwise cost the
 * account itself.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 03/08/26
 * Time: 08.29
 */
@SpringBootTest
class EmailChangeServiceTest {

    private static final String EMAIL = "change.probe@example.test";
    private static final String NEW_EMAIL = "change.moved@example.test";
    private static final String SOMEBODY_ELSE = "change.taken@example.test";
    private static final String PASSWORD = "current-password-1";

    @Autowired
    private EmailChangeService emailChangeService;

    @Autowired
    private EmailChangeRepository emailChangeRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Account account;

    @BeforeEach
    void setUp() {
        account = accountFor(EMAIL, PASSWORD);
    }

    @AfterEach
    void cleanUp() {
        for (String email : new String[]{EMAIL, NEW_EMAIL, SOMEBODY_ELSE}) {
            accountRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
                emailChangeRepository.deleteAll(emailChangeRepository.findAllByAccountId(existing.getId()));
                accountRepository.delete(existing);
            });
        }
    }

    private Account accountFor(String email, String password) {
        return accountRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Account created = new Account();
            created.setEmail(email);
            created.setPassword(passwordEncoder.encode(password));
            created.setStatus(AccountStatus.ACTIVE);
            created.setCreatedAt(Instant.now());
            created.setEmailVerifiedAt(Instant.now());
            return accountRepository.save(created);
        });
    }

    private String tokenFrom(String confirmUrl) {
        return confirmUrl.substring(confirmUrl.lastIndexOf('/') + 1);
    }

    private String currentEmail() {
        return accountRepository.findById(account.getId()).orElseThrow().getEmail();
    }

    @Test
    void confirmingMovesTheAccountAndSpendsTheLink() {
        String token = tokenFrom(emailChangeService.request(account, NEW_EMAIL, PASSWORD).orElseThrow());

        assertThat(emailChangeService.confirm(token)).isEqualTo(NEW_EMAIL);
        assertThat(currentEmail()).isEqualTo(NEW_EMAIL);

        assertThatThrownBy(() -> emailChangeService.confirm(token))
                .isInstanceOf(EmailChangeException.class)
                .hasMessageContaining("not valid");
    }

    /**
     * The whole point of the wait: until the link is opened the account signs in
     * exactly as it did, so a mistyped address costs an email and nothing else.
     */
    @Test
    void nothingChangesUntilTheLinkIsOpened() {
        emailChangeService.request(account, NEW_EMAIL, PASSWORD).orElseThrow();

        assertThat(currentEmail()).isEqualTo(EMAIL);
        assertThat(emailChangeService.pendingFor(account).map(EmailChange::getNewEmail))
                .contains(NEW_EMAIL);
    }

    /**
     * This is the credential itself, so holding a session is not enough: without
     * the password a stolen session would be a way to take the account over.
     */
    @Test
    void theCurrentPasswordIsRequired() {
        assertThatThrownBy(() -> emailChangeService.request(account, NEW_EMAIL, "not-the-password"))
                .isInstanceOf(EmailChangeException.class)
                .hasMessageContaining("not the current password");

        assertThat(emailChangeRepository.findAllByAccountId(account.getId())).isEmpty();
    }

    @Test
    void movingToTheAddressAlreadyOnTheAccountIsRefused() {
        assertThatThrownBy(() -> emailChangeService.request(account, EMAIL.toUpperCase(), PASSWORD))
                .isInstanceOf(EmailChangeException.class)
                .hasMessageContaining("already the address");
    }

    @Test
    void anAddressThatBelongsToSomebodyElseIsRefused() {
        accountFor(SOMEBODY_ELSE, PASSWORD);

        assertThatThrownBy(() -> emailChangeService.request(account, SOMEBODY_ELSE, PASSWORD))
                .isInstanceOf(EmailChangeException.class)
                .hasMessageContaining("already exists");
    }

    /**
     * A day may pass between asking and confirming, and requesting an address
     * reserves nothing, so whoever registered it in the meantime keeps it.
     */
    @Test
    void anAddressTakenAfterTheRequestIsRefusedAtConfirmation() {
        String token = tokenFrom(emailChangeService.request(account, SOMEBODY_ELSE, PASSWORD).orElseThrow());
        accountFor(SOMEBODY_ELSE, PASSWORD);

        assertThatThrownBy(() -> emailChangeService.confirm(token))
                .isInstanceOf(EmailChangeException.class)
                .hasMessageContaining("already exists");
        assertThat(currentEmail()).isEqualTo(EMAIL);
    }

    /**
     * Otherwise a typo would leave two addresses waiting and two links working,
     * and whichever mailbox was reached first would win.
     */
    @Test
    void askingAgainReplacesTheEarlierRequest() {
        String first = tokenFrom(emailChangeService.request(account, SOMEBODY_ELSE, PASSWORD).orElseThrow());
        String second = tokenFrom(emailChangeService.request(account, NEW_EMAIL, PASSWORD).orElseThrow());

        assertThatThrownBy(() -> emailChangeService.confirm(first))
                .isInstanceOf(EmailChangeException.class);
        assertThat(emailChangeService.pendingFor(account).map(EmailChange::getNewEmail))
                .contains(NEW_EMAIL);
        assertThat(emailChangeService.confirm(second)).isEqualTo(NEW_EMAIL);
    }

    @Test
    void cancellingDropsTheRequestAndItsLink() {
        String token = tokenFrom(emailChangeService.request(account, NEW_EMAIL, PASSWORD).orElseThrow());

        emailChangeService.cancel(account);

        assertThat(emailChangeService.pendingFor(account)).isEmpty();
        assertThatThrownBy(() -> emailChangeService.confirm(token))
                .isInstanceOf(EmailChangeException.class);
        assertThat(currentEmail()).isEqualTo(EMAIL);
    }

    /**
     * Opening the link proves the mailbox can be read, which is the only thing
     * verification ever asked for.
     */
    @Test
    void confirmingCountsAsVerifyingTheNewAddress() {
        Account unverified = accountRepository.findById(account.getId()).orElseThrow();
        unverified.setEmailVerifiedAt(null);
        accountRepository.save(unverified);

        String token = tokenFrom(emailChangeService.request(account, NEW_EMAIL, PASSWORD).orElseThrow());
        emailChangeService.confirm(token);

        assertThat(accountRepository.findById(account.getId()).orElseThrow().isEmailVerified()).isTrue();
    }

    /**
     * Only a hash is kept, so a leaked database yields no working links.
     */
    @Test
    void onlyAHashOfTheTokenIsStored() {
        String token = tokenFrom(emailChangeService.request(account, NEW_EMAIL, PASSWORD).orElseThrow());

        assertThat(emailChangeRepository.findAllByAccountId(account.getId()))
                .allSatisfy(change -> {
                    assertThat(change.getTokenHash()).doesNotContain(token);
                    assertThat(change.getTokenHash()).hasSize(64);
                });
    }
}
