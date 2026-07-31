package id.my.hendisantika.multitenancy.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Chooses where the counters live, once, at startup.
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
@Component
public class RateLimiterFactory {

    private final RateLimitProperties properties;
    private final StringRedisTemplate redis;
    private final boolean useRedis;

    public RateLimiterFactory(RateLimitProperties properties,
                              ObjectProvider<StringRedisTemplate> redisTemplate) {
        this.properties = properties;
        this.redis = redisTemplate.getIfAvailable();
        this.useRedis = decide();
    }

    public RateLimiter create(RateLimitProperties.Limit limit) {
        if (useRedis) {
            return new RedisRateLimiter(redis, limit.getCapacity(), limit.getWindow());
        }
        return new InMemoryRateLimiter(limit.getCapacity(), limit.getWindow(),
                properties.getMaxKeys(), Clock.systemUTC());
    }

    /**
     * AUTO pings Redis once rather than assuming: the starter auto-configures a
     * template whether or not a server is listening, so the bean existing proves
     * nothing.
     */
    private boolean decide() {
        return switch (properties.getBackend()) {
            case MEMORY -> {
                log.info("Rate limit counters are per instance, by configuration");
                yield false;
            }
            case REDIS -> {
                if (!reachable()) {
                    throw new IllegalStateException(
                            "application.rate-limit.backend is REDIS but Redis is unreachable");
                }
                log.info("Rate limit counters are shared through Redis");
                yield true;
            }
            case AUTO -> {
                boolean up = reachable();
                log.info(up
                        ? "Rate limit counters are shared through Redis"
                        : "Redis is not answering; rate limit counters are per instance");
                yield up;
            }
        };
    }

    private boolean reachable() {
        if (redis == null) {
            return false;
        }
        try {
            redis.hasKey("ratelimit:probe");
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
