package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.alquiler_de_vehiculos.dto.DanosDTOs.DanoRequest;

public record CheckoutRequest(
        LocalDate fechaDevolucion,
        String horaDevolucion,
        BigDecimal garantiaRetenida,
        String observaciones,
        /** Lectura del odómetro al recibir el vehículo (km). */
        Integer kilometraje,
        /** Nivel de combustible al recibir el vehículo: LLENO, 3/4, 1/2 o 1/4. */
        String combustible,
        /** Momento fijado al abrir la pantalla de recepción: es la hora de devolución que se registra. */
        LocalDateTime momentoRecepcion,
        /** Daños nuevos que se encuentran al recibir el vehículo. */
        List<DanoRequest> danos
) {}
