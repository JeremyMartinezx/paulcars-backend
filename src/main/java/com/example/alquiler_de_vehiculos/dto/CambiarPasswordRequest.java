package com.example.alquiler_de_vehiculos.dto;

public record CambiarPasswordRequest(
    String passwordActual,
    String passwordNuevo,
    String codigoVerificacion
) {}
