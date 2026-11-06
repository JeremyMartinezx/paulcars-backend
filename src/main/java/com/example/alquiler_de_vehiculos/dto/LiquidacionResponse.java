package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LiquidacionResponse(
        Integer idAlquiler,
        String cliente,
        String documentoCliente,
        String vehiculo,
        String placa,
        String categoria,
        BigDecimal garantiaRetenida,
        BigDecimal garantiaLiberada,
        String estadoAlquiler,
        String estadoVehiculo,
        String estadoCliente,
        String observaciones,
        LocalDateTime fechaCheckout
) {}
