package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Estado de la devolución de dinero de una reserva cancelada: lo que ve el cliente y el administrador. */
public record DevolucionDTO(
        Integer idAlquiler,
        boolean cancelada,
        BigDecimal montoPagado,
        BigDecimal montoDevuelto,
        BigDecimal montoRetenido,
        String metodos,
        /** SIN_DEVOLUCION, EN_TRAMITE (aún no llega a la cuenta del cliente) o DEVUELTA */
        String estado,
        String plazo,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaDevuelta,
        List<NotaCreditoRef> notasCredito,
        String mensaje,
        /** Fecha límite en que llega la devolución a la cuenta del cliente (null si fue en efectivo). */
        LocalDate fechaEstimada
) {
    public record NotaCreditoRef(Integer idBoleta, String numero, BigDecimal monto) {}
}
