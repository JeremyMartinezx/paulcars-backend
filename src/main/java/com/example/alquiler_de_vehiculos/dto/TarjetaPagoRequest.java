package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record TarjetaPagoRequest(
    @NotNull(message = "El id del alquiler es obligatorio")
    Integer idAlquiler,

    String token,
    String marca,
    String ultimosCuatroDigitos,
    String titular,
    BigDecimal monto,

    String numeroTarjeta,
    String vencimiento,
    String cvv
) {}

