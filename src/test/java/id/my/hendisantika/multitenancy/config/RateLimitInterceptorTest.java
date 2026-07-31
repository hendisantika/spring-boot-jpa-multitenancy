package id.my.hendisantika.multitenancy.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.53
 */
class RateLimitInterceptorTest {

    private final RateLimiter shared =
            new InMemoryRateLimiter(4, Duration.ofMinutes(5), 100, Clock.systemUTC());

    private MockHttpServletRequest request(String ip, String email) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(ip);
        if (email != null) {
            request.setAttribute(RateLimitBodyFilter.EMAIL_ATTRIBUTE, email);
        }
        return request;
    }

    private int statusAfter(RateLimitInterceptor interceptor, String ip, String email) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        interceptor.preHandle(request(ip, email), response, new Object());
        return response.getStatus();
    }

    /**
     * Two limiters sharing one bucket is exactly the bug this guards: each would
     * write it with its own capacity, so neither limit would be the configured
     * one.
     */
    @Test
    void limitersDoNotShareABucketForTheSameAddress() throws Exception {
        RateLimitInterceptor login = new RateLimitInterceptor(shared, "login", true);
        RateLimitInterceptor forgot = new RateLimitInterceptor(shared, "forgot-password", false);

        for (int i = 0; i < 4; i++) {
            assertThat(statusAfter(login, "10.0.0.1", "a@example.test")).isEqualTo(200);
        }
        assertThat(statusAfter(login, "10.0.0.1", "a@example.test")).isEqualTo(429);

        // The other endpoint still has its own full allowance.
        assertThat(statusAfter(forgot, "10.0.0.1", "a@example.test")).isEqualTo(200);
    }

    @Test
    void differentAddressesAreCountedApart() throws Exception {
        RateLimitInterceptor login = new RateLimitInterceptor(shared, "login", true);

        for (int i = 0; i < 4; i++) {
            statusAfter(login, "10.0.0.2", "b@example.test");
        }
        assertThat(statusAfter(login, "10.0.0.2", "b@example.test")).isEqualTo(429);
        assertThat(statusAfter(login, "10.0.0.3", "c@example.test")).isEqualTo(200);
    }

    /**
     * One host must not get a fresh allowance simply by changing the address it
     * guesses at.
     */
    @Test
    void oneHostCannotEscapeTheLimitByChangingEmail() throws Exception {
        RateLimitInterceptor login = new RateLimitInterceptor(shared, "login", true);

        for (int i = 0; i < 4; i++) {
            statusAfter(login, "10.0.0.4", "victim" + i + "@example.test");
        }
        assertThat(statusAfter(login, "10.0.0.4", "another@example.test")).isEqualTo(429);
    }

    /**
     * And one address must not be hammered from many hosts.
     */
    @Test
    void oneAddressCannotBeAttackedFromManyHosts() throws Exception {
        RateLimitInterceptor login = new RateLimitInterceptor(shared, "login", true);

        for (int i = 0; i < 4; i++) {
            statusAfter(login, "10.1.0." + i, "target@example.test");
        }
        assertThat(statusAfter(login, "10.1.0.99", "target@example.test")).isEqualTo(429);
    }

    @Test
    void aRefusalCarriesRetryAfter() throws Exception {
        RateLimitInterceptor login = new RateLimitInterceptor(shared, "login", true);
        for (int i = 0; i < 4; i++) {
            statusAfter(login, "10.0.0.5", "d@example.test");
        }

        MockHttpServletResponse response = new MockHttpServletResponse();
        login.preHandle(request("10.0.0.5", "d@example.test"), response, new Object());

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(Integer.parseInt(response.getHeader("Retry-After"))).isPositive();
    }
}
