package id.my.hendisantika.multitenancy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.41
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "application.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /**
     * Ceiling on tracked keys per limiter, so spraying unique addresses cannot
     * grow memory without bound.
     */
    private int maxKeys = 50_000;

    /**
     * Only failed sign-ins are counted, so somebody with the right password is
     * never locked out by using it.
     */
    private Limit login = new Limit(10, Duration.ofMinutes(5));

    /**
     * Every request costs, because each one sends mail.
     */
    private Limit forgotPassword = new Limit(5, Duration.ofMinutes(15));

    @Getter
    @Setter
    public static class Limit {

        /**
         * How many requests may be made back to back.
         */
        private int capacity;

        /**
         * How long a full allowance takes to come back.
         */
        private Duration window;

        public Limit() {
        }

        public Limit(int capacity, Duration window) {
            this.capacity = capacity;
            this.window = window;
        }
    }
}
