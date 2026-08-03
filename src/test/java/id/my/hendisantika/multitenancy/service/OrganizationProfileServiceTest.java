package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.central.Account;
import id.my.hendisantika.multitenancy.entity.central.AccountStatus;
import id.my.hendisantika.multitenancy.entity.central.OrgStructure;
import id.my.hendisantika.multitenancy.entity.central.PracticeSpeciality;
import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.repository.central.AccountRepository;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.repository.central.UserTenantRepository;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.31
 */
@SpringBootTest
class OrganizationProfileServiceTest {

    private static final String OWNER_EMAIL = "edit.owner@example.test";
    private static final String ORGANIZATION = "Edit Probe Clinic";
    private static final String SLUG = "editprobeclinic";

    @Autowired
    private OrganizationProfileService organizationProfileService;

    @Autowired
    private TenantProvisioningService tenantProvisioningService;

    @Autowired
    private TenantRegistrationRepository tenantRegistrationRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserTenantRepository userTenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        Account owner = accountRepository.findByEmailIgnoreCase(OWNER_EMAIL).orElseGet(() -> {
            Account account = new Account();
            account.setEmail(OWNER_EMAIL);
            account.setPassword(passwordEncoder.encode("owner-password"));
            account.setStatus(AccountStatus.ACTIVE);
            account.setCreatedAt(Instant.now());
            account.setEmailVerifiedAt(Instant.now());
            return accountRepository.save(account);
        });
        if (tenantRegistrationRepository.findBySlug(SLUG).isEmpty()) {
            tenantProvisioningService.provision(
                    new OrganizationProfile(ORGANIZATION, "before@example.test", "organizations/before.png",
                            "Before", "Name", "Nurse", "+62 811 0000 0001",
                            OrgStructure.SINGLE_LOCATION_CLINIC, PracticeSpeciality.GENERAL_PRACTICE),
                    owner);
        }
    }

    @AfterEach
    void cleanUp() {
        if (tenantRegistrationRepository.findBySlug(SLUG).isPresent()) {
            tenantProvisioningService.deprovision(SLUG);
        }
        accountRepository.findByEmailIgnoreCase(OWNER_EMAIL).ifPresent(account -> {
            userTenantRepository.deleteAll(userTenantRepository.findAllByAccountId(account.getId()));
            accountRepository.delete(account);
        });
    }

    private OrganizationProfile changed() {
        return new OrganizationProfile(
                "Renamed Clinic", "after@example.test", null,
                "After", "Person", "Practice Manager", "+62 811 0000 0002",
                OrgStructure.MULTI_LOCATION_HOSPITAL, PracticeSpeciality.DENTAL);
    }

    /**
     * Replacing a photo was possible; having none again was not.
     */
    @Test
    void aPhotoCanBeRemoved() {
        organizationProfileService.update(SLUG, changed(), "organizations/first.png");
        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey())
                .isEqualTo("organizations/first.png");

        organizationProfileService.update(SLUG, changed(), null, true);

        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey()).isNull();
        verify(storageService).delete("organizations/first.png");
    }

    /**
     * Asking to remove one while uploading another is a contradiction, and the
     * upload wins rather than leaving the tenant with neither.
     */
    @Test
    void anUploadWinsOverTheRemovalFlag() {
        organizationProfileService.update(SLUG, changed(), "organizations/first.png");

        organizationProfileService.update(SLUG, changed(), "organizations/second.png", true);

        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey())
                .isEqualTo("organizations/second.png");
    }

    /**
     * The flag on a tenant that has no photo is not an error, it is nothing —
     * and in particular it does not ask the bucket to delete null.
     */
    @Test
    void removingWhenThereIsNoPhotoDoesNothing() {
        organizationProfileService.update(SLUG, changed(), null, true);
        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey()).isNull();

        organizationProfileService.update(SLUG, changed(), null, true);

        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey()).isNull();
        // Once for the photo the fixture started with, and not again.
        verify(storageService, times(1)).delete(any());
    }

    /**
     * The card on the organization page changes a picture without carrying the
     * profile, which is the point: the profile is what gets overwritten when it
     * is re-sent to do something else.
     */
    @Test
    void changingOnlyThePhotoLeavesTheProfileAlone() {
        organizationProfileService.updatePhoto(SLUG, "organizations/only.png", false);

        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(SLUG).orElseThrow();
        assertThat(tenant.getPhotoKey()).isEqualTo("organizations/only.png");
        assertThat(tenant.getDisplayName()).isEqualTo(ORGANIZATION);
        assertThat(tenant.getBusinessEmail()).isEqualTo("before@example.test");
        assertThat(tenant.getPhoneNumber()).isEqualTo("+62 811 0000 0001");
        // The one it replaced goes with it rather than lingering unreferenced.
        verify(storageService).delete("organizations/before.png");
    }

    /**
     * The same three rules as the profile form, because they are the same rules:
     * both ways in run the one method.
     */
    @Test
    void thePhotoOnItsOwnFollowsTheSameThreeRules() {
        organizationProfileService.updatePhoto(SLUG, null, false);
        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey())
                .isEqualTo("organizations/before.png");

        organizationProfileService.updatePhoto(SLUG, "organizations/second.png", true);
        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey())
                .isEqualTo("organizations/second.png");

        organizationProfileService.updatePhoto(SLUG, null, true);
        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey()).isNull();
        verify(storageService).delete("organizations/second.png");
    }

    @Test
    void everyProfileFieldIsUpdated() {
        organizationProfileService.update(SLUG, changed(), null);

        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(SLUG).orElseThrow();
        assertThat(tenant.getDisplayName()).isEqualTo("Renamed Clinic");
        assertThat(tenant.getBusinessEmail()).isEqualTo("after@example.test");
        assertThat(tenant.getContactFirstName()).isEqualTo("After");
        assertThat(tenant.getContactLastName()).isEqualTo("Person");
        assertThat(tenant.getJobTitle()).isEqualTo("Practice Manager");
        assertThat(tenant.getPhoneNumber()).isEqualTo("+62 811 0000 0002");
        assertThat(tenant.getOrgStructure()).isEqualTo(OrgStructure.MULTI_LOCATION_HOSPITAL);
        assertThat(tenant.getPracticeSpeciality()).isEqualTo(PracticeSpeciality.DENTAL);
    }

    /**
     * The whole point of the restriction: rows are routed by the slug, a database
     * cannot be renamed under running connections, and the subdomain may already
     * be in somebody's bookmarks.
     */
    @Test
    void renamingTheBusinessDoesNotMoveTheTenant() {
        organizationProfileService.update(SLUG, changed(), null);

        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(SLUG).orElseThrow();
        assertThat(tenant.getSlug()).isEqualTo(SLUG);
        assertThat(tenant.getDatabaseName()).isEqualTo(SLUG);
        assertThat(tenant.getSubdomain()).isEqualTo(SLUG + ".jvm.my.id");
        assertThat(tenant.getOwner().getEmail()).isEqualTo(OWNER_EMAIL);
    }

    @Test
    void aNewPhotoReplacesTheOldOneAndDeletesIt() {
        organizationProfileService.update(SLUG, changed(), "organizations/after.png");

        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey())
                .isEqualTo("organizations/after.png");
        // Otherwise every edit leaves an object nothing points at.
        verify(storageService).delete("organizations/before.png");
    }

    @Test
    void withoutANewPhotoTheCurrentOneStays() {
        organizationProfileService.update(SLUG, changed(), null);

        assertThat(tenantRegistrationRepository.findBySlug(SLUG).orElseThrow().getPhotoKey())
                .isEqualTo("organizations/before.png");
        verify(storageService, never()).delete("organizations/before.png");
    }

    @Test
    void anUnknownOrganizationIsRefused() {
        assertThatThrownBy(() -> organizationProfileService.update("nosuchtenant", changed(), null))
                .isInstanceOf(TenantProvisioningException.class)
                .hasMessageContaining("not registered");
    }
}
