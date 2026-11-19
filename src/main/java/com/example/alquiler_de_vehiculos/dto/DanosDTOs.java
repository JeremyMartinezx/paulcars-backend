package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Datos de entrada y salida del módulo de daños del vehículo (inspección, tasación y cobro de la deuda). */
public final class DanosDTOs {

    private DanosDTOs() {
    }

    /** Un daño que registra el asesor al entregar o al recibir el vehículo. */
    public record DanoRequest(String zona, String tipo, String gravedad, String descripcion, String fotoUrl) {}

    public record DanoDTO(
            Integer idDano,
            String momento,
            String zona,
            String tipo,
            String gravedad,
            String descripcion,
            String fotoUrl,
            String registradoPor,
            LocalDateTime fechaRegistro,
            BigDecimal costoReparacion
    ) {}

    public record CostoDano(Integer idDano, BigDecimal costo) {}

    /**
     * Tasación del administrador: costo real de reparar cada daño. Si el cliente contrató el Seguro Todo Riesgo, el seguro
     * aplica solo; el administrador marca «seguroNoAplica» únicamente ante una exclusión (y debe indicar el motivo).
     */
    public record TasacionRequest(List<CostoDano> costos, Boolean seguroNoAplica, String observacion) {}

    /** Cómo se reparte el costo de los daños y cómo queda la garantía. */
    public record TasacionResultado(
            BigDecimal costoTotal,
            boolean tieneSeguro,
            /** true si el seguro cubre el daño (el cliente paga solo el deducible). */
            boolean seguroAplica,
            /** Deducible del Seguro Todo Riesgo según la categoría del vehículo. */
            BigDecimal deducible,
            /** Parte del costo que paga el cliente. */
            BigDecimal cargoCliente,
            /** Parte del costo que asume el seguro. */
            BigDecimal cubiertoSeguro,
            BigDecimal garantia,
            BigDecimal cargoCombustible,
            BigDecimal cargoRetraso,
            BigDecimal totalCargos,
            BigDecimal garantiaLiberada,
            /** Lo que supera la garantía y el cliente debe pagar. */
            BigDecimal deuda
    ) {}

    public record DanosAlquilerDTO(
            /** null = sin daños; PENDIENTE_EVALUACION; EVALUADO. */
            String estadoDanos,
            boolean tieneSeguro,
            BigDecimal garantia,
            List<DanoDTO> entrega,
            List<DanoDTO> recepcion,
            /** Reparto vigente (solo si ya fue evaluado). */
            TasacionResultado tasacion,
            String observacionTasacion,
            String tasadoPor,
            LocalDateTime fechaTasacion,
            BigDecimal deudaPagada,
            BigDecimal deudaPendiente
    ) {}

    /** Pago de la deuda por daños o cargos que superaron la garantía. Si no se indica monto, se paga todo el saldo. */
    public record PagoDeudaRequest(Integer idMetodoPago, BigDecimal monto, BigDecimal montoRecibido, String numeroOperacion) {}
}
