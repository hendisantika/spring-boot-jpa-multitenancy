package id.my.hendisantika.multitenancy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public TenantSubdomainInterceptor tenantSubdomainInterceptor() {
        return new TenantSubdomainInterceptor(tenantProperties);
    }

    @Bean
    public TenantAccessInterceptor tenantAccessInterceptor() {
        return new TenantAccessInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantSubdomainInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(tenantAccessInterceptor())
                .addPathPatterns("/**")
                // Signing in has to work from a tenant subdomain, before the caller
                // holds a token that proves the membership.
                .excludePathPatterns("/api/auth/**");
    }
}
