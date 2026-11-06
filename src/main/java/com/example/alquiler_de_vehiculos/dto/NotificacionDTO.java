package com.example.alquiler_de_vehiculos.dto;

public record NotificacionDTO(
        String clave,
        String tipo,
        String titulo,
        String mensaje,
        String fecha,
        Integer idAlquiler,
        boolean leida
) {}
