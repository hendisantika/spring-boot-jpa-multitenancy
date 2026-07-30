package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.central.TenantRole;
import id.my.hendisantika.multitenancy.service.TokenService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Reads the caller's identity and tenant roles out of the validated token.
 * <p>
 * Roles are checked against the tenant named in the request rather than the one
 * resolved from the host, because organization administration happens on the
 * parent domain where no tenant is resolved.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.28
 */
@Component
public class TenantSecurity {

    public Jwt currentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("Not authenticated");
        }
        return jwt;
    }

    public Long currentAccountId() {
        return Long.valueOf(currentToken().getSubject());
    }

    public Optional<TenantRole> roleFor(String tenantSlug) {
        Map<String, Object> memberships = currentToken().getClaimAsMap(TokenService.CLAIM_MEMBERSHIPS);
        if (memberships == null) {
            return Optional.empty();
        }
        Object role = memberships.get(tenantSlug);
        return role == null ? Optional.empty() : Optional.of(TenantRole.valueOf(role.toString()));
    }

    public void requireMember(String tenantSlug) {
        roleFor(tenantSlug).orElseThrow(() ->
                new AccessDeniedException("You are not a member of '" + tenantSlug + "'"));
    }

    /**
     * Only the owner may administer an organization, which is what stops a member
     * from adding further members.
     */
    public void requireOwner(String tenantSlug) {
        TenantRole role = roleFor(tenantSlug).orElseThrow(() ->
                new AccessDeniedException("You are not a member of '" + tenantSlug + "'"));
        if (!TenantRole.OWNER.equals(role)) {
            throw new AccessDeniedException("Only the owner of '" + tenantSlug + "' may do that");
        }
    }
}
