package id.my.hendisantika.multitenancy.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

/**
 * Slows down guessing at the endpoints that are open by necessity.
 * <p>
 * Keyed by client address <em>and</em> by the address in the body. Either alone
 * leaves a hole: by IP only punishes everyone behind one NAT and lets a botnet
 * through, by email only lets one host work through a list of addresses.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.41
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String CONSUMED_KEYS = RateLimitInterceptor.class.getName() + ".keys";

    private final RateLimiter limiter;
    private final String name;

    /**
     * When true, a request that succeeded gives its token back, so only failures
     * count towards the limit.
     */
    private final boolean refundOnSuccess;

    public RateLimitInterceptor(RateLimiter limiter, String name, boolean refundOnSuccess) {
        this.limiter = limiter;
        this.name = name;
        this.refundOnSuccess = refundOnSuccess;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String ipKey = "ip:" + clientAddress(request);
        String emailKey = emailKey(request);

        Duration wait = limiter.consume(ipKey);
        Duration emailWait = emailKey == null ? Duration.ZERO : limiter.consume(emailKey);
        if (emailWait.compareTo(wait) > 0) {
            wait = emailWait;
        }

        if (!wait.isZero()) {
            log.warn("Rate limited a {} request from {}", name, clientAddress(request));
            tooManyRequests(response, wait);
            return false;
        }

        request.setAttribute(CONSUMED_KEYS, new String[]{ipKey, emailKey});
        return true;
    }

    /**
     * Gives the token back when the request worked, so a correct password never
     * counts against the caller.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, @Nullable Exception ex) {
        if (!refundOnSuccess || response.getStatus() >= HttpStatus.BAD_REQUEST.value()) {
            return;
        }
        Object consumed = request.getAttribute(CONSUMED_KEYS);
        if (consumed instanceof String[] keys) {
            for (String key : keys) {
                if (key != null) {
                    limiter.refund(key);
                }
            }
        }
    }

    private void tooManyRequests(HttpServletResponse response, Duration wait) throws IOException {
        long seconds = Math.max(1, wait.toSeconds());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Too many attempts. Try again in %d seconds."}"""
                .formatted(seconds));
    }

    /**
     * The email is read from a wrapper the filter chain installed, so the body can
     * still be read again by the controller.
     */
    private String emailKey(HttpServletRequest request) {
        Object email = request.getAttribute(RateLimitBodyFilter.EMAIL_ATTRIBUTE);
        if (email instanceof String value && StringUtils.hasText(value)) {
            return "email:" + name + ":" + value.trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    /**
     * X-Forwarded-For is trusted only for its first entry, and only because this
     * is expected to run behind a proxy that sets it.
     */
    private String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
