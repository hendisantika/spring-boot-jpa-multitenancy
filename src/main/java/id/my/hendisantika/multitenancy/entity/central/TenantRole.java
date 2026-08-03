package id.my.hendisantika.multitenancy.entity.central;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * What an account may do inside one tenant.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
public enum TenantRole {

    /**
     * Registered the organization; may create users inside it.
     */
    OWNER,

    /**
     * Created by the owner.
     */
    MEMBER;

    /**
     * The roles somebody searching would have meant, so "own" finds the owners.
     * Nobody types OWNER, and nobody should have to.
     * <p>
     * Resolved here rather than in a query, which would mean casting an enum to
     * text in HQL. Both lists that carry a role do this, so it lives with the
     * role rather than being written out twice.
     *
     * @return empty for a blank query, which the caller reads as "no role
     * widened the search" rather than as "no role matched"
     */
    public static List<TenantRole> matching(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(role -> role.name().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    /**
     * The roles among what was asked for as a filter, dropping anything that is
     * not one. Exact names, not substrings: a filter names what it wants, and
     * an unknown name narrows to nothing rather than being ignored.
     */
    public static List<TenantRole> parseAll(Collection<String> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.strip().toUpperCase(Locale.ROOT))
                .distinct()
                .flatMap(value -> Arrays.stream(values()).filter(role -> role.name().equals(value)))
                .toList();
    }
}
