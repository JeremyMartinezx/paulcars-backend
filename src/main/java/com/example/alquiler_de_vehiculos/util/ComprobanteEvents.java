package com.example.alquiler_de_vehiculos.util;

/**
 * Eventos que disparan el envío automático de comprobantes por correo
 * una vez que la transacción que los originó ya se confirmó.
 */
public final class ComprobanteEvents {

    private ComprobanteEvents() {
    }

    public record PagoRegistrado(Integer idPago) {}

    public record EntregaRegistrada(Integer idAlquiler) {}
}
