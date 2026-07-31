package id.my.hendisantika.multitenancy.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-jpa-multitenancy
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 31/07/26
 * Time: 10.41
 */
class InMemoryRateLimiterTest {

    /**
     * A clock the test moves by hand, so nothing here waits on real time.
     */
    private static final class TestClock extends Clock {
        private final AtomicLong millis = new AtomicLong(1_000_000);

        @Override
        public long millis() {
            return millis.get();
        }

        void advance(Duration duration) {
            millis.addAndGet(duration.toMillis());
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }

    @Test
    void allowsUpToCapacityThenRefuses() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(3, Duration.ofMinutes(1), 100, new TestClock());

        assertThat(limiter.consume("a")).isZero();
        assertThat(limiter.consume("a")).isZero();
        assertThat(limiter.consume("a")).isZero();
        assertThat(limiter.consume("a")).isPositive();
    }

    @Test
    void refillsGraduallyRatherThanAllAtOnce() {
        TestClock clock = new TestClock();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(3, Duration.ofMinutes(1), 100, clock);
        for (int i = 0; i < 3; i++) {
            limiter.consume("a");
        }
        assertThat(limiter.consume("a")).isPositive();

        // A third of the window is a third of the allowance, so exactly one more.
        clock.advance(Duration.ofSeconds(20));
        assertThat(limiter.consume("a")).isZero();
        assertThat(limiter.consume("a")).isPositive();

        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.consume("a")).isZero();
    }

    @Test
    void keysAreIndependent() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(1, Duration.ofMinutes(1), 100, new TestClock());

        assertThat(limiter.consume("a")).isZero();
        assertThat(limiter.consume("a")).isPositive();
        assertThat(limiter.consume("b")).isZero();
    }

    @Test
    void theWaitItReportsIsLongEnough() {
        TestClock clock = new TestClock();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(2, Duration.ofSeconds(10), 100, clock);
        limiter.consume("a");
        limiter.consume("a");

        Duration wait = limiter.consume("a");
        assertThat(wait).isPositive();

        // Just short of the advice is still refused; the advice itself is enough.
        clock.advance(wait.minusMillis(2));
        assertThat(limiter.consume("a")).isPositive();
        clock.advance(Duration.ofMillis(4));
        assertThat(limiter.consume("a")).isZero();
    }

    /**
     * A successful sign-in gives its token back, so the right password never
     * counts towards the limit.
     */
    @Test
    void refundingPutsATokenBack() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(1, Duration.ofMinutes(1), 100, new TestClock());

        assertThat(limiter.consume("a")).isZero();
        assertThat(limiter.consume("a")).isPositive();

        limiter.refund("a");
        assertThat(limiter.consume("a")).isZero();
    }

    @Test
    void refundingNeverExceedsCapacity() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(1, Duration.ofMinutes(1), 100, new TestClock());
        limiter.consume("a");
        limiter.refund("a");
        limiter.refund("a");
        limiter.refund("a");

        assertThat(limiter.consume("a")).isZero();
        assertThat(limiter.consume("a")).isPositive();
    }

    /**
     * Spraying unique addresses must not be a way to fill memory.
     */
    @Test
    void refilledKeysAreEvictedOnceTheMapGrows() {
        TestClock clock = new TestClock();
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(1, Duration.ofSeconds(1), 10, clock);

        for (int i = 0; i < 50; i++) {
            limiter.consume("key-" + i);
        }
        // Everything has refilled by now, so the next call clears them out.
        clock.advance(Duration.ofSeconds(5));
        limiter.consume("trigger");

        // Still limiting correctly after the sweep.
        assertThat(limiter.consume("trigger")).isPositive();
    }
}
