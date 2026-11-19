package com.example.alquiler_de_vehiculos.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Límite por hora para las dos únicas altas públicas (registro de cliente y reserva) cuando quien llama
 * NO ha iniciado sesión. Clientes y personal con sesión no se ven afectados. Se registra en la cadena de
 * Spring Security después del filtro JWT (no es un @Component), así la sesión ya está resuelta.
 */
public class LimitePublicoFilter extends OncePerRequestFilter {

    private final int limitePorHora;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public LimitePublicoFilter(int limitePorHora) {
        this.limitePorHora = limitePorHora;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        boolean altaPublica = "POST".equalsIgnoreCase(req.getMethod())
                && ("/api/clientes".equals(uri) || "/api/alquileres".equals(uri));
        if (altaPublica && !conSesion()) {
            String xf = req.getHeader("X-Forwarded-For");
            String ip = (xf != null && !xf.isBlank()) ? xf.split(",")[0].trim() : req.getRemoteAddr();
            Bucket b = buckets.computeIfAbsent(String.valueOf(ip), k -> Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(limitePorHora)
                            .refillGreedy(limitePorHora, Duration.ofHours(1)).build())
                    .build());
            if (!b.tryConsume(1)) {
                res.setStatus(429);
                res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Demasiadas solicitudes desde su conexión. Intente nuevamente más tarde o inicie sesión.\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private boolean conSesion() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken);
    }
}
