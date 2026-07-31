package id.my.hendisantika.multitenancy.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises the Lua script against a real Redis, because the atomicity it exists
 * for cannot be shown any other way.
 * <p>
 * Skips when Redis is not answering, so nobody needs it running to work on the
 * rest. CI sets REDIS_INTEGRATION_REQUIRED, which turns a Redis that failed to
 * start into a failure rather than a silent skip.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.53
 */
@SpringBootTest
class RedisRateLimiterIntegrationTest {

    @Autowired
    private StringRedisTemplate redis;

    private String key;

    @BeforeEach
    void requireRedis() {
        boolean reachable = reachable();
        if (!reachable && "true".equalsIgnoreCase(System.getenv("REDIS_INTEGRATION_REQUIRED"))) {
            fail("REDIS_INTEGRATION_REQUIRED is set but Redis is not answering");
        }
        assumeTrue(reachable, "Redis is not answering, skipping");
        key = "test-" + UUID.randomUUID();
    }

    private boolean reachable() {
        try {
            redis.hasKey("ratelimit:probe");
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Test
    void allowsUpToCapacityThenRefuses() {
        RateLimiter limiter = new RedisRateLimiter(redis, 3, Duration.ofMinutes(1));

        assertThat(limiter.consume(key)).isZero();
        assertThat(limiter.consume(key)).isZero();
        assertThat(limiter.consume(key)).isZero();
        assertThat(limiter.consume(key)).isPositive();
    }

    /**
     * The point of moving the counters out of the process: two instances share
     * one allowance instead of each getting their own.
     */
    @Test
    void twoLimitersShareOneAllowance() {
        RateLimiter first = new RedisRateLimiter(redis, 4, Duration.ofMinutes(1));
        RateLimiter second = new RedisRateLimiter(redis, 4, Duration.ofMinutes(1));

        assertThat(first.consume(key)).isZero();
        assertThat(second.consume(key)).isZero();
        assertThat(first.consume(key)).isZero();
        assertThat(second.consume(key)).isZero();

        // Four spent between them, so both are now refused.
        assertThat(first.consume(key)).isPositive();
        assertThat(second.consume(key)).isPositive();
    }

    @Test
    void keysAreIndependent() {
        RateLimiter limiter = new RedisRateLimiter(redis, 1, Duration.ofMinutes(1));

        assertThat(limiter.consume(key)).isZero();
        assertThat(limiter.consume(key)).isPositive();
        assertThat(limiter.consume(key + "-other")).isZero();
    }

    @Test
    void refundingPutsATokenBackForEveryInstance() {
        RateLimiter first = new RedisRateLimiter(redis, 1, Duration.ofMinutes(1));
        RateLimiter second = new RedisRateLimiter(redis, 1, Duration.ofMinutes(1));

        assertThat(first.consume(key)).isZero();
        assertThat(second.consume(key)).isPositive();

        first.refund(key);
        assertThat(second.consume(key)).isZero();
    }

    @Test
    void refundingNeverExceedsCapacity() {
        RateLimiter limiter = new RedisRateLimiter(redis, 1, Duration.ofMinutes(1));
        limiter.consume(key);
        limiter.refund(key);
        limiter.refund(key);
        limiter.refund(key);

        assertThat(limiter.consume(key)).isZero();
        assertThat(limiter.consume(key)).isPositive();
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        // Two seconds for the whole allowance, so a second buys one back.
        RateLimiter limiter = new RedisRateLimiter(redis, 2, Duration.ofSeconds(2));
        limiter.consume(key);
        limiter.consume(key);
        assertThat(limiter.consume(key)).isPositive();

        Thread.sleep(1100);
        assertThat(limiter.consume(key)).isZero();
    }

    /**
     * Concurrent callers must not both get the last token, which is what doing
     * the whole read-modify-write inside the script buys.
     */
    @Test
    void concurrentCallersCannotOverspend() throws InterruptedException {
        int capacity = 20;
        int threads = 40;
        RateLimiter limiter = new RedisRateLimiter(redis, capacity, Duration.ofMinutes(5));

        var allowed = new java.util.concurrent.atomic.AtomicInteger();
        var start = new java.util.concurrent.CountDownLatch(1);
        var done = new java.util.concurrent.CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    start.await();
                    if (limiter.consume(key).isZero()) {
                        allowed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(30, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        assertThat(allowed.get()).isEqualTo(capacity);
    }

    @Test
    void theKeyExpiresSoIdleBucketsDoNotAccumulate() {
        RateLimiter limiter = new RedisRateLimiter(redis, 2, Duration.ofSeconds(30));
        limiter.consume(key);

        Long ttl = redis.getExpire("ratelimit:" + key);
        assertThat(ttl).isPositive();
        // Twice the window, so an idle bucket disappears once it has refilled.
        assertThat(ttl).isLessThanOrEqualTo(60);
    }
}
