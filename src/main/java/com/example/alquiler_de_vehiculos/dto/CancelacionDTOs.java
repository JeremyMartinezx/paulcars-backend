package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Datos de entrada y salida de la cancelación de reservas y del cambio de vehículo. */
public final class CancelacionDTOs {

    private CancelacionDTOs() {
    }

    public record CancelarReservaRequest(
            String motivo,
            String observaciones,
            /** Código de operación de la devolución (voucher de anulación, código Yape, etc.); no se pide en efectivo. */
            String codigoOperacion
    ) {}

    public record CambiarUnidadRequest(String placa, String motivo) {}

    public record UnidadDisponible(String placa, String color, String sede) {}

    /** Lo que pasaría si se cancelara ahora: reglas aplicadas, porcentaje y monto a devolver. */
    public record PreviaCancelacion(
            Integer idAlquiler,
            String estado,
            boolean tienePagos,
            BigDecimal montoPagado,
            String metodosPago,
            LocalDateTime recojoPactado,
            long horasParaRecojo,
            int porcentajeReembolso,
            BigDecimal montoReembolso,
            BigDecimal montoRetenido,
            String regla,
            boolean puedeCancelarElUsuario,
            LocalDateTime venceElPago,
            boolean pagoVencido,
            /** Plazo en que llegaría la devolución si el pago no fue en efectivo (vacío en efectivo). */
            String plazoDevolucion
    ) {}

    public record ResultadoCancelacion(
            Integer idAlquiler,
            BigDecimal montoReembolsado,
            String notasCredito,
            String mensaje
    ) {}
}
