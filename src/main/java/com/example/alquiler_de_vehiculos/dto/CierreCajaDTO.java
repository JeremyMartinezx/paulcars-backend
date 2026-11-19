package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Cierre de caja de un día: lo cobrado por cada asesor y método de pago, más el detalle de cada cobro. */
public record CierreCajaDTO(
        LocalDate fecha,
        LocalDateTime generadoEn,
        String generadoPor,
        List<FilaAsesor> porAsesor,
        FilaAsesor totales,
        List<Detalle> detalle
) {
    public record FilaAsesor(
            String asesor,
            long cantidad,
            BigDecimal efectivo,
            BigDecimal tarjeta,
            BigDecimal billetera,
            BigDecimal transferencia,
            BigDecimal total
    ) {}

    public record Detalle(
            LocalDateTime fechaHora,
            String boleta,
            String contrato,
            String cliente,
            String metodo,
            String operacion,
            String autorizacion,
            String asesor,
            BigDecimal monto
    ) {}
}
