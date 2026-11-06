package com.example.alquiler_de_vehiculos.dto;

import jakarta.validation.constraints.NotBlank;

public record CrearUsuarioRequest(
        @NotBlank String username,
        String password,
        @NotBlank String rolNombre,
        String emailRecuperacion,
        String nombreCompleto,
        String nombres,
        String apellidos,
        String dni) {
}
