package id.my.hendisantika.multitenancy.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Reads the email out of the body of the rate limited endpoints and hands it to
 * the interceptor, then replays the body so the controller still sees it.
 * <p>
 * Rate limiting has to happen per address as well as per client, and the address
 * only exists in the body, which can otherwise be read once.
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
public class RateLimitBodyFilter extends OncePerRequestFilter {

    public static final String EMAIL_ATTRIBUTE = RateLimitBodyFilter.class.getName() + ".email";

    /**
     * These bodies are two short fields; anything larger is not one of them.
     */
    private static final int MAX_BODY_BYTES = 8 * 1024;

    private static final Set<String> PATHS = Set.of("/api/auth/login", "/api/auth/password/forgot");

    private final ObjectMapper objectMapper;

    public RateLimitBodyFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PATHS.contains(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES);
        try {
            String email = objectMapper.readTree(body).path("email").asString(null);
            if (email != null) {
                request.setAttribute(EMAIL_ATTRIBUTE, email);
            }
        } catch (RuntimeException e) {
            // A malformed body is the controller's problem to report, not this
            // filter's: it just means there is no address to key on.
            log.debug("Could not read an email out of the request body");
        }
        chain.doFilter(new CachedBodyRequest(request, body), response);
    }

    /**
     * Serves the bytes already read, so nothing downstream notices.
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream source = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return source.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return source.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
