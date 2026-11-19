package com.example.alquiler_de_vehiculos.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;

/**
 * Orquesta la generación de comprobantes en PDF y su envío por correo al cliente.
 */
@Service
public class ComprobanteService {

    private static final Logger log = LoggerFactory.getLogger(ComprobanteService.class);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String PDF = "application/pdf";

    private final ComprobantePdfService pdf;
    private final EmailService email;
    private final PagoRepository pagoRepository;
    private final AlquilerRepository alquilerRepository;
    private final BoletaService boletaService;

    public ComprobanteService(ComprobantePdfService pdf, EmailService email,
                              PagoRepository pagoRepository, AlquilerRepository alquilerRepository,
                              BoletaService boletaService) {
        this.pdf = pdf;
        this.email = email;
        this.pagoRepository = pagoRepository;
        this.alquilerRepository = alquilerRepository;
        this.boletaService = boletaService;
    }

    /**
     * Los comprobantes del contrato solo existen cuando el servicio está pagado: reserva pagada, en ruta o finalizado.
     * Una solicitud pendiente, una reserva por pagar o un contrato anulado no generan PDF.
     */
    public void exigirServicioPagado(Alquiler alq) {
        String estado = alq.getEstado() != null && alq.getEstado().getNombreEstado() != null
                ? alq.getEstado().getNombreEstado().toUpperCase() : "";
        switch (estado) {
            case "ACTIVO", "EN_CURSO", "EN CURSO", "FINALIZADO", "CERRADO_CON_DEUDA" -> {
                return;
            }
            case "RESERVADO", "PAGADO", "CONFIRMADO" -> {
                BigDecimal pagado = pagoRepository.findByAlquiler_IdAlquiler(alq.getIdAlquiler()).stream()
                        .filter(p -> p.getTipoPago() == null || "ALQUILER".equalsIgnoreCase(p.getTipoPago()))
                        .map(Pago::getMontoTotal).filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal total = alq.getPrecioTotal() != null ? BigDecimal.valueOf(alq.getPrecioTotal()) : BigDecimal.ZERO;
                if (pagado.signum() > 0 && pagado.compareTo(total) >= 0) return;
            }
            default -> { }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El comprobante en PDF solo está disponible cuando el servicio está pagado.");
    }

    // ── Búsquedas ──

    public Pago buscarPago(Integer idPago) {
        return pagoRepository.findById(idPago)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
    }

    public Alquiler buscarAlquiler(Integer idAlquiler) {
        return alquilerRepository.findById(idAlquiler)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado"));
    }

    // ── PDF para descarga ──

    @Transactional(readOnly = true)
    public byte[] reciboPdf(Integer idPago) {
        return pdf.reciboPago(buscarPago(idPago));
    }

    /** PDF de la nota de crédito de una devolución de esa reserva. */
    @Transactional(readOnly = true)
    public byte[] notaCreditoPdf(Integer idAlquiler, Integer idBoleta) {
        com.example.alquiler_de_vehiculos.model.BoletaElectronica n = boletaService.porId(idBoleta)
                .filter(b -> b.esNotaCredito() && b.getPago() != null && b.getPago().getAlquiler() != null
                        && idAlquiler.equals(b.getPago().getAlquiler().getIdAlquiler()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Nota de crédito no encontrada"));
        return pdf.notaCredito(n);
    }

    @Transactional(readOnly = true)
    public byte[] comprobantePdf(Integer idAlquiler) {
        Alquiler a = buscarAlquiler(idAlquiler);
        exigirServicioPagado(a);
        return pdf.comprobanteAlquiler(a);
    }

    @Transactional(readOnly = true)
    public byte[] actaFinalizacionPdf(Integer idAlquiler) {
        Alquiler a = buscarAlquiler(idAlquiler);
        String estado = a.getEstado() != null ? String.valueOf(a.getEstado().getNombreEstado()).toUpperCase() : "";
        if (!"FINALIZADO".equals(estado) && !"CERRADO_CON_DEUDA".equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El acta de finalización solo existe cuando el alquiler está finalizado.");
        }
        return pdf.actaFinalizacion(a);
    }

    @Transactional(readOnly = true)
    public byte[] actaEntregaPdf(Integer idAlquiler) {
        Alquiler a = buscarAlquiler(idAlquiler);
        if (a.getFechaEntrega() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este alquiler todavía no tiene acta de entrega");
        }
        return pdf.actaEntrega(a);
    }

    public String nombreArchivoRecibo(Pago p) {
        return pdf.numeroRecibo(p) + ".pdf";
    }

    // ── Envío por correo ──

    @Transactional(readOnly = true)
    public boolean enviarRecibo(Integer idPago) {
        Pago pago = buscarPago(idPago);
        Alquiler alq = pago.getAlquiler();
        Cliente c = alq.getCliente();
        boletaService.obtenerOEmitir(idPago); // la boleta debe existir antes de armar el número y el PDF
        String numero = pdf.numeroRecibo(pago);
        String vehiculo = nombreVehiculo(alq);
        String metodo = pago.getMetodoPago() != null ? pago.getMetodoPago().getNombreMetodo() : "-";
        LocalDateTime cuando = pago.getFechaHoraPago() != null ? pago.getFechaHoraPago()
                : (pago.getFechaValidacion() != null ? pago.getFechaValidacion() : LocalDateTime.now());

        String html = plantilla(
                "Boleta de venta electrónica " + numero,
                "Hola <strong>" + email.escapar(c.getNombres()) + "</strong>, hemos registrado tu pago correctamente. "
                        + "Adjuntamos tu boleta de venta electrónica en formato PDF.",
                new String[][] {
                        { "N° de boleta", numero },
                        { "Contrato", codigo(alq) },
                        { "Vehículo", vehiculo },
                        { "Método de pago", metodo },
                        { "Fecha y hora", cuando.format(FECHA_HORA) },
                        { "Importe pagado", soles(pago.getMontoTotal()) }
                },
                "#0d1b3e");

        boolean ok = email.enviarConAdjunto(c.getEmail(), "Boleta de venta electrónica " + numero + " - Paul Car's", html,
                nombreArchivoRecibo(pago), pdf.reciboPago(pago), PDF);
        log.info("Recibo {} {} a {}", numero, ok ? "enviado" : "NO enviado", c.getEmail());
        return ok;
    }

    @Transactional(readOnly = true)
    public boolean enviarActaEntrega(Integer idAlquiler) {
        Alquiler alq = buscarAlquiler(idAlquiler);
        if (alq.getFechaEntrega() == null) return false;
        Cliente c = alq.getCliente();

        String html = plantilla(
                "Acta de entrega del vehículo",
                "Hola <strong>" + email.escapar(c.getNombres()) + "</strong>, te entregamos las llaves de tu vehículo. "
                        + "Adjuntamos el acta de entrega con el estado en el que lo recibiste.",
                new String[][] {
                        { "Contrato", codigo(alq) },
                        { "Vehículo", nombreVehiculo(alq) },
                        { "Placa", alq.getVehiculoUnidad() != null ? alq.getVehiculoUnidad().getPlaca() : "-" },
                        { "Entrega", alq.getFechaEntrega().format(FECHA_HORA) },
                        { "Devolución pactada", alq.getFechaFinEstimada() + " " + (alq.getHoraDevolucion() != null ? alq.getHoraDevolucion() : "") + " hrs - "
                                + (alq.getSedeDevolucion() != null ? alq.getSedeDevolucion() : "") },
                        { "Combustible / Kilometraje", alq.getCombustibleEntrega() + " / "
                                + String.format(Locale.US, "%,d km", alq.getKmEntrega() != null ? alq.getKmEntrega() : 0) }
                },
                "#0d1b3e");

        boolean ok = email.enviarConAdjunto(c.getEmail(), "Acta de entrega " + codigo(alq) + " - Paul Car's", html,
                "ACTA-ENTREGA-" + codigo(alq) + ".pdf", pdf.actaEntrega(alq), PDF);
        log.info("Acta de entrega {} {} a {}", codigo(alq), ok ? "enviada" : "NO enviada", c.getEmail());
        return ok;
    }

    // Reenvío manual del comprobante general del contrato
    @Transactional(readOnly = true)
    public boolean enviarComprobanteAlquiler(Integer idAlquiler) {
        Alquiler alq = buscarAlquiler(idAlquiler);
        exigirServicioPagado(alq);
        Cliente c = alq.getCliente();

        String html = plantilla(
                "Comprobante de tu alquiler",
                "Hola <strong>" + email.escapar(c.getNombres()) + "</strong>, adjuntamos el comprobante actualizado de tu contrato.",
                new String[][] {
                        { "Contrato", codigo(alq) },
                        { "Vehículo", nombreVehiculo(alq) },
                        { "Recojo", alq.getFechaInicio() + " " + (alq.getHoraRecojo() != null ? alq.getHoraRecojo() : "") + " hrs" },
                        { "Devolución", alq.getFechaFinEstimada() + " " + (alq.getHoraDevolucion() != null ? alq.getHoraDevolucion() : "") + " hrs" },
                        { "Estado", alq.getEstado() != null ? alq.getEstado().getNombreEstado() : "-" }
                },
                "#0d1b3e");

        return email.enviarConAdjunto(c.getEmail(), "Comprobante " + codigo(alq) + " - Paul Car's", html,
                "COMPROBANTE-" + codigo(alq) + ".pdf", pdf.comprobanteAlquiler(alq), PDF);
    }

    // ── Plantilla HTML ──

    private String plantilla(String titulo, String intro, String[][] filas, String colorAcento) {
        StringBuilder tabla = new StringBuilder();
        for (String[] f : filas) {
            tabla.append("<tr><td style=\"padding:8px 10px;color:#64748b;font-size:13px;border-bottom:1px solid #e2e8f0;\">")
                    .append(email.escapar(f[0]))
                    .append("</td><td style=\"padding:8px 10px;color:#0d1b3e;font-size:13px;font-weight:600;text-align:right;border-bottom:1px solid #e2e8f0;\">")
                    .append(email.escapar(f[1])).append("</td></tr>");
        }
        return """
            <div style="font-family:'Segoe UI',Helvetica,Arial,sans-serif;max-width:560px;margin:0 auto;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;background:#ffffff;">
                <div style="background:#0d1b3e;padding:20px 24px;border-bottom:3px solid #c9a227;">
                    <h2 style="color:#ffffff;margin:0;font-size:22px;">Paul Car's</h2>
                    <span style="color:#cbd5e1;font-size:12px;">Alquiler de Vehículos en Lima, Perú</span>
                </div>
                <div style="padding:24px;">
                    <h3 style="margin:0 0 10px;color:%s;font-size:18px;">%s</h3>
                    <p style="color:#475569;font-size:14px;line-height:1.5;margin:0 0 16px;">%s</p>
                    <table style="width:100%%;border-collapse:collapse;background:#f8fafc;border-radius:8px;">%s</table>
                    <p style="color:#64748b;font-size:12px;line-height:1.5;margin:18px 0 0;">
                        Conserva este documento como sustento de tu operación. Si no reconoces este movimiento o necesitas ayuda,
                        comunícate con nuestra central de atención o acércate a cualquiera de nuestras sedes.
                    </p>
                </div>
            </div>
            """.formatted(colorAcento, email.escapar(titulo), intro, tabla);
    }

    private String codigo(Alquiler a) {
        return String.format("ALQ-%06d", a.getIdAlquiler() != null ? a.getIdAlquiler() : 0);
    }

    private String nombreVehiculo(Alquiler a) {
        if (a.getVehiculoModelo() == null) return "Vehículo";
        String marca = a.getVehiculoModelo().getMarca() != null ? a.getVehiculoModelo().getMarca().getNombreMarca() : "";
        return (marca + " " + a.getVehiculoModelo().getNombreModelo()).trim();
    }

    private String soles(BigDecimal v) {
        return "S/ " + String.format(Locale.US, "%,.2f", v != null ? v : BigDecimal.ZERO);
    }
}
