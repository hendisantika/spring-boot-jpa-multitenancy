package id.my.hendisantika.multitenancy.service;

import java.util.Collection;
import java.util.List;

/**
 * The coded fields a caller may narrow a list of business units by. Each is a
 * code from the tenant's own reference lists, or nothing for "any".
 * <p>
 * Province and unit type take several at once — "the Bali and Jawa Barat
 * branches", "the clinics and the pharmacies" are ordinary questions. Several
 * values within one filter mean <em>either</em>; separate filters still mean
 * <em>both</em>.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 01/08/26
 * Time: 09.16
 */
public record UnitFilter(List<String> unitTypes, String operatingStatus, List<String> provinces) {

    public static UnitFilter of(Collection<String> unitTypes, String operatingStatus,
                                Collection<String> provinces) {
        return new UnitFilter(
                TenantListing.filterCodes(unitTypes),
                TenantListing.filterCode(operatingStatus),
                TenantListing.filterCodes(provinces));
    }

    public static UnitFilter none() {
        return new UnitFilter(List.of(), null, List.of());
    }

    /** Whether the unit type filter is switched off, which is what "any" means. */
    public boolean anyUnitType() {
        return unitTypes.isEmpty();
    }

    /** Whether the province filter is switched off, which is what "any" means. */
    public boolean anyProvince() {
        return provinces.isEmpty();
    }
}
