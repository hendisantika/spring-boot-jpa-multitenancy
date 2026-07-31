package id.my.hendisantika.multitenancy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Resolves the tenant for every request, then checks the caller is allowed to use
 * it. Order matters: the subdomain has to be resolved before access to it can be
 * judged.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 06.09
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final TenantProperties tenantProperties;
    private final RateLimitProperties rateLimitProperties;

    @Bean
    public TenantSubdomainInterceptor tenantSubdomainInterceptor() {
        return new TenantSubdomainInterceptor(tenantProperties);
    }

    @Bean
    public TenantAccessInterceptor tenantAccessInterceptor() {
        return new TenantAccessInterceptor();
    }

    /**
     * Runs before the interceptors, because the address it extracts is what they
     * key on.
     */
    @Bean
    public FilterRegistrationBean<RateLimitBodyFilter> rateLimitBodyFilter(ObjectMapper objectMapper) {
        FilterRegistrationBean<RateLimitBodyFilter> registration =
                new FilterRegistrationBean<>(new RateLimitBodyFilter(objectMapper));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    private RateLimiter limiterFor(RateLimitProperties.Limit limit) {
        return new RateLimiter(limit.getCapacity(), limit.getWindow(),
                rateLimitProperties.getMaxKeys(), Clock.systemUTC());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitProperties.isEnabled()) {
            // Only failed sign-ins count, so the right password never locks anyone out.
            registry.addInterceptor(new RateLimitInterceptor(
                            limiterFor(rateLimitProperties.getLogin()), "login", true))
                    .addPathPatterns("/api/auth/login");
            // Every request costs here, because each one sends mail.
            registry.addInterceptor(new RateLimitInterceptor(
                            limiterFor(rateLimitProperties.getForgotPassword()), "forgot-password", false))
                    .addPathPatterns("/api/auth/password/forgot");
        }

        registry.addInterceptor(tenantSubdomainInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(tenantAccessInterceptor())
                .addPathPatterns("/**")
                // Signing in has to work from a tenant subdomain, before the caller
                // holds a token that proves the membership.
                .excludePathPatterns("/api/auth/**");
    }
}
