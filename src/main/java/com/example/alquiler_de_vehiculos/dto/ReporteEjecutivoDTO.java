package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteEjecutivoDTO(
        LocalDate desde,
        LocalDate hasta,
        String sede,
        String categoria,

        BigDecimal ingresosTotales,
        BigDecimal ocupacionFlota,
        BigDecimal ticketPromedio,
        long alquileresEnPeriodo,
        long alquileresFinalizados,
        long diasTotalesAlquilados,

        List<FilaVehiculo> topVehiculos,
        List<FilaMonto> ingresosPorSede,
        List<FilaMonto> ingresosPorCategoria,
        List<FilaMonto> ingresosPorMetodo
) {
    public record FilaVehiculo(
            Integer idVehiculo,
            String vehiculo,
            String placa,
            String categoria,
            long alquileres,
            long diasAlquilados,
            BigDecimal ingresos,
            BigDecimal utilizacion
    ) {}

    public record FilaMonto(String etiqueta, long cantidad, BigDecimal total) {}
}
