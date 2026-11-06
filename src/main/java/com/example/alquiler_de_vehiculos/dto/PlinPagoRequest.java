package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PlinPagoRequest(
    @NotNull(message = "El id del alquiler es obligatorio")
    Integer idAlquiler,

    @Pattern(regexp = "^9\\d{8}$|^$", message = "El celular debe iniciar con 9 y contener 9 dígitos")
    String celular,

    String banco,

    BigDecimal monto
) {}
