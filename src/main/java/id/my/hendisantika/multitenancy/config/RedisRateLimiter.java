package id.my.hendisantika.multitenancy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

/**
 * A token bucket per key, held in Redis, so every instance draws from the same
 * allowance.
 * <p>
 * The whole read-modify-write runs inside one Lua script, because doing it in
 * Java would let two instances read the same count and both decide they are
 * under the limit.
 * <p>
 * Time comes from Redis rather than from the callers: instances whose clocks
 * disagree would otherwise refill each other's buckets by the difference.
 * <p>
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.53
 */
@Slf4j
public class RedisRateLimiter implements RateLimiter {

    /**
     * ARGV: capacity, tokens per millisecond, key lifetime in millis, 1 to
     * consume or -1 to refund. Returns 0 when allowed, otherwise the millis to
     * wait.
     */
    private static final String SCRIPT = """
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local ttl = tonumber(ARGV[3])
            local take = tonumber(ARGV[4])

            local time = redis.call('TIME')
            local now = (tonumber(time[1]) * 1000) + math.floor(tonumber(time[2]) / 1000)

            local stored = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
            local tokens = tonumber(stored[1])
            local ts = tonumber(stored[2])
            if tokens == nil or ts == nil then
              tokens = capacity
              ts = now
            end

            local elapsed = now - ts
            if elapsed > 0 then
              tokens = math.min(capacity, tokens + (elapsed * refill))
            end

            local wait = 0
            if take < 0 then
              tokens = math.min(capacity, tokens + 1)
            elseif tokens >= 1 then
              tokens = tokens - 1
            else
              wait = math.ceil((1 - tokens) / refill)
            end

            redis.call('HSET', KEYS[1], 'tokens', tokens, 'ts', now)
            redis.call('PEXPIRE', KEYS[1], ttl)
            return wait
            """;

    private static final RedisScript<Long> TOKEN_BUCKET = new DefaultRedisScript<>(SCRIPT, Long.class);
    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redis;
    private final int capacity;
    private final double refillPerMilli;
    private final long ttlMillis;

    public RedisRateLimiter(StringRedisTemplate redis, int capacity, Duration window) {
        this.redis = redis;
        this.capacity = capacity;
        long windowMillis = Math.max(1, window.toMillis());
        this.refillPerMilli = (double) capacity / windowMillis;
        // Twice the window: a bucket idle that long has refilled anyway, so
        // letting Redis drop it is the eviction story.
        this.ttlMillis = windowMillis * 2;
    }

    @Override
    public Duration consume(String key) {
        Long wait = run(key, 1);
        // A Redis outage must not lock everyone out of signing in, so this fails
        // open. The trade is that protection is lost exactly while Redis is down.
        return wait == null ? Duration.ZERO : Duration.ofMillis(wait);
    }

    @Override
    public void refund(String key) {
        run(key, -1);
    }

    private Long run(String key, int take) {
        try {
            return redis.execute(TOKEN_BUCKET, List.of(KEY_PREFIX + key),
                    String.valueOf(capacity),
                    String.valueOf(refillPerMilli),
                    String.valueOf(ttlMillis),
                    String.valueOf(take));
        } catch (RuntimeException e) {
            log.warn("Rate limiting is failing open: Redis is unreachable ({})", e.getMessage());
            return null;
        }
    }
}
