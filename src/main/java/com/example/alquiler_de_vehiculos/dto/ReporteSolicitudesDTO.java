package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO.FilaMonto;

/**
 * Reporte de solicitudes y alquileres registrados en un periodo:
 * detalle fila por fila más un resumen para gerencia.
 */
public record ReporteSolicitudesDTO(
        LocalDate desde,
        LocalDate hasta,
        String estado,
        String sede,
        String categoria,
        String busqueda,
        /** Resto de filtros aplicados (método de pago, asesor, vehículo, placa), ya redactados para mostrar. */
        String filtrosExtra,
        LocalDateTime generadoEn,
        String generadoPor,

        Resumen resumen,
        List<FilaSolicitud> filas,
        List<FilaMonto> porEstado,
        List<FilaMonto> porMetodoPago,
        List<FilaMonto> porSede,
        List<FilaVehiculoSolicitado> vehiculosSolicitados,
        List<FilaPago> pagos
) {
    public record Resumen(
            long total,
            long pendientes,
            long porPagar,
            long pagadas,
            long enRuta,
            long finalizadas,
            long anuladas,
            BigDecimal montoContratado,
            BigDecimal montoCobrado,
            BigDecimal saldoPorCobrar
    ) {}

    public record FilaSolicitud(
            Integer idAlquiler,
            String codigo,
            LocalDateTime fechaRegistro,
            LocalDateTime fechaUltimoEstado,
            String cliente,
            String documento,
            String vehiculo,
            String placa,
            String categoria,
            String sede,
            LocalDate fechaRecojo,
            String horaRecojo,
            LocalDate fechaDevolucion,
            String horaDevolucion,
            int dias,
            String grupoEstado,
            String estado,
            String estadoPago,
            String metodosPago,
            BigDecimal montoTotal,
            BigDecimal montoPagado,
            BigDecimal saldo,
            LocalDateTime fechaEntrega,
            String entregadoPor,
            LocalDateTime fechaDevolucionReal,
            Integer kmEntrega,
            String combustibleEntrega,
            /** Minutos de diferencia entre el recojo real y el pactado (positivo = llegó tarde); nulo si aún no hay recojo. */
            Long difRecojoMin,
            /** Minutos de diferencia entre la devolución real y la pactada (positivo = devolvió tarde); nulo si aún no devuelve. */
            Long difDevolucionMin,
            /** Quién atendió: el asesor que entregó las llaves o, si no, quien cobró. */
            String atendidoPor
    ) {}

    /** Un cobro individual (para la hoja "Pagos" del Excel). */
    public record FilaPago(
            String codigoContrato,
            String recibo,
            LocalDateTime fechaHora,
            String cliente,
            String metodo,
            String canal,
            String operacion,
            BigDecimal monto
    ) {}

    public record FilaVehiculoSolicitado(String vehiculo, String categoria, long solicitudes, BigDecimal monto) {}
}
