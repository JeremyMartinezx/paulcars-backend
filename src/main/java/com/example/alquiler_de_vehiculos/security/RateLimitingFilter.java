package com.example.alquiler_de_vehiculos.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de límite de tasa de solicitudes (Rate Limiting) con algoritmo Token Bucket.
 * Mitiga ataques de fuerza bruta en autenticación, registro y recuperación OTP,
 * además de prevenir sobrecarga o denegación de servicio (DoS) en la API.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String clientIp = resolveClientIp(request);

        // Control de fuerza bruta estricto en credenciales y recuperación OTP (10 intentos/min)
        if (uri.startsWith("/api/auth/login") ||
            uri.startsWith("/api/auth/registro") ||
            uri.startsWith("/api/auth/recuperar-password")) {
            Bucket bucket = authBuckets.computeIfAbsent(clientIp, k -> createAuthBucket());
            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response, "Límite de intentos de autenticación/recuperación excedido. Espere 1 minuto.");
                return;
            }
        } else if (uri.startsWith("/api/")) {
            // Protección volumétrica en el resto de la API (120 peticiones/min)
            Bucket bucket = apiBuckets.computeIfAbsent(clientIp, k -> createApiBucket());
            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response, "Demasiadas peticiones a la API. Intente nuevamente en un momento.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillGreedy(10, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(120)
                .refillGreedy(120, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = String.format("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"%s\"}", message);
        response.getWriter().write(json);
    }
}
