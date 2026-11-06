package com.example.alquiler_de_vehiculos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

import com.example.alquiler_de_vehiculos.model.VehiculoModelo;

public final class TarifarioPaulCars {

    private TarifarioPaulCars() {
    }

    public static final BigDecimal SEGURO_TODO_RIESGO_DIA = new BigDecimal("35.00");
    public static final BigDecimal CONDUCTOR_ADICIONAL_DIA = new BigDecimal("6.50");
    public static final BigDecimal SILLA_BEBE_DIA = new BigDecimal("6.50");

    public static final Set<String> MOTIVOS_RECHAZO = Set.of(
            "DNI_NO_COINCIDE",
            "LICENCIA_VENCIDA_INVALIDA",
            "HISTORIAL_NEGATIVO",
            "SIN_DISPONIBILIDAD_FLOTA",
            "INCUMPLIMIENTO_POLITICA_EDAD",
            "RECHAZADO_DOCUMENTACION",
            "SIN_STOCK_ALTA_DEMANDA");

    public static final String CLIENTE_ACTIVO = "ACTIVO";
    public static final String CLIENTE_LISTA_NEGRA = "LISTA_NEGRA";
    public static final String CLIENTE_OBSERVADO = "LISTA_NEGRA";

    public static String categoria(VehiculoModelo vehiculo) {
        String tipo = vehiculo != null && vehiculo.getTipo() != null && vehiculo.getTipo().getNombreTipo() != null
                ? vehiculo.getTipo().getNombreTipo().toUpperCase()
                : "";
        if (tipo.contains("PICKUP") || tipo.contains("CAMIONETA")) return "PICKUP";
        if (tipo.contains("SUV")) return "SUV";
        if (tipo.contains("HATCH")) return "HATCHBACK";
        return "SEDAN";
    }

    public static BigDecimal deduciblePorCategoria(VehiculoModelo vehiculo) {
        return switch (categoria(vehiculo)) {
            case "HATCHBACK" -> new BigDecimal("2500.00");
            case "SUV" -> new BigDecimal("5000.00");
            case "PICKUP" -> new BigDecimal("7000.00");
            default -> new BigDecimal("3500.00");
        };
    }

    public static BigDecimal garantiaHoldPorCategoria(VehiculoModelo vehiculo) {
        return deduciblePorCategoria(vehiculo);
    }

    public static BigDecimal extrasPorDia(boolean seguro, boolean conductor, boolean silla) {
        BigDecimal total = BigDecimal.ZERO;
        if (seguro) total = total.add(SEGURO_TODO_RIESGO_DIA);
        if (conductor) total = total.add(CONDUCTOR_ADICIONAL_DIA);
        if (silla) total = total.add(SILLA_BEBE_DIA);
        return total;
    }

    public static String textoExtras(boolean seguro, boolean conductor, boolean silla) {
        StringBuilder sb = new StringBuilder();
        if (seguro) sb.append("Seguro Todo Riesgo");
        if (conductor) sb.append(sb.length() > 0 ? ", " : "").append("Conductor Adicional");
        if (silla) sb.append(sb.length() > 0 ? ", " : "").append("Silla de Bebé");
        return sb.length() == 0 ? "Ninguno" : sb.toString();
    }

    public static boolean tieneSeguro(String extras) {
        return extras != null && extras.toLowerCase().contains("seguro");
    }

    public static boolean tieneConductor(String extras) {
        return extras != null && extras.toLowerCase().contains("conductor");
    }

    public static boolean tieneSilla(String extras) {
        return extras != null && extras.toLowerCase().contains("silla");
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
