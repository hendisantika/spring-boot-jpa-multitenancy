package id.my.hendisantika.multitenancy.service;

/**
 * The coded fields a caller may narrow a list of people by. Each is a code from
 * the tenant's own reference lists, or null for "any".
 * <p>
 * A filter is not a search. The search widens — one term against every field,
 * matched loosely — while each filter narrows, on one field, exactly. They
 * combine, so a search for "budi" with a blood type of O+ means both.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 01/08/26
 * Time: 09.14
 */
public record PersonFilter(String gender, String maritalStatus, String bloodType, String identityDocumentType) {

    /**
     * An unknown code is left alone rather than refused: it simply matches
     * nothing, which is the honest answer to "show me the people whose blood
     * type is one this organization does not keep".
     */
    public static PersonFilter of(String gender, String maritalStatus, String bloodType, String identityDocument) {
        return new PersonFilter(
                TenantListing.filterCode(gender),
                TenantListing.filterCode(maritalStatus),
                TenantListing.filterCode(bloodType),
                TenantListing.filterCode(identityDocument));
    }

    public static PersonFilter none() {
        return new PersonFilter(null, null, null, null);
    }
}
