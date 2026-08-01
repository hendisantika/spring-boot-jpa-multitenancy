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
 * Every list but gender takes several at once — "O+ or O−", "KTP or Kartu
 * Keluarga", "single or widowed" are ordinary questions. Gender holds two
 * values, where choosing both is choosing neither.
 * <p>
 * Several values within one filter mean <em>either</em>; separate filters still
 * mean <em>both</em>.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 01/08/26
 * Time: 09.14
 */
public record PersonFilter(String gender, List<String> maritalStatuses, List<String> bloodTypes,
                           List<String> identityDocumentTypes) {

    /**
     * An unknown code is left alone rather than refused: it simply matches
     * nothing, which is the honest answer to "show me the people whose blood
     * type is one this organization does not keep".
     */
    public static PersonFilter of(String gender, Collection<String> maritalStatuses,
                                  Collection<String> bloodTypes, Collection<String> identityDocuments) {
        return new PersonFilter(
                TenantListing.filterCode(gender),
                TenantListing.filterCodes(maritalStatuses),
                TenantListing.filterCodes(bloodTypes),
                TenantListing.filterCodes(identityDocuments));
    }

    public static PersonFilter none() {
        return new PersonFilter(null, List.of(), List.of(), List.of());
    }

    /** Whether the marital status filter is switched off, which is what "any" means. */
    public boolean anyMaritalStatus() {
        return maritalStatuses.isEmpty();
    }

    /** Whether the blood type filter is switched off, which is what "any" means. */
    public boolean anyBloodType() {
        return bloodTypes.isEmpty();
    }

    /** Whether the identity document filter is switched off. */
    public boolean anyIdentityDocument() {
        return identityDocumentTypes.isEmpty();
    }
}
