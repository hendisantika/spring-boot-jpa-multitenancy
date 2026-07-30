package id.my.hendisantika.multitenancy.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

/**
 * Resolves the tenant for the current request from the host name, so
 * sehat.mhdc.co.id is served from the "sehat" database.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:53
 * To change this template use File | Settings | File Templates.
 */
@RequiredArgsConstructor
public class TenantSubdomainInterceptor implements HandlerInterceptor {

    /**
     * Overrides the host, for development and tests where wildcard DNS for
     * *.mhdc.co.id is not available.
     */
    public static final String TENANT_HEADER = "X-Tenant";

    private final TenantProperties tenantProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TenantContext.setTenant(resolve(request));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, @Nullable Exception ex) {
        TenantContext.clearTenant();
    }

    /**
     * @return the tenant slug, or {@code null} when the request targets the apex
     * domain or a neutral host such as localhost
     */
    private String resolve(HttpServletRequest request) {
        String header = request.getHeader(TENANT_HEADER);
        if (StringUtils.hasText(header)) {
            return header.trim().toLowerCase(Locale.ROOT);
        }
        return subdomainOf(request.getServerName());
    }

    private String subdomainOf(String host) {
        if (!StringUtils.hasText(host)) {
            return null;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        if (tenantProperties.getNeutralHosts().contains(normalized)) {
            return null;
        }
        String baseDomain = tenantProperties.getBaseDomain().toLowerCase(Locale.ROOT);
        if (normalized.endsWith("." + baseDomain)) {
            String label = normalized.substring(0, normalized.length() - baseDomain.length() - 1);
            // Only a single label counts, so a.b.mhdc.co.id is not a tenant.
            return label.contains(".") ? null : label;
        }
        // Hosts outside the configured base domain, including the apex itself,
        // carry no tenant.
        return null;
    }
}
