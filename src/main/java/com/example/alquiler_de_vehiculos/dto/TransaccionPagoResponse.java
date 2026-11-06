package com.example.alquiler_de_vehiculos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransaccionPagoResponse(
    String status,
    String transactionId,
    String message,
    String metodo,
    BigDecimal monto,
    String fecha,
    Integer idAlquiler,
    String nuevoEstado
) {
    public static TransaccionPagoResponse exitoso(String transactionId, String message, String metodo, BigDecimal monto, Integer idAlquiler, String nuevoEstado) {
        return new TransaccionPagoResponse("PAID", transactionId, message, metodo, monto, LocalDateTime.now().toString(), idAlquiler, nuevoEstado);
    }
}
