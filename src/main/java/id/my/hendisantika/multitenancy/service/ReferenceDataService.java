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
