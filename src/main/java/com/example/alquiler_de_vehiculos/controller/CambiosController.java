package com.example.alquiler_de_vehiculos.controller;

import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alquiler_de_vehiculos.service.CambiosListener;

/**
 * Número que cambia cada vez que se modifica algo importante. Las pantallas del personal y del cliente lo consultan
 * para refrescarse solas. Solo es un contador: no revela ningún dato de las reservas de nadie.
 */
@RestController
@RequestMapping("/api/cambios")
public class CambiosController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Map<String, Long> version() {
        return Map.of("version", CambiosListener.version());
    }
}
