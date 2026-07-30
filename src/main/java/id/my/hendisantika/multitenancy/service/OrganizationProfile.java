package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.central.OrgStructure;
import id.my.hendisantika.multitenancy.entity.central.PracticeSpeciality;

/**
 * The organization registration form, as the service layer sees it. Keeps the
 * web DTO out of the provisioning code.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.28
 */
public record OrganizationProfile(
        String businessName,
        String businessEmail,
        String photoKey,
        String contactFirstName,
        String contactLastName,
        String jobTitle,
        String phoneNumber,
        OrgStructure orgStructure,
        PracticeSpeciality practiceSpeciality
) {

    /**
     * The business name alone is enough to provision; the rest of the form is
     * profile detail.
     */
    public static OrganizationProfile ofName(String businessName) {
        return new OrganizationProfile(businessName, null, null, null, null, null, null, null, null);
    }
}
