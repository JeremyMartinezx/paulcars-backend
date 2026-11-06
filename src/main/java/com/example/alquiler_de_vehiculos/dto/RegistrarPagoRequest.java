package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record RegistrarPagoRequest(
        @NotNull Integer idAlquiler,
        @NotNull Integer idMetodoPago,
        @NotNull BigDecimal monto,
        BigDecimal montoRecibido,
        String ultimosDigitos,
        String numeroOperacion,
        String bancoOrigen,
        Boolean garantiaConfirmada
) {}
