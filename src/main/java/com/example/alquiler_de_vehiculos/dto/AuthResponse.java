package com.example.alquiler_de_vehiculos.dto;

public record AuthResponse(
        String token,
        String tipo,
        String username,
        String rol,
        String nombre,
        String email,
        String refreshToken
) {
    public AuthResponse(String token, String tipo, String username, String rol, String nombre, String email) {
        this(token, tipo, username, rol, nombre, email, null);
    }
}
