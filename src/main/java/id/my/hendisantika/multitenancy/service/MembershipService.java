package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.entity.central.UserTenant;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * The owner adding people to their organization.
 * <p>
 * A member is an account plus a membership, so somebody who already has an
 * account elsewhere is granted access rather than duplicated.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipService {

    private final AccountRepository accountRepository;
    private final UserTenantRepository userTenantRepository;
    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional("centralTransactionManager")
    public UserTenant addMember(String tenantSlug, String email, String phoneNumber,
                                String password, TenantRole role) {
        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new TenantProvisioningException("'" + tenantSlug + "' is not registered"));

        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Account account = accountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> createAccount(normalizedEmail, phoneNumber, password));

        if (userTenantRepository.existsByAccountIdAndTenantSlug(account.getId(), tenant.getSlug())) {
            throw new AccountAlreadyExistsException(
                    "'" + normalizedEmail + "' is already a member of '" + tenant.getSlug() + "'");
        }

        UserTenant membership = new UserTenant();
        membership.setAccount(account);
        membership.setUserName(account.getEmail());
        membership.setTenantSlug(tenant.getSlug());
        membership.setRole(role == null ? TenantRole.MEMBER : role);
        membership.setCreatedAt(Instant.now());
        UserTenant saved = userTenantRepository.save(membership);
        log.info("Added {} to tenant {} as {}", account.getEmail(), tenant.getSlug(), saved.getRole());
        return saved;
    }

    @Transactional(value = "centralTransactionManager", readOnly = true)
    public List<UserTenant> membersOf(String tenantSlug) {
        return userTenantRepository.findAllByTenantSlug(tenantSlug);
    }

    /**
     * Organizations the caller belongs to, which is all they are allowed to see.
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public List<TenantRegistration> organizationsOf(Long accountId) {
        List<String> slugs = userTenantRepository.findAllByAccountId(accountId).stream()
                .map(UserTenant::getTenantSlug)
                .toList();
        return slugs.isEmpty() ? List.of() : tenantRegistrationRepository.findAllBySlugIn(slugs);
    }

    @Transactional("centralTransactionManager")
    public void removeMember(String tenantSlug, Long accountId) {
        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> new TenantProvisioningException("'" + tenantSlug + "' is not registered"));
        if (tenant.getOwner() != null && tenant.getOwner().getId().equals(accountId)) {
            // Removing the owner would leave nobody able to administer it.
            throw new TenantProvisioningException("The owner cannot be removed from their own organization");
        }
        userTenantRepository.findAllByTenantSlug(tenantSlug).stream()
                .filter(membership -> membership.getAccount() != null
                        && membership.getAccount().getId().equals(accountId))
                .forEach(userTenantRepository::delete);
    }

    private Account createAccount(String email, String phoneNumber, String password) {
        Account account = new Account();
        account.setEmail(email);
        account.setPhoneNumber(phoneNumber);
        account.setPassword(passwordEncoder.encode(password));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(Instant.now());
        return accountRepository.save(account);
    }
}
