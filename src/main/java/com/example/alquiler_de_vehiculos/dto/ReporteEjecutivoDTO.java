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
        /** Reservas confirmadas que todavía no se han pagado y el monto por cobrar. */
        long pagosPendientes,
        BigDecimal montoPendiente,

        List<FilaVehiculo> topVehiculos,
        List<FilaMonto> ingresosPorSede,
        List<FilaMonto> ingresosPorCategoria,
        List<FilaMonto> ingresosPorMetodo,

        /** "TODOS" o el vehículo elegido, por ejemplo "Kia Rio (JKL-012)". */
        String vehiculo,
        /** Alquileres del vehículo elegido en el periodo (vacío cuando se ven todos los vehículos). */
        List<FilaAlquilerVehiculo> alquileresVehiculo
) {
    public record FilaAlquilerVehiculo(
            String codigo,
            String cliente,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            int dias,
            String estado,
            BigDecimal total,
            BigDecimal pagado
    ) {}

    /** Una unidad de la flota para el selector de vehículo. */
    public record UnidadOpcion(String placa, String nombre, String categoria, String sede) {}

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
