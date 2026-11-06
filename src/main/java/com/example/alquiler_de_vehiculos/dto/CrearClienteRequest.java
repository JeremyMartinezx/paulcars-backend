package com.example.alquiler_de_vehiculos.dto;

public record CrearClienteRequest(
        String nombres,
        String apellidos,
        String tipoDocumento,
        String numeroDocumento,
        String telefono,
        String email,
        String licenciaConducir,
        String direccion,
        String password
) {}
