package com.example.alquiler_de_vehiculos.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record AlquilerCounterRequest(
        @NotNull Integer idCliente,
        @NotNull Integer idVehiculo,
        @NotNull LocalDate fechaInicio,
        @NotNull LocalDate fechaFin,
        String sedeRecojo,
        String sedeDevolucion,
        String horaRecojo,
        String horaDevolucion,
        Boolean seguroTodoRiesgo,
        Boolean conductorAdicional,
        Boolean sillaBebe
) {}
