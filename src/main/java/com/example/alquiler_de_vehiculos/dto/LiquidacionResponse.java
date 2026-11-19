package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.alquiler_de_vehiculos.dto.DanosDTOs.DanoDTO;

public record LiquidacionResponse(
        Integer idAlquiler,
        String cliente,
        String documentoCliente,
        String vehiculo,
        String placa,
        String categoria,
        BigDecimal garantiaRetenida,
        BigDecimal garantiaLiberada,
        String estadoAlquiler,
        String estadoVehiculo,
        String estadoCliente,
        String observaciones,
        LocalDateTime fechaCheckout,
        /** Si la unidad llegó a su kilometraje de mantenimiento y quedó fuera de servicio, el aviso para el asesor. */
        String avisoMantenimiento,
        BigDecimal cargoCombustible,
        BigDecimal cargoRetraso,
        Integer minutosRetraso,
        /** Suma de los cargos de la recepción. */
        BigDecimal totalCargos,
        /** Parte de los cargos que supera la garantía y queda por cobrar al cliente. */
        BigDecimal saldoPorCobrar,
        /** null = sin daños; PENDIENTE_EVALUACION; EVALUADO. */
        String estadoDanos,
        boolean tieneSeguro,
        BigDecimal costoDanos,
        Integer coberturaSeguroPct,
        BigDecimal montoCubiertoSeguro,
        /** Parte del costo de los daños que paga el cliente. */
        BigDecimal cargoDanos,
        BigDecimal deudaPagada,
        /** Lo que todavía debe el cliente (saldo por cobrar menos lo ya pagado). */
        BigDecimal deudaPendiente,
        List<DanoDTO> danosEntrega,
        List<DanoDTO> danosRecepcion
) {}
