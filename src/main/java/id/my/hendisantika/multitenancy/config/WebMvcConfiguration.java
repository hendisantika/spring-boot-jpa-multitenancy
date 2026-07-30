package id.my.hendisantika.multitenancy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link TenantIdentifierInterceptor} so that the {@code tenant} request
 * parameter is resolved into {@link TenantContext} for every incoming request.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Bean
    public TenantIdentifierInterceptor tenantIdentifierInterceptor() {
        return new TenantIdentifierInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantIdentifierInterceptor()).addPathPatterns("/**");
    }
}
