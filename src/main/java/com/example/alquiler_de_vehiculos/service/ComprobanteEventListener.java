package com.example.alquiler_de_vehiculos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.alquiler_de_vehiculos.util.ComprobanteEvents;

/**
 * Envía los comprobantes por correo una vez confirmada la transacción,
 * en segundo plano para no demorar la respuesta al usuario.
 */
@Component
public class ComprobanteEventListener {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteEventListener.class);

    private final ComprobanteService comprobanteService;

    public ComprobanteEventListener(ComprobanteService comprobanteService) {
        this.comprobanteService = comprobanteService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarPago(ComprobanteEvents.PagoRegistrado evento) {
        try {
            comprobanteService.enviarRecibo(evento.idPago());
        } catch (Exception e) {
            log.warn("No se pudo enviar el recibo del pago {}: {}", evento.idPago(), e.getMessage());
        }
    }

    // La entrega de llaves ya no genera ni envía PDF: los documentos son la boleta electrónica y el comprobante de finalización.
}
