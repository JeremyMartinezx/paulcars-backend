package com.example.alquiler_de_vehiculos.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Servicio criptográfico de gestión de JSON Web Tokens (HMAC-SHA).
 * Incluye lista de revocación en memoria para invalidar tokens ante cierres de sesión o bloqueos.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    // Cache de revocación en memoria (Token -> Timestamp de expiración)
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generarToken(UserDetails userDetails) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("type", "ACCESS")
                .claim("roles", userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String generarRefreshToken(UserDetails userDetails) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("type", "REFRESH")
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + refreshExpirationMs))
                .signWith(key)
                .compact();
    }

    public String obtenerUsername(String token) {
        return claims(token).getSubject();
    }

    public String obtenerTipoToken(String token) {
        Claims c = claims(token);
        return c.get("type", String.class);
    }

    public boolean esValido(String token, UserDetails userDetails) {
        if (estaRevocado(token)) {
            return false;
        }
        Claims claims = claims(token);
        return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date())
                && userDetails.isEnabled();
    }

    public boolean esRefreshTokenValido(String token, UserDetails userDetails) {
        if (estaRevocado(token)) {
            return false;
        }
        Claims claims = claims(token);
        String tipo = claims.get("type", String.class);
        return "REFRESH".equalsIgnoreCase(tipo)
                && claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date())
                && userDetails.isEnabled();
    }

    /**
     * Invalida un token emitido agregándolo a la lista de revocación.
     */
    public void revocarToken(String token) {
        if (token == null || token.isBlank()) return;
        try {
            Claims c = claims(token);
            blacklistedTokens.put(token, c.getExpiration().getTime());
            limpiarBlacklistExpirada();
        } catch (Exception ignored) {
            // Token malformado o ya expirado no requiere blacklist
        }
    }

    /**
     * Determina si un token ha sido revocado explícitamente.
     */
    public boolean estaRevocado(String token) {
        if (token == null) return true;
        Long exp = blacklistedTokens.get(token);
        if (exp == null) return false;
        if (System.currentTimeMillis() > exp) {
            blacklistedTokens.remove(token);
            return false;
        }
        return true;
    }

    private void limpiarBlacklistExpirada() {
        long ahora = System.currentTimeMillis();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue() < ahora);
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
