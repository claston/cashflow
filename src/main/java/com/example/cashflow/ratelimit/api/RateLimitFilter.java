package com.example.cashflow.ratelimit.api;

import com.example.cashflow.ratelimit.application.RateLimiter;
import com.example.cashflow.shared.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean enabled;

    public RateLimitFilter(
            RateLimiter rateLimiter,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${app.rate-limit.enabled:true}") boolean enabled
    ) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = request.getHeader("X-Client-Id");
        if (clientId == null || clientId.isBlank()) {
            clientId = "anonymous";
        }

        if (rateLimiter.allow(clientId)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(
                new ErrorResponse(
                        "RATE_LIMIT_EXCEEDED",
                        "Too many requests. Try again later.",
                        Instant.now(clock)
                )
        ));
    }
}
