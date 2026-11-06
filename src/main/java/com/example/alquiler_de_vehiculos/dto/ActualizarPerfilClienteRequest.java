package com.example.alquiler_de_vehiculos.dto;

public record ActualizarPerfilClienteRequest(
        String nombres,
        String apellidos,
        String tipoDocumento,
        String numeroDocumento,
        String telefono,
        String licenciaConducir,
        String direccion,
        String password
) {}
