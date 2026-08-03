package id.my.hendisantika.multitenancy.controller;

import id.my.hendisantika.multitenancy.entity.tenant.Organization;
import id.my.hendisantika.multitenancy.service.storage.StorageService;

/**
 * A business unit as a client sees one.
 * <p>
 * The entity is no longer handed out directly, for the same reason a person is
 * not: it carries {@code photoKey}, which is storage rather than something a
 * client should hold, and what a client needs instead is a signed URL that has
 * to be built per response.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 03/08/26
 * Time: 10.54
 */
public record UnitView(
        Long id,
        String name,
        String unitType,
        String operatingStatus,
        String address,
        String province,
        String email,
        String photoUrl
) {

    public static UnitView of(Organization organization, StorageService storageService) {
        return new UnitView(
                organization.getId(),
                organization.getName(),
                organization.getUnitType(),
                organization.getOperatingStatus(),
                organization.getAddress(),
                organization.getProvince(),
                organization.getEmail(),
                storageService.urlOf(organization.getPhotoKey()));
    }
}
