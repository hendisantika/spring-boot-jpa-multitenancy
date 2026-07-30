package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Enforces that the caller's token actually grants the tenant the request
 * resolved to, so a token minted for sehat cannot read sehat2 by swapping the
 * host name.
 * <p>
 * Runs as an MVC interceptor, which is after the security filter chain, so the
 * authentication is already established by the time it is consulted.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Slf4j
public class TenantAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenant = TenantContext.getTenant();
        if (tenant == null) {
            // A central request; URL level rules already decided whether it is allowed.
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }
        Map<String, Object> memberships = jwt.getClaimAsMap(TokenService.CLAIM_MEMBERSHIPS);
        if (memberships == null || !memberships.containsKey(tenant)) {
            log.warn("Account {} tried to reach tenant {} without a membership", jwt.getSubject(), tenant);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return false;
        }
        return true;
    }
}
