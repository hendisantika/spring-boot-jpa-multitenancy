package id.my.hendisantika.multitenancy.service;

import java.util.Collection;
import java.util.List;

/**
 * The coded fields a caller may narrow a list of business units by. Each is a
 * code from the tenant's own reference lists, or nothing for "any".
 * <p>
 * Province takes several at once, because there are 38 of them and "the Bali
 * and Jawa Barat branches" is a real question. Several values within one filter
 * mean <em>either</em>; separate filters still mean <em>both</em>. The others
 * hold a handful of values each, where picking two is close enough to picking
 * none that it is not worth the extra control.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 01/08/26
 * Time: 09.16
 */
public record UnitFilter(String unitType, String operatingStatus, List<String> provinces) {

    public static UnitFilter of(String unitType, String operatingStatus, Collection<String> provinces) {
        return new UnitFilter(
                TenantListing.filterCode(unitType),
                TenantListing.filterCode(operatingStatus),
                TenantListing.filterCodes(provinces));
    }

    public static UnitFilter none() {
        return new UnitFilter(null, null, List.of());
    }

    /** Whether the province filter is switched off, which is what "any" means. */
    public boolean anyProvince() {
        return provinces.isEmpty();
    }
}
