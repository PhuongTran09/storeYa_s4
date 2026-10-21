package com.storeya.shop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storeya.shop.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Coarse abuse damper for the two endpoints that are open without a token: the auth
 * endpoints (credential stuffing, OTP spam) and the VNPay IPN callback.
 *
 * Runs ahead of the security filter chain on purpose — SecurityProperties.DEFAULT_FILTER_ORDER
 * is -100, so -200 rejects noise before any JWT work is spent.
 *
 * This is a damper, not an identity system: client IP is spoofable unless TRUST_PROXY is
 * set behind a real reverse proxy, and the OTP flow is protected properly by the per-email
 * counters in AuthService, not by this filter.
 */
@Slf4j
@Component
@Order(-200)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RedisWindowCounter counter;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final boolean trustProxy;
    private final Map<String, Integer> bucketLimits;

    public RateLimitFilter(RedisWindowCounter counter,
                           ObjectMapper objectMapper,
                           @Value("${security.rate-limit.enabled:true}") boolean enabled,
                           @Value("${security.trust-proxy:false}") boolean trustProxy,
                           @Value("${security.rate-limit.auth-per-minute:10}") int authPerMinute,
                           @Value("${security.rate-limit.ipn-per-minute:120}") int ipnPerMinute) {
        this.counter = counter;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.trustProxy = trustProxy;
        this.bucketLimits = Map.of("auth", authPerMinute, "ipn", ipnPerMinute);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // A CORS preflight is not the request it precedes; counting it would eat the
        // real request's budget and break the browser flow.
        String bucket = enabled && !"OPTIONS".equalsIgnoreCase(request.getMethod())
                ? resolveBucket(request) : null;

        if (bucket == null) {
            filterChain.doFilter(request, response);
            return;
        }

        int limit = bucketLimits.get(bucket);
        long hits = counter.increment("RL:" + bucket + ":" + clientIp(request), WINDOW);

        if (hits > limit) {
            log.warn("Rate limit exceeded for bucket {}", bucket);
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBucket(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            return "auth";
        }
        if (path.equals("/api/payments/vnpay-ipn")) {
            return "ipn";
        }
        return null;
    }

    /**
     * X-Forwarded-For is only believed when explicitly enabled, because trusting it by
     * default lets any caller pick their own rate-limit identity.
     */
    private String clientIp(HttpServletRequest request) {
        if (trustProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.error("Quá nhiều yêu cầu, vui lòng thử lại sau."));
    }
}
