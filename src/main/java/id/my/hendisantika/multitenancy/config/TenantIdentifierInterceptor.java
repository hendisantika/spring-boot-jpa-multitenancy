package id.my.hendisantika.multitenancy.config;

import id.my.hendisantika.multitenancy.entity.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

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
public class TenantIdentifierInterceptor extends WebRequestHandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object hadler) {
        String tenant = request.getParameter("tenant");
        TenantContext.setTenant(Tenant.findByName(tenant));
        return true;
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, @Nullable Exception ex) throws Exception {
        TenantContext.clearTenant();
    }
}
