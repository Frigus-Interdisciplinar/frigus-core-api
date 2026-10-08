package com.frigus.coreapi.security;

import com.frigus.coreapi.model.User;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private static final DefaultRedisScript<List> LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); " +
            "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end; " +
            "return {count, redis.call('TTL', KEYS[1])}", List.class);

    private final StringRedisTemplate redis;
    private final MeterRegistry meters;

    @Value("${SECURITY_RATE_LIMIT_AUTH:10}") private int authLimit;
    @Value("${SECURITY_RATE_LIMIT_AI:20}") private int aiLimit;
    @Value("${SECURITY_RATE_LIMIT_CHECKOUT:10}") private int checkoutLimit;
    @Value("${SECURITY_RATE_LIMIT_WINDOW_SECONDS:60}") private long windowSeconds;
    @Value("${SECURITY_TRUSTED_PROXY_CIDRS:}") private String trustedProxyCidrs;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!"POST".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        int limit;
        String scope;
        if (List.of("/auth/login", "/auth/register", "/auth/refresh").contains(path)) {
            limit = authLimit;
            scope = "ip:" + clientAddress(request);
        } else if ("/ai/recipes/chat".equals(path) || "/transactions/checkout".equals(path)) {
            limit = "/ai/recipes/chat".equals(path) ? aiLimit : checkoutLimit;
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof User user)) {
                chain.doFilter(request, response);
                return;
            }
            scope = "user:" + user.getId();
        } else {
            chain.doFilter(request, response);
            return;
        }

        String key = "rate-limit:" + path + ":" + scope;
        List<?> result = redis.execute(LIMIT_SCRIPT, List.of(key), String.valueOf(windowSeconds));
        if (result == null || result.size() != 2) {
            response.sendError(503, "Rate limit unavailable");
            return;
        }
        if (((Number) result.get(0)).longValue() > limit) {
            meters.counter("security.rate_limit.blocked", "route", path).increment();
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, ((Number) result.get(1)).longValue())));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Limite de requisições excedido\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String clientAddress(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!isTrustedProxy(remote)) return remote;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null) return remote;
        String[] hops = forwarded.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String candidate = hops[i].trim();
            if (!isIpv4(candidate)) return remote;
            if (!isTrustedProxy(candidate)) return candidate;
        }
        return remote;
    }

    private boolean isTrustedProxy(String address) {
        if (!isIpv4(address)) return false;
        long ip = ipv4(address);
        for (String cidr : trustedProxyCidrs.split(",")) {
            String[] parts = cidr.trim().split("/");
            if (parts.length != 2 || !isIpv4(parts[0])) continue;
            try {
                int bits = Integer.parseInt(parts[1]);
                if (bits < 0 || bits > 32) continue;
                long mask = bits == 0 ? 0 : (0xffffffffL << (32 - bits)) & 0xffffffffL;
                if ((ip & mask) == (ipv4(parts[0]) & mask)) return true;
            } catch (NumberFormatException ignored) {
                // Invalid configuration cannot make a proxy trusted.
            }
        }
        return false;
    }

    private boolean isIpv4(String address) {
        if (address == null || !address.matches("[0-9.]+")) return false;
        String[] octets = address.split("\\.", -1);
        if (octets.length != 4) return false;
        try {
            for (String octet : octets) {
                if (octet.isEmpty() || octet.length() > 3 || Integer.parseInt(octet) > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private long ipv4(String address) {
        long result = 0;
        for (String octet : address.split("\\.")) result = (result << 8) | Integer.parseInt(octet);
        return result;
    }
}
