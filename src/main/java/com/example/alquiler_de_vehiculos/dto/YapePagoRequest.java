package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record YapePagoRequest(
    @NotNull(message = "El id del alquiler es obligatorio")
    Integer idAlquiler,

    @NotBlank(message = "El celular Yape es obligatorio")
    @Pattern(regexp = "^9\\d{8}$", message = "El celular debe iniciar con 9 y contener 9 dígitos")
    String celular,

    @NotBlank(message = "El código OTP de aprobación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código de aprobación OTP debe tener exactamente 6 dígitos")
    String otp,

    BigDecimal monto
) {}
