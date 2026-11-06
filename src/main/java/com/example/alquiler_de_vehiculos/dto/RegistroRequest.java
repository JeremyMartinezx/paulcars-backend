package com.example.alquiler_de_vehiculos.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(
        @NotBlank String username,
        @NotBlank String password,
        String emailRecuperacion,
        Integer idCliente,
        String nombres,
        String apellidos,
        String telefono) {
}
