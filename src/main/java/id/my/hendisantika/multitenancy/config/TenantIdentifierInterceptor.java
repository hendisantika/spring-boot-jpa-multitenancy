package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 2/17/24
 * Time: 13:53
 * To change this template use File | Settings | File Templates.
 */
public class TenantIdentifierInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenant = request.getParameter("tenant");
        TenantContext.setTenant(Tenant.findByName(tenant));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, @Nullable Exception ex) {
        TenantContext.clearTenant();
    }
}
