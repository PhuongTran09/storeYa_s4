package com.storeya.shop.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * The window counter is what stands between ten million guesses and the OTP attempt
 * ceiling, so its two contracts matter: one round trip per increment, and a refusal to
 * fail closed when Redis is unreachable.
 */
class RedisWindowCounterTest {

    private StringRedisTemplate redisTemplate;
    private RedisWindowCounter counter;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        counter = new RedisWindowCounter(redisTemplate);
    }

    @Test
    void theScriptsAnswerIsTheWindowCount() {
        Mockito.doReturn(4L).when(redisTemplate)
                .execute(Mockito.<RedisScript<Long>>any(), anyList(), any());

        assertEquals(4L, counter.increment("RL:auth:1.2.3.4:1", Duration.ofMinutes(1)));
    }

    @Test
    void aMissingScriptResultCountsAsAllowed() {
        Mockito.doReturn(null).when(redisTemplate)
                .execute(Mockito.<RedisScript<Long>>any(), anyList(), any());

        assertEquals(0L, counter.increment("RL:auth:1.2.3.4:1", Duration.ofMinutes(1)));
    }

    /** An outage must not lock customers out of sign-in or checkout. */
    @Test
    void redisBeingDownFailsOpen() {
        Mockito.doThrow(new IllegalStateException("Unable to connect to Redis")).when(redisTemplate)
                .execute(Mockito.<RedisScript<Long>>any(), anyList(), any());

        assertEquals(0L, counter.increment("OTP:ATT:user@example.com", Duration.ofMinutes(15)));
    }

    @Test
    void resetSwallowsAnUnavailableRedis() {
        Mockito.doThrow(new IllegalStateException("Unable to connect to Redis")).when(redisTemplate)
                .delete(anyString());

        counter.reset("OTP:ATT:user@example.com");

        Mockito.verify(redisTemplate).delete("OTP:ATT:user@example.com");
    }
}
