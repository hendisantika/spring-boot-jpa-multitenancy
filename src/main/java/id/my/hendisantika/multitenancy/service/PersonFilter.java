package id.my.hendisantika.multitenancy.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The coded fields a caller may narrow a list of people by. Each is a code from
 * the tenant's own reference lists, or nothing for "any".
 * <p>
 * A filter is not a search. The search widens — one term against every field,
 * matched loosely — while each filter narrows, on one field, exactly. They
 * combine, so a search for "budi" with a blood type of O+ means both.
 * <p>
 * Every list takes several at once — "O+ or O−", "KTP or Kartu Keluarga",
 * "single or widowed" are ordinary questions. Several values within one filter
 * mean <em>either</em>; separate filters still mean <em>both</em>.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 01/08/26
 * Time: 09.14
 */
public record PersonFilter(List<String> genders, List<String> maritalStatuses,
                           List<String> bloodTypes, List<String> identityDocumentTypes,
                           List<Long> unitIds) {

    /**
     * An unknown code is left alone rather than refused: it simply matches
     * nothing, which is the honest answer to "show me the people whose blood
     * type is one this organization does not keep".
     */
    public static PersonFilter of(Collection<String> genders, Collection<String> maritalStatuses,
                                  Collection<String> bloodTypes, Collection<String> identityDocuments,
                                  Collection<String> units) {
        return new PersonFilter(
                TenantListing.filterCodes(genders),
                TenantListing.filterCodes(maritalStatuses),
                TenantListing.filterCodes(bloodTypes),
                TenantListing.filterCodes(identityDocuments),
                unitIds(units));
    }

    public static PersonFilter none() {
        return new PersonFilter(List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /**
     * Ids no row can have, for a unit filter that was asked for in terms no
     * unit can satisfy. The codes do this with an empty string; ids are
     * positive, so a negative one is the same trick.
     */
    private static final List<Long> NO_UNIT = List.of(-1L);

    /**
     * A unit is a record, so it is asked for by id rather than by a code.
     * Anything that is not a number is dropped — but dropping all of them is
     * not the same as asking for none: "show me the people at unit banana"
     * answers nobody, the way an unknown blood type does.
     */
    private static List<Long> unitIds(Collection<String> units) {
        if (units == null || units.isEmpty()) {
            return List.of();
        }
        List<Long> parsed = units.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .distinct()
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        return parsed.isEmpty() ? NO_UNIT : parsed;
    }

    /** Whether the unit filter is switched off, which is what "any" means. */
    public boolean anyUnit() {
        return unitIds.isEmpty();
    }

    /** Whether the gender filter is switched off, which is what "any" means. */
    public boolean anyGender() {
        return genders.isEmpty();
    }

    /** Whether the marital status filter is switched off. */
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
