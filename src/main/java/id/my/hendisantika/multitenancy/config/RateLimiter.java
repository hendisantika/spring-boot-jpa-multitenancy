package id.my.hendisantika.multitenancy.config;

import java.time.Duration;

/**
 * A token bucket per key.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.53
 */
public interface RateLimiter {

    /**
     * @return how long to wait before retrying, or {@link Duration#ZERO} when the
     * request is allowed
     */
    Duration consume(String key);

    /**
     * Puts a token back, so an allowed-but-successful request need not cost
     * anything.
     */
    void refund(String key);
}
