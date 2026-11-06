package com.example.alquiler_de_vehiculos.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alquiler_de_vehiculos.dto.NotificacionDTO;
import com.example.alquiler_de_vehiculos.service.NotificacionService;

@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("isAuthenticated()")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<NotificacionDTO> listar(Authentication authentication) {
        return notificacionService.listar(authentication.getName());
    }

    @PostMapping("/{clave}/leida")
    public Map<String, Boolean> marcarLeida(Authentication authentication, @PathVariable String clave) {
        notificacionService.marcarLeidas(authentication.getName(), List.of(clave));
        return Map.of("ok", true);
    }

    @PostMapping("/leidas")
    public Map<String, Boolean> marcarVarias(Authentication authentication, @RequestBody List<String> claves) {
        notificacionService.marcarLeidas(authentication.getName(), claves);
        return Map.of("ok", true);
    }
}
