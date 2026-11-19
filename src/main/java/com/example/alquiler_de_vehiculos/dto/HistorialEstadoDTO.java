package com.example.alquiler_de_vehiculos.dto;

import java.time.LocalDateTime;

/** Un cambio de estado de un alquiler (para ordenar y mostrar el historial en el panel). */
public record HistorialEstadoDTO(
        Integer idAlquiler,
        String estadoAnterior,
        String estadoNuevo,
        LocalDateTime fechaHora
) {}
