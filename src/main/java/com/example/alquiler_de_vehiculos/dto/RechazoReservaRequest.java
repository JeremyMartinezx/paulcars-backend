package com.example.alquiler_de_vehiculos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazoReservaRequest(
        @NotBlank String motivo,
        @Size(max = 500) String observaciones
) {}
