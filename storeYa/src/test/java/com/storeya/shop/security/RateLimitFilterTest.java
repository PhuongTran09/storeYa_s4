package com.storeya.shop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * /api/auth/** and the VNPay IPN callback are the two doors open without a token, and
 * neither had a limit. The filter has to count those and nothing else, and it must not
 * let a caller pick their own bucket by sending X-Forwarded-For.
 */
class RateLimitFilterTest {

    private static final int AUTH_LIMIT = 10;
    private static final int IPN_LIMIT = 120;

    private RedisWindowCounter counter;

    @BeforeEach
    void setUp() {
        counter = mock(RedisWindowCounter.class);
    }

    @Test
    void requestsOverTheAuthLimitAreRefused() throws Exception {
        when(counter.increment(anyString(), any(Duration.class))).thenReturn((long) AUTH_LIMIT + 1);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter(true, false).doFilter(post("/api/auth/login"), response, chain);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertNull(chain.getRequest(), "a refused request must not reach the dispatcher");
        assertTrue(response.getContentAsString().contains("Quá nhiều yêu cầu"),
                response.getContentAsString());
    }

    @Test
    void requestsWithinTheLimitPassThrough() throws Exception {
        when(counter.increment(anyString(), any(Duration.class))).thenReturn(1L);

        MockFilterChain chain = new MockFilterChain();
        filter(true, false).doFilter(post("/api/auth/login"), new MockHttpServletResponse(), chain);

        assertNotNull(chain.getRequest(), "the request was swallowed by the filter");
        verify(counter).increment(anyString(), any(Duration.class));
    }

    /** The IPN callback is VNPay's, not an attacker's brute force: a separate, wider bucket. */
    @Test
    void theIpnCallbackHasItsOwnBucket() throws Exception {
        when(counter.increment(anyString(), any(Duration.class))).thenReturn(1L);

        filter(true, false).doFilter(get("/api/payments/vnpay-ipn"),
                new MockHttpServletResponse(), new MockFilterChain());

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(counter).increment(key.capture(), any(Duration.class));
        assertTrue(key.getValue().startsWith("RL:ipn:"), key.getValue());
    }

    /** Redis being down must not take sign-in or checkout down with it. */
    @Test
    void aDeadCounterFailsOpen() throws Exception {
        when(counter.increment(anyString(), any(Duration.class))).thenReturn(0L);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter(true, false).doFilter(post("/api/auth/login"), response, chain);

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void preflightRequestsAreNotCounted() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter(true, false).doFilter(new MockHttpServletRequest("OPTIONS", "/api/auth/login"),
                new MockHttpServletResponse(), chain);

        verifyNoInteractions(counter);
        assertNotNull(chain.getRequest());
    }

    @Test
    void endpointsOutsideTheTwoBucketsAreNotCounted() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter(true, false).doFilter(get("/api/products/public/all"),
                new MockHttpServletResponse(), chain);

        verifyNoInteractions(counter);
        assertNotNull(chain.getRequest());
    }

    @Test
    void theFilterCanBeTurnedOff() throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter(false, false).doFilter(post("/api/auth/login"), new MockHttpServletResponse(), chain);

        verifyNoInteractions(counter);
        assertNotNull(chain.getRequest());
    }

    /**
     * Ahead of an untrusted proxy, X-Forwarded-For is attacker-supplied: believing it by
     * default would let anyone reset their own limit by rotating the header.
     */
    @Test
    void forwardedForIsIgnoredUntilAProxyIsTrusted() throws Exception {
        when(counter.increment(anyString(), any(Duration.class))).thenReturn(1L);

        MockHttpServletRequest spoofed = post("/api/auth/login");
        spoofed.setRemoteAddr("203.0.113.9");
        spoofed.addHeader("X-Forwarded-For", "198.51.100.7");
        filter(true, false).doFilter(spoofed, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest behindProxy = post("/api/auth/login");
        behindProxy.setRemoteAddr("127.0.0.1");
        behindProxy.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.1");
        filter(true, true).doFilter(behindProxy, new MockHttpServletResponse(), new MockFilterChain());

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(counter, times(2)).increment(keys.capture(), any(Duration.class));
        List<String> recorded = keys.getAllValues();
        assertTrue(recorded.get(0).contains("203.0.113.9"),
                () -> "an untrusted filter keyed off a spoofable header: " + recorded.get(0));
        assertTrue(recorded.get(1).contains("198.51.100.7"),
                () -> "a trusted filter ignored the proxy header: " + recorded.get(1));
    }

    private RateLimitFilter filter(boolean enabled, boolean trustProxy) {
        return new RateLimitFilter(counter, new ObjectMapper(), enabled, trustProxy,
                AUTH_LIMIT, IPN_LIMIT);
    }

    private static MockHttpServletRequest post(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        return request;
    }

    private static MockHttpServletRequest get(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
    }
}
