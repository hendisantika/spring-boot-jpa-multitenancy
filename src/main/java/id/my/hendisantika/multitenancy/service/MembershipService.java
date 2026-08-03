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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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

    /**
     * A page of them. A membership list only grows — a hospital group is not a
     * handful of people — and an endpoint that returns all of it is one nobody
     * can withdraw later, which is why the tenant's own lists are paged too.
     *
     * @param query matched against the address and the role; null or blank
     *              means everybody
     * @param roles narrows to these, and several of them mean either — empty
     *              means no filtering at all
     * @param page  zero based
     * @param size  clamped, so a client cannot ask for the lot in one go
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public Page<UserTenant> membersOf(String tenantSlug, String query, Collection<String> roles,
                                      Integer page, Integer size) {
        String term = TenantListing.searchTerm(query);
        // Nothing asked for is no filter at all; something asked for that maps
        // to no role is a filter that matches nothing, which is what asking for
        // a role nobody holds should give you.
        boolean anyRole = roles == null || roles.isEmpty();
        return userTenantRepository.search(
                tenantSlug,
                // Blank means everybody, not nobody.
                term == null ? TenantListing.MATCH_EVERYTHING : term,
                TenantRole.matching(query),
                anyRole,
                anyRole ? List.of() : TenantRole.parseAll(roles),
                TenantListing.pageRequest(page, size));
    }


    /**
     * One membership, by the pair that identifies it.
     */
    @Transactional(value = "centralTransactionManager", readOnly = true)
    public Optional<UserTenant> memberOf(String tenantSlug, Long accountId) {
        return userTenantRepository.findByTenantSlugAndAccountId(tenantSlug, accountId);
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
