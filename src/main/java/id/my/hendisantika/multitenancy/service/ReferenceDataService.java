package id.my.hendisantika.multitenancy.service;

import id.my.hendisantika.multitenancy.entity.tenant.ReferenceData;
import id.my.hendisantika.multitenancy.repository.tenant.ReferenceDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The lists a tenant fills its forms from. Read only: these arrive by migration,
 * and changing them is not something this project offers yet.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 18.40
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final ReferenceDataRepository referenceDataRepository;

    /**
     * Every list at once, keyed by category. A form usually needs several of
     * them, so fetching them one request at a time would be the wrong shape.
     * <p>
     * Switched-off values are included, carrying {@code active: false}. A record
     * written before a value was retired still holds that code, and a client
     * that never saw the row could only show the raw code back — which is
     * storage, not something anybody should read. Offering is the client's job;
     * refusing a retired code is this service's, in {@link #requireValidCode}.
     * <p>
     * The map keeps insertion order, and the query orders by category then by
     * sort order, so what a client renders is what a clinic expects to read.
     */
    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public Map<String, List<ReferenceData>> findAllByCategory() {
        Map<String, List<ReferenceData>> byCategory = new LinkedHashMap<>();
        for (ReferenceData value : referenceDataRepository.findAllByOrderByCategoryAscSortOrderAsc()) {
            byCategory.computeIfAbsent(value.getCategory(), key -> new ArrayList<>()).add(value);
        }
        return byCategory;
    }

    /**
     * An unknown category is an empty list rather than a 404: asking for a list
     * this tenant does not keep is not an error, it is an empty dropdown.
     */
    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public List<ReferenceData> findByCategory(String category) {
        return referenceDataRepository.findByCategoryOrderBySortOrderAsc(
                category == null ? "" : category.trim().toUpperCase());
    }

    /**
     * The codes in a list whose <em>label</em> somebody searching would have
     * meant.
     * <p>
     * A record stores {@code BRANCH_CLINIC}; a person types "branch clinic", or
     * "Bali". So a search is resolved to codes here and the query matches those,
     * rather than matching the stored code against what was typed.
     * <p>
     * The label only, never the code. A code is storage — it is never shown, so
     * nobody is searching for one — and codes contain underscores, so matching
     * them would make a typed {@code _} find nearly every record while the
     * free-text half of the same search treats it as a literal.
     * <p>
     * Matched in Java rather than in SQL because these lists are tiny — 38 rows
     * at the largest — and because {@code contains} has no wildcards to escape,
     * so a typed {@code %} is a literal here exactly as it is everywhere else.
     * <p>
     * Retired values are included: a unit holding a code that is no longer
     * offered should still be findable by the label it was given.
     *
     * @return the matching codes, empty when nothing was typed or nothing matched
     */
    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public List<String> codesMatching(String category, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.strip().toLowerCase();
        return referenceDataRepository.findByCategoryOrderBySortOrderAsc(category).stream()
                .filter(value -> value.getLabel().toLowerCase().contains(needle))
                .map(ReferenceData::getCode)
                .toList();
    }

    /**
     * No code can equal this, so an {@code in} clause holding it is false rather
     * than the invalid — or, depending on who renders it, quietly true — SQL an
     * empty collection would produce.
     */
    private static final List<String> NO_CODES = List.of("");

    /**
     * {@link #codesMatching} shaped for a query parameter: never empty, so the
     * caller can drop it straight into an {@code in} clause.
     */
    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public List<String> codesForSearch(String category, String query) {
        List<String> codes = codesMatching(category, query);
        return codes.isEmpty() ? NO_CODES : codes;
    }

    /**
     * A dropdown is a courtesy to whoever is typing, not a guarantee about what
     * arrives: the same field can be posted with anything in it. So a stored
     * code is checked against the list it claims to come from.
     * <p>
     * Blank is allowed, because these fields are optional. What is refused is a
     * value that is not in the list, which would otherwise sit in the database
     * looking like data.
     *
     * @return the code, trimmed and upper-cased, or null when nothing was given
     */
    @Transactional(value = "tenantTransactionManager", readOnly = true)
    public String requireValidCode(String category, String code, String fieldName) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalised = code.trim().toUpperCase();
        boolean known = referenceDataRepository
                .findByCategoryAndActiveTrueOrderBySortOrderAsc(category)
                .stream()
                .anyMatch(value -> value.getCode().equals(normalised));
        if (!known) {
            throw new TenantRecordInvalidException(
                    "\"" + code + "\" is not one of the " + fieldName + " values this organization keeps");
        }
        return normalised;
    }
}
