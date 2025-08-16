package com.example.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redis;
    private static final int LIMIT = 5;      // 100 req per minute
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public RateLimiterFilter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Allow login to be rate-limited too, but you can exclude actuator if needed
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String user = (req.getUserPrincipal() != null)
                ? req.getUserPrincipal().getName()
                : req.getRemoteAddr(); // fallback to IP before JWT

        String key = "rate:" + user + ":" + (System.currentTimeMillis() / WINDOW.toMillis());
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, WINDOW);
        }
        if (count != null && count > LIMIT) {
            res.setStatus(429);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write("{\"success\":false,\"message\":\"Too Many Requests\",\"errors\":[]}");
            return;
        }
        chain.doFilter(req, res);
    }
}
