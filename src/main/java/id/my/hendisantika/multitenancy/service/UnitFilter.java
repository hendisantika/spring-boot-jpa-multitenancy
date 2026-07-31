package id.my.hendisantika.multitenancy.service;

/**
 * The coded fields a caller may narrow a list of business units by. Each is a
 * code from the tenant's own reference lists, or null for "any".
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 01/08/26
 * Time: 09.16
 */
public record UnitFilter(String unitType, String operatingStatus, String province) {

    public static UnitFilter of(String unitType, String operatingStatus, String province) {
        return new UnitFilter(
                TenantListing.filterCode(unitType),
                TenantListing.filterCode(operatingStatus),
                TenantListing.filterCode(province));
    }

    public static UnitFilter none() {
        return new UnitFilter(null, null, null);
    }
}
