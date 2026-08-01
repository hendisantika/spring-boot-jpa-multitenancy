package id.my.hendisantika.multitenancy.service;

import java.util.Collection;
import java.util.List;

/**
 * The coded fields a caller may narrow a list of people by. Each is a code from
 * the tenant's own reference lists, or nothing for "any".
 * <p>
 * A filter is not a search. The search widens — one term against every field,
 * matched loosely — while each filter narrows, on one field, exactly. They
 * combine, so a search for "budi" with a blood type of O+ means both.
 * <p>
 * Blood type takes several at once, because "O+ or O−" is a real question in a
 * clinic. Several values within one filter mean <em>either</em>; separate
 * filters still mean <em>both</em>.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 01/08/26
 * Time: 09.14
 */
public record PersonFilter(String gender, String maritalStatus, List<String> bloodTypes,
                           String identityDocumentType) {

    /**
     * An unknown code is left alone rather than refused: it simply matches
     * nothing, which is the honest answer to "show me the people whose blood
     * type is one this organization does not keep".
     */
    public static PersonFilter of(String gender, String maritalStatus, Collection<String> bloodTypes,
                                  String identityDocument) {
        return new PersonFilter(
                TenantListing.filterCode(gender),
                TenantListing.filterCode(maritalStatus),
                TenantListing.filterCodes(bloodTypes),
                TenantListing.filterCode(identityDocument));
    }

    public static PersonFilter none() {
        return new PersonFilter(null, null, List.of(), null);
    }

    /** Whether the blood type filter is switched off, which is what "any" means. */
    public boolean anyBloodType() {
        return bloodTypes.isEmpty();
    }
}
