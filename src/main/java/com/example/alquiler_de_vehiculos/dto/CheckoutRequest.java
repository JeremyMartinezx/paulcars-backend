package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CheckoutRequest(
        LocalDate fechaDevolucion,
        String horaDevolucion,
        BigDecimal garantiaRetenida,
        String observaciones
) {}
