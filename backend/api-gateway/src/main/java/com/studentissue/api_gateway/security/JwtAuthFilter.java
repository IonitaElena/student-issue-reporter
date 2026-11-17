package com.studentissue.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PREFIXES = List.of("/api/auth/");

    private final SecretKey key;

    public JwtAuthFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // preflight CORS: lăsăm să treacă fără token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        Map<String, String> identity = new HashMap<>();

        if (!isPublic(request.getRequestURI())) {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                unauthorized(response, "Token lipsă");
                return;
            }
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(header.substring(7))
                        .getPayload();

                identity.put("X-User-Id", String.valueOf(claims.get("userId")));
                identity.put("X-User-Email", claims.getSubject());
                identity.put("X-User-Role", String.valueOf(claims.get("role")));
            } catch (JwtException | IllegalArgumentException e) {
                unauthorized(response, "Token invalid sau expirat");
                return;
            }
        }

        chain.doFilter(new IdentityHeaderRequest(request, identity), response);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}