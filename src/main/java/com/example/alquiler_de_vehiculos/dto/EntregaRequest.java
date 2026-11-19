package com.example.alquiler_de_vehiculos.dto;

import java.util.List;

import com.example.alquiler_de_vehiculos.dto.DanosDTOs.DanoRequest;

/**
 * Datos del acta de entrega del vehículo que registra el asesor en counter.
 */
public record EntregaRequest(
        Boolean documentosVerificados,
        /** Lectura del odómetro al entregar el vehículo (km). */
        Integer kilometraje,
        String combustible,
        List<String> checklist,
        String observaciones,
        /** Hora real (HH:mm) a la que se entregan las llaves; si es nula se usa la hora actual. */
        String horaEntrega,
        /** Daños que el vehículo ya tiene al entregarlo (estado inicial). */
        List<DanoRequest> danos
) {}
