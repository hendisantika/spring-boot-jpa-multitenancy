package id.my.hendisantika.multitenancy.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The rules every paged list inside a tenant's database shares.
 * <p>
 * They live here rather than in each service because two lists that clamp
 * differently, or escape differently, are two lists that behave differently for
 * no reason anybody could explain.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 15.02
 */
public final class TenantListing {

    /**
     * A page is a promise that asking for the next one shows different records,
     * which an unordered query cannot keep. Insertion order is the least
     * surprising of the deterministic ones.
     */
    public static final Sort ORDER = Sort.by(Sort.Direction.ASC, "id");

    /**
     * High enough that nobody paging through a clinic notices it, low enough
     * that one request cannot ask for the whole database.
     */
    public static final int MAX_PAGE_SIZE = 200;

    public static final int DEFAULT_PAGE_SIZE = 20;

    private TenantListing() {
    }

    /**
     * A term that matches everything, for when nothing was searched for. It lets
     * one query serve both cases rather than branching, which matters now that
     * filters can apply with no search term at all.
     */
    public static final String MATCH_EVERYTHING = "%";

    /**
     * A filter value as it should be compared: trimmed, upper-cased to match how
     * codes are stored, and null when nothing was chosen.
     */
    public static String filterCode(String value) {
        return value == null || value.isBlank() ? null : value.strip().toUpperCase();
    }

    /**
     * The same for a filter that accepts several values at once. Blanks are
     * dropped and duplicates collapsed, so an empty list means "any" exactly as
     * a null single value does.
     */
    public static List<String> filterCodes(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(TenantListing::filterCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * A collection for an {@code in} clause that is never empty. Which value it
     * holds does not matter when the clause is disabled by its "any" flag, but
     * an empty one would be invalid — or, depending on who renders it, quietly
     * true — so it is never allowed to happen.
     */
    public static List<String> orNothing(List<String> codes) {
        return codes.isEmpty() ? List.of("") : codes;
    }

    public static Pageable pageRequest(Integer page, Integer size) {
        return pageRequest(page, size, ORDER);
    }

    /**
     * The same clamping, for a list whose order is part of what it means.
     * Invitations read newest first, and paging them by id would have quietly
     * turned that into oldest first.
     */
    public static Pageable pageRequest(Integer page, Integer size, Sort order) {
        int number = page == null ? 0 : Math.max(0, page);
        int length = size == null ? DEFAULT_PAGE_SIZE : Math.clamp(size, 1, MAX_PAGE_SIZE);
        return PageRequest.of(number, length, order);
    }

    /**
     * A blank box is not a search for nothing, it is everybody, so it answers
     * null and the caller skips the query.
     * <p>
     * The wildcards a user types are part of what they are looking for, not
     * part of the query, so they are escaped rather than honoured. The escape
     * character itself goes first, or escaping would corrupt what follows.
     */
    public static String searchTerm(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String escaped = query.strip()
                .toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
