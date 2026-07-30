package id.my.hendisantika.multitenancy.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 */
class TenantSubdomainInterceptorTest {

    private final TenantSubdomainInterceptor interceptor =
            new TenantSubdomainInterceptor(new TenantProperties());

    @AfterEach
    void clear() {
        TenantContext.clearTenant();
    }

    private String resolve(String host) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName(host);
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        return TenantContext.getTenant();
    }

    @Test
    void readsTenantFromSubdomain() {
        assertThat(resolve("sehat.mhdc.co.id")).isEqualTo("sehat");
        assertThat(resolve("sehat2.mhdc.co.id")).isEqualTo("sehat2");
        assertThat(resolve("SEHAT.MHDC.CO.ID")).isEqualTo("sehat");
    }

    @Test
    void apexAndNeutralHostsCarryNoTenant() {
        assertThat(resolve("mhdc.co.id")).isNull();
        assertThat(resolve("localhost")).isNull();
        assertThat(resolve("127.0.0.1")).isNull();
    }

    @Test
    void hostsOutsideTheBaseDomainCarryNoTenant() {
        assertThat(resolve("sehat.example.com")).isNull();
        // Nested labels are not a tenant, so evil.sehat.mhdc.co.id cannot pose as one.
        assertThat(resolve("evil.sehat.mhdc.co.id")).isNull();
    }

    @Test
    void headerOverridesHostForLocalDevelopment() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.addHeader(TenantSubdomainInterceptor.TENANT_HEADER, "Sehat");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        assertThat(TenantContext.getTenant()).isEqualTo("sehat");
    }

    @Test
    void clearsTenantAfterTheRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("sehat.mhdc.co.id");
        interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertThat(TenantContext.getTenant()).isNull();
    }
}
