package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;

public record ReporteOperativoDTO(
        long solicitudesPendientes,
        long recojosHoy,
        long devolucionesHoy,
        long devolucionesVencidas,
        long pagosHoy,
        BigDecimal cobradoHoy,
        long vehiculosDisponibles,
        long vehiculosEnMantenimiento,
        long vehiculosEnLimpieza
) {}
