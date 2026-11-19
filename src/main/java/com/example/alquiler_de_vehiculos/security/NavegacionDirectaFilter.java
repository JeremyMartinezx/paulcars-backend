package com.example.alquiler_de_vehiculos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Las direcciones /api/** son solo para la aplicación web: si alguien las abre escribiéndolas
 * en la barra del navegador (navegación directa), se responde 404 y no se muestra ningún dato.
 * El navegador marca esas peticiones con Sec-Fetch-Mode: navigate / Sec-Fetch-Dest: document,
 * mientras que las llamadas de la aplicación (fetch/XHR) llevan cors / empty.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class NavegacionDirectaFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ruta = req.getRequestURI();
        boolean abiertaPorTodos = ruta.startsWith("/images/") || ruta.startsWith("/uploads/") || ruta.startsWith("/actuator/health");
        if (!abiertaPorTodos) {
            String modo = req.getHeader("Sec-Fetch-Mode");
            String destino = req.getHeader("Sec-Fetch-Dest");
            boolean directa = "navigate".equalsIgnoreCase(modo)
                    || "document".equalsIgnoreCase(destino)
                    || "iframe".equalsIgnoreCase(destino)
                    || "frame".equalsIgnoreCase(destino);
            if (directa) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.setContentType("text/plain;charset=UTF-8");
                res.setHeader("Cache-Control", "no-store");
                res.getWriter().write("No encontrado");
                return;
            }
            res.setHeader("Cache-Control", "no-store");
            res.setHeader("X-Robots-Tag", "noindex, nofollow");
        }
        chain.doFilter(req, res);
    }
}
