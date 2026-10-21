package com.storeya.shop.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Fixed-window counter backed by Redis, shared by the HTTP rate limiter and the
 * OTP throttles so the atomicity requirement lives in exactly one place.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisWindowCounter {

    /**
     * INCR and set the TTL in one round trip. Doing it as two commands leaves a key
     * with no expiry if the process dies in between, which would block that caller
     * forever. Only the first increment sets the window.
     */
    private static final DefaultRedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1])\n"
                    + "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end\n"
                    + "return count",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    /**
     * @return the hit count for this window, or 0 when Redis is unreachable.
     *         Fail-open is deliberate: Redis is not on the correctness path for
     *         anything else here, and an outage must not block sign-in or checkout.
     */
    public long increment(String key, Duration window) {
        try {
            Long count = redisTemplate.execute(INCR_WITH_TTL, List.of(key),
                    String.valueOf(window.toMillis()));
            return count == null ? 0L : count;
        } catch (Exception e) {
            log.warn("Rate-limit counter unavailable, allowing request through: {}", e.getMessage());
            return 0L;
        }
    }

    public void reset(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Could not reset rate-limit counter: {}", e.getMessage());
        }
    }
}
