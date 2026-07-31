package id.my.hendisantika.multitenancy.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    public static Pageable pageRequest(Integer page, Integer size) {
        int number = page == null ? 0 : Math.max(0, page);
        int length = size == null ? DEFAULT_PAGE_SIZE : Math.clamp(size, 1, MAX_PAGE_SIZE);
        return PageRequest.of(number, length, ORDER);
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
