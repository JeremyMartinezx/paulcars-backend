package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;

/** Cargos que se aplicarían al recibir el vehículo ahora mismo, para mostrarlos al asesor antes de confirmar. */
public record CargosPreviosResponse(
        BigDecimal cargoCombustible,
        int cuartosFaltantes,
        BigDecimal cargoRetraso,
        int minutosRetraso,
        BigDecimal garantia
) {}
