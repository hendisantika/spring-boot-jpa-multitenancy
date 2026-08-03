package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.central.TenantRegistration;
import id.my.hendisantika.multitenancy.repository.central.TenantRegistrationRepository;
import id.my.hendisantika.multitenancy.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Editing an organization's profile after it exists.
 * <p>
 * The slug, database name and subdomain are deliberately not editable. They are
 * the tenant's identity: rows are routed by the slug, a database cannot be
 * renamed underneath running connections, and a subdomain may already be in
 * somebody's bookmarks. Changing the business name changes the label only.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationProfileService {

    private final TenantRegistrationRepository tenantRegistrationRepository;
    private final StorageService storageService;

    /**
     * @param newPhotoKey the freshly stored photo, or null to keep the current one
     */
    @Transactional("centralTransactionManager")
    public TenantRegistration update(String slug, OrganizationProfile profile, String newPhotoKey) {
        return update(slug, profile, newPhotoKey, false);
    }

    /**
     * @param removePhoto drops the current photo, for whoever wants none rather
     *                    than a different one. A supplied photo wins over it.
     */
    // Annotated as well as the overload above: a caller reaching this one
    // directly — which the controller now does — would otherwise run with no
    // transaction and see its changes quietly dropped.
    @Transactional("centralTransactionManager")
    public TenantRegistration update(String slug, OrganizationProfile profile, String newPhotoKey,
                                     boolean removePhoto) {
        TenantRegistration tenant = tenantRegistrationRepository.findBySlug(slug)
                .orElseThrow(() -> new TenantProvisioningException("'" + slug + "' is not registered"));

        tenant.setDisplayName(profile.businessName());
        tenant.setBusinessEmail(profile.businessEmail());
        tenant.setContactFirstName(profile.contactFirstName());
        tenant.setContactLastName(profile.contactLastName());
        tenant.setJobTitle(profile.jobTitle());
        tenant.setPhoneNumber(profile.phoneNumber());
        tenant.setOrgStructure(profile.orgStructure());
        tenant.setPracticeSpeciality(profile.practiceSpeciality());

        String previous = tenant.getPhotoKey();
        if (StringUtils.hasText(newPhotoKey)) {
            tenant.setPhotoKey(newPhotoKey);
            // Otherwise every edit leaves an orphan in the bucket that nothing
            // will ever point at again.
            if (StringUtils.hasText(previous) && !previous.equals(newPhotoKey)) {
                storageService.delete(previous);
            }
        } else if (removePhoto && StringUtils.hasText(previous)) {
            tenant.setPhotoKey(null);
            storageService.delete(previous);
        }

        log.info("Updated the profile of tenant {}", slug);
        return tenant;
    }
}
