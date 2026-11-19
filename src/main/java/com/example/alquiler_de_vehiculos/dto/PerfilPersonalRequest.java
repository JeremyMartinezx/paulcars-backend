package com.example.alquiler_de_vehiculos.dto;

/** Datos que el personal (administrador o asesor) puede corregir de su propio perfil. */
public record PerfilPersonalRequest(String nombres, String apellidos, String dni) {}
