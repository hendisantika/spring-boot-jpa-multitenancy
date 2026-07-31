package id.my.hendisantika.multitenancy.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A token bucket per key, held in this process.
 * <p>
 * Continuous refill rather than a fixed window, so a caller cannot spend a whole
 * window's allowance twice by straddling the boundary.
 * <p>
 * <strong>Per instance.</strong> Two instances behind a load balancer each allow
 * the configured rate, so this raises the cost of guessing rather than capping it
 * globally. Doing that properly needs shared state, which this project does not
 * have yet.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.41
 */
public class InMemoryRateLimiter implements RateLimiter {

    private final int capacity;
    private final double refillPerMilli;
    private final long refillMillis;
    private final int maxKeys;
    private final Clock clock;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param capacity how many requests may be made back to back
     * @param window   how long a full bucket takes to refill from empty
     * @param maxKeys  ceiling on tracked keys, so spraying unique addresses cannot
     *                 grow this without bound
     */
    public InMemoryRateLimiter(int capacity, Duration window, int maxKeys, Clock clock) {
        this.capacity = capacity;
        this.refillMillis = Math.max(1, window.toMillis());
        this.refillPerMilli = (double) capacity / this.refillMillis;
        this.maxKeys = maxKeys;
        this.clock = clock;
    }

    /**
     * @return how long to wait before retrying, or {@link Duration#ZERO} when the
     * request is allowed
     */
    @Override
    public Duration consume(String key) {
        long now = clock.millis();
        evictIfCrowded();

        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity, now));
        synchronized (bucket) {
            bucket.refill(now);
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return Duration.ZERO;
            }
            // Time until one whole token exists again.
            double missing = 1 - bucket.tokens;
            return Duration.ofMillis((long) Math.ceil(missing / refillPerMilli));
        }
    }

    /**
     * Puts a token back, so an allowed-but-successful request need not cost
     * anything.
     */
    @Override
    public void refund(String key) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            return;
        }
        synchronized (bucket) {
            bucket.tokens = Math.min(capacity, bucket.tokens + 1);
        }
    }

    /**
     * Drops buckets that have refilled completely: they carry no state worth
     * keeping, and this is what stops unique-key spraying from filling memory.
     */
    private void evictIfCrowded() {
        if (buckets.size() <= maxKeys) {
            return;
        }
        long now = clock.millis();
        buckets.entrySet().removeIf(entry -> {
            Bucket bucket = entry.getValue();
            synchronized (bucket) {
                bucket.refill(now);
                return bucket.tokens >= capacity;
            }
        });
    }

    private final class Bucket {
        private double tokens;
        private long lastRefill;

        private Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefill = now;
        }

        private void refill(long now) {
            long elapsed = now - lastRefill;
            if (elapsed > 0) {
                tokens = Math.min(capacity, tokens + elapsed * refillPerMilli);
                lastRefill = now;
            }
        }
    }
}
