package id.my.hendisantika.multitenancy.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link TenantSubdomainInterceptor} so that every request resolves its
 * tenant from the host name before a handler runs.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final TenantProperties tenantProperties;

    @Bean
    public TenantSubdomainInterceptor tenantSubdomainInterceptor() {
        return new TenantSubdomainInterceptor(tenantProperties);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantSubdomainInterceptor()).addPathPatterns("/**");
    }
}
