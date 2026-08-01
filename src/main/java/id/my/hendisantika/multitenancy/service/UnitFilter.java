package id.my.hendisantika.multitenancy.service;

import java.util.Collection;
import java.util.List;

/**
 * The coded fields a caller may narrow a list of business units by. Each is a
 * code from the tenant's own reference lists, or nothing for "any".
 * <p>
 * All three take several at once — "the Bali and Jawa Barat branches", "the
 * clinics and the pharmacies", "closed for now or for good" are ordinary
 * questions. Several values within one filter mean <em>either</em>; separate
 * filters still mean <em>both</em>.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 01/08/26
 * Time: 09.16
 */
public record UnitFilter(List<String> unitTypes, List<String> operatingStatuses,
                         List<String> provinces) {

    public static UnitFilter of(Collection<String> unitTypes, Collection<String> operatingStatuses,
                                Collection<String> provinces) {
        return new UnitFilter(
                TenantListing.filterCodes(unitTypes),
                TenantListing.filterCodes(operatingStatuses),
                TenantListing.filterCodes(provinces));
    }

    public static UnitFilter none() {
        return new UnitFilter(List.of(), List.of(), List.of());
    }

    /** Whether the unit type filter is switched off, which is what "any" means. */
    public boolean anyUnitType() {
        return unitTypes.isEmpty();
    }

    /** Whether the operating status filter is switched off. */
    public boolean anyOperatingStatus() {
        return operatingStatuses.isEmpty();
    }

    /** Whether the province filter is switched off, which is what "any" means. */
    public boolean anyProvince() {
        return provinces.isEmpty();
    }
}
