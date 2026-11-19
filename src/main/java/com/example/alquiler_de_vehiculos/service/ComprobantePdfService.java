package com.example.alquiler_de_vehiculos.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.config.EmpresaProperties;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.BoletaElectronica;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
import com.example.alquiler_de_vehiculos.util.NumeroALetras;
import com.example.alquiler_de_vehiculos.util.TarifarioPaulCars;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import jakarta.transaction.Transactional;

/**
 * Genera los documentos PDF oficiales de Paul Car's:
 * recibo de pago, comprobante del alquiler y acta de entrega del vehículo.
 */
@Service
public class ComprobantePdfService {

    private static final Color NAVY = new Color(0x0D, 0x1B, 0x3E);
    private static final Color GOLD = new Color(0xC9, 0xA2, 0x27);
    private static final Color GRIS_TEXTO = new Color(0x47, 0x55, 0x69);
    private static final Color GRIS_SUAVE = new Color(0xF1, 0xF5, 0xF9);
    private static final Color GRIS_BORDE = new Color(0xCB, 0xD5, 0xE1);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PagoRepository pagoRepository;
    private final EmpresaProperties empresa;
    private final BoletaService boletaService;

    private final DanoService danoService;

    public ComprobantePdfService(PagoRepository pagoRepository, EmpresaProperties empresa, BoletaService boletaService,
                                 DanoService danoService) {
        this.danoService = danoService;
        this.pagoRepository = pagoRepository;
        this.empresa = empresa;
        this.boletaService = boletaService;
    }

    // ───────────────────────── BOLETA DE VENTA ELECTRÓNICA ─────────────────────────

    @Transactional
    public byte[] reciboPago(Pago pago) {
        BoletaElectronica b = boletaService.obtenerOEmitir(pago.getIdPago());
        Alquiler alq = pago.getAlquiler();

        return construir("BOLETA DE VENTA ELECTRÓNICA", b.numero(), "", doc -> {
            doc.add(metaStrip(
                    "Fecha y hora de emisión", fechaHora(b.getFechaEmision()),
                    "Contrato", codigo(alq),
                    "Moneda", "Soles (PEN)"));
            espacio(doc, 8);

            doc.add(seccion("Detalle del servicio contratado", datosDelServicio(alq)));
            espacio(doc, 6);

            List<String[]> cliente = new ArrayList<>();
            cliente.add(new String[] { "Señor(es)", b.getClienteNombre() });
            cliente.add(new String[] { etiquetaDocumento(b.getClienteTipoDoc()), b.getClienteNumDoc() });
            if (alq.getCliente() != null && alq.getCliente().getDireccion() != null && !alq.getCliente().getDireccion().isBlank()) {
                cliente.add(new String[] { "Dirección", alq.getCliente().getDireccion().trim() });
            }
            doc.add(seccion("Datos del adquirente", cliente));
            espacio(doc, 6);

            // Ítems con las columnas estándar de SUNAT; los precios incluyen IGV
            doc.add(tablaItems(itemsBoleta(alq, b.getTotal())));
            espacio(doc, 6);

            // Cómo se canceló
            List<String[]> pagoFilas = new ArrayList<>();
            pagoFilas.add(new String[] { "Medio de pago", metodoLegible(pago) });
            if (pago.getRegistradoPor() != null && !pago.getRegistradoPor().isBlank()) {
                pagoFilas.add(new String[] { "Atendido por", pago.getRegistradoPor() });
            }
            doc.add(seccion("Detalle del pago", pagoFilas));
            espacio(doc, 6);

            // Resumen con el total y, justo debajo, el importe en letras
            List<String[]> montos = new ArrayList<>();
            montos.add(new String[] { "Operación gravada", soles(b.getOpGravada()) });
            montos.add(new String[] { "IGV (18%)", soles(b.getIgv()) });
            doc.add(desglose("Resumen de la boleta", montos));
            doc.add(totalDestacado("IMPORTE TOTAL", soles(b.getTotal()), NAVY));
            espacio(doc, 4);
            doc.add(nota(NumeroALetras.soles(b.getTotal())));
        });
    }

    // ───────────────────────── NOTA DE CRÉDITO ELECTRÓNICA ─────────────────────────

    @Transactional
    public byte[] notaCredito(BoletaElectronica n) {
        Pago dev = n.getPago();
        Alquiler alq = dev.getAlquiler();
        return construir("NOTA DE CRÉDITO ELECTRÓNICA", n.numero(), "", doc -> {
            doc.add(metaStrip(
                    "Fecha y hora de emisión", fechaHora(n.getFechaEmision()),
                    "Contrato", codigo(alq),
                    "Moneda", "Soles (PEN)"));
            espacio(doc, 8);

            List<String[]> cliente = new ArrayList<>();
            cliente.add(new String[] { "Señor(es)", n.getClienteNombre() });
            cliente.add(new String[] { etiquetaDocumento(n.getClienteTipoDoc()), n.getClienteNumDoc() });
            doc.add(seccion("Datos del adquirente", cliente));
            espacio(doc, 6);

            List<String[]> modifica = new ArrayList<>();
            modifica.add(new String[] { "Comprobante que modifica", n.getDocumentoReferencia() });
            // Solo el texto del motivo, sin el código del catálogo SUNAT ("01 - ...")
            String motivoNota = n.getMotivoNota() == null ? "" : n.getMotivoNota().replaceFirst("^[0-9]+ *[-–] *", "");
            modifica.add(new String[] { "Motivo", motivoNota });
            modifica.add(new String[] { "Descripción", "Devolución por cancelación del servicio de alquiler de vehículo" });
            doc.add(seccion("Documento que modifica", modifica));
            espacio(doc, 6);

            // Devolución total: se repiten las líneas de la boleta que se modifica
            List<String[]> lineasNota = lineasDelServicio(alq, n.getTotal());
            if (lineasNota != null) {
                doc.add(seccion("Detalle del servicio devuelto", lineasNota, new float[] { 3.2f, 0.8f }));
                espacio(doc, 6);
            }

            // A dónde va el dinero
            List<String[]> destino = new ArrayList<>();
            destino.add(new String[] { "Devuelto por", metodoLegible(dev) });
            if (dev.getMetodoPago() != null && dev.getMetodoPago().getNombreMetodo() != null
                    && dev.getMetodoPago().getNombreMetodo().toUpperCase().contains("EFECTIVO")) {
                destino.add(new String[] { "Entrega", "En efectivo, en la sede" });
            }
            doc.add(seccion("Detalle de la devolución", destino));
            espacio(doc, 6);

            doc.add(nota(NumeroALetras.soles(n.getTotal())));
            espacio(doc, 4);
            List<String[]> montos = new ArrayList<>();
            montos.add(new String[] { "Operación gravada", soles(n.getOpGravada()) });
            montos.add(new String[] { "IGV (18%)", soles(n.getIgv()) });
            doc.add(desglose("Resumen de la nota de crédito", montos));
            doc.add(totalDestacado("IMPORTE DEVUELTO", soles(n.getTotal()), NAVY));
        });
    }

    /**
     * Líneas del cobro: el alquiler (días × precio por día) y cada servicio adicional (días × precio por día).
     * Solo si el comprobante cubre el total del contrato; si es un cobro parcial no se puede desglosar y devuelve null.
     */
    private List<String[]> lineasDelServicio(Alquiler alq, BigDecimal totalComprobante) {
        if (alq == null || alq.getPrecioTotal() == null || totalComprobante == null) return null;
        BigDecimal contrato = bd(alq.getPrecioTotal());
        if (contrato.subtract(totalComprobante).abs().compareTo(new BigDecimal("0.01")) > 0) return null;
        int dias = alq.getDias() != null && alq.getDias() > 0 ? alq.getDias() : 1;
        String unidadDias = dias + (dias == 1 ? " día" : " días");
        BigDecimal base = alq.getPrecioBase() != null ? bd(alq.getPrecioBase()) : contrato;

        List<String[]> filas = new ArrayList<>();
        filas.add(new String[] { "Alquiler de vehículo (" + unidadDias + ")", soles(base) });
        String extras = alq.getServiciosExtras();
        if (TarifarioPaulCars.tieneSeguro(extras)) filas.add(filaExtra("Seguro todo riesgo", TarifarioPaulCars.SEGURO_TODO_RIESGO_DIA, dias, unidadDias));
        if (TarifarioPaulCars.tieneConductor(extras)) filas.add(filaExtra("Conductor adicional", TarifarioPaulCars.CONDUCTOR_ADICIONAL_DIA, dias, unidadDias));
        if (TarifarioPaulCars.tieneSilla(extras)) filas.add(filaExtra("Silla de bebé", TarifarioPaulCars.SILLA_BEBE_DIA, dias, unidadDias));
        return filas;
    }

    private String[] filaExtra(String nombre, BigDecimal porDia, int dias, String unidadDias) {
        return new String[] { nombre + " (" + unidadDias + ")", soles(porDia.multiply(BigDecimal.valueOf(dias))) };
    }

    /** Ítems de la boleta: [días, descripción, precio por día, importe]. Los precios incluyen IGV. */
    private List<String[]> itemsBoleta(Alquiler alq, BigDecimal totalComprobante) {
        String vehiculo = alq != null ? nombreVehiculo(alq.getVehiculoModelo()) : "";
        String placa = alq != null && alq.getVehiculoUnidad() != null ? " (" + alq.getVehiculoUnidad().getPlaca() + ")" : "";
        int dias = alq != null && alq.getDias() != null && alq.getDias() > 0 ? alq.getDias() : 1;
        String descripcion = "Alquiler de vehículo - " + vehiculo + placa;

        List<String[]> items = new ArrayList<>();
        BigDecimal contrato = alq != null && alq.getPrecioTotal() != null ? bd(alq.getPrecioTotal()) : null;
        boolean completo = contrato != null && totalComprobante != null
                && contrato.subtract(totalComprobante).abs().compareTo(new BigDecimal("0.01")) <= 0;
        if (!completo) {
            // Cobro parcial: no se puede desglosar, se muestra una sola línea por lo cobrado
            items.add(new String[] { "-", descripcion, "-", soles(totalComprobante) });
            return items;
        }
        BigDecimal base = alq.getPrecioBase() != null ? bd(alq.getPrecioBase()) : contrato;
        items.add(new String[] { String.valueOf(dias), descripcion, soles(base.divide(BigDecimal.valueOf(dias), 2, RoundingMode.HALF_UP)), soles(base) });
        String extras = alq.getServiciosExtras();
        if (TarifarioPaulCars.tieneSeguro(extras)) items.add(itemExtra("Seguro todo riesgo", TarifarioPaulCars.SEGURO_TODO_RIESGO_DIA, dias));
        if (TarifarioPaulCars.tieneConductor(extras)) items.add(itemExtra("Conductor adicional", TarifarioPaulCars.CONDUCTOR_ADICIONAL_DIA, dias));
        if (TarifarioPaulCars.tieneSilla(extras)) items.add(itemExtra("Silla de bebé", TarifarioPaulCars.SILLA_BEBE_DIA, dias));
        return items;
    }

    /** Vehículo, placa y periodo exacto del alquiler. */
    private List<String[]> datosDelServicio(Alquiler alq) {
        int dias = alq != null && alq.getDias() != null && alq.getDias() > 0 ? alq.getDias() : 1;
        String dia = dias + (dias == 1 ? " día" : " días");
        String vehiculo = alq == null ? "" : nombreVehiculo(alq.getVehiculoModelo())
                + (alq.getVehiculoUnidad() != null ? " - " + alq.getVehiculoUnidad().getPlaca() : "");
        String periodo = alq == null ? "" : "Del " + fecha(alq.getFechaInicio()) + hora(alq.getHoraRecojo())
                + " al " + fecha(alq.getFechaFinEstimada()) + hora(alq.getHoraDevolucion()) + " (" + dia + ")";
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[] { "Vehículo / Placa:", vehiculo });
        filas.add(new String[] { "Periodo de alquiler:", periodo });
        return filas;
    }

    private String[] itemExtra(String nombre, BigDecimal porDia, int dias) {
        return new String[] { String.valueOf(dias), nombre, soles(porDia), soles(porDia.multiply(BigDecimal.valueOf(dias))) };
    }

    private String hora(String h) {
        return h == null || h.isBlank() ? "" : " " + h.trim();
    }

    private PdfPTable tablaItems(List<String[]> items) {
        PdfPTable t = new PdfPTable(new float[] { 0.7f, 3.5f, 1.5f, 1.1f });
        t.setWidthPercentage(100);
        t.setKeepTogether(true);
        String[] cab = { "DÍAS", "DESCRIPCIÓN", "PRECIO POR DÍA", "IMPORTE" };
        for (int i = 0; i < cab.length; i++) {
            PdfPCell h = new PdfPCell(new Phrase(cab[i], fuente(8, Font.BOLD, Color.WHITE)));
            h.setBackgroundColor(NAVY);
            h.setBorder(Rectangle.NO_BORDER);
            h.setPadding(5);
            h.setHorizontalAlignment(i == 1 ? Element.ALIGN_LEFT : i >= 2 ? Element.ALIGN_RIGHT : Element.ALIGN_CENTER);
            t.addCell(h);
        }
        boolean zebra = false;
        for (String[] f : items) {
            for (int i = 0; i < 4; i++) {
                PdfPCell c = new PdfPCell(new Phrase(f[i], fuente(9, i == 3 ? Font.BOLD : Font.NORMAL, i == 3 ? NAVY : GRIS_TEXTO)));
                c.setBackgroundColor(zebra ? GRIS_SUAVE : Color.WHITE);
                c.setBorderColor(GRIS_BORDE);
                c.setBorderWidth(0.5f);
                c.setPadding(5);
                c.setHorizontalAlignment(i == 1 ? Element.ALIGN_LEFT : i >= 2 ? Element.ALIGN_RIGHT : Element.ALIGN_CENTER);
                t.addCell(c);
            }
            zebra = !zebra;
        }
        return t;
    }

    /** "TARJETA" → "Tarjeta"; si el pago no trae método, se toma el del pago original. */
    private String metodoLegible(Pago pago) {
        String m = pago.getMetodoPago() != null ? pago.getMetodoPago().getNombreMetodo() : null;
        if ((m == null || m.isBlank()) && pago.getIdPagoReferencia() != null) {
            m = pagoRepository.findById(pago.getIdPagoReferencia()).map(o -> o.getMetodoPago() != null ? o.getMetodoPago().getNombreMetodo() : null).orElse(null);
        }
        if (m == null || m.isBlank()) return "No registrado";
        m = m.trim().toLowerCase();
        return Character.toUpperCase(m.charAt(0)) + m.substring(1);
    }

    public String numeroRecibo(Pago pago) {
        return boletaService.buscar(pago.getIdPago()).map(BoletaElectronica::numero)
                .orElse(String.format("REC-%06d", pago.getIdPago() != null ? pago.getIdPago() : 0));
    }

    private String etiquetaDocumento(String tipo) {
        return switch (tipo == null ? "" : tipo) {
            case "1" -> "DNI";
            case "4" -> "Carnet de extranjería";
            case "7" -> "Pasaporte";
            default -> "Documento";
        };
    }

    // ───────────────────────── ACTA DE FINALIZACIÓN ─────────────────────────

    @Transactional
    public byte[] actaFinalizacion(Alquiler alq) {
        LocalDateTime pactadaDev = alq.getFechaFinEstimada() == null ? null
                : alq.getFechaFinEstimada().atTime(horaOr(alq.getHoraDevolucion(), java.time.LocalTime.of(18, 0)));

        return construir("ACTA DE FINALIZACIÓN DEL ALQUILER", codigo(alq), "Recepción del vehículo y cierre del contrato", doc -> {
            doc.add(metaStrip(
                    "Fecha y hora de devolución", fechaHora(alq.getFechaCheckout()),
                    "Contrato", codigo(alq),
                    "Estado", DanoService.PENDIENTE.equals(alq.getEstadoDanos()) ? "EN EVALUACIÓN DE DAÑOS"
                            : danoService.deudaPendiente(alq).signum() > 0 ? "CERRADO CON DEUDA" : "FINALIZADO"));
            espacio(doc, 8);

            // Acta breve: solo lo que prueba la entrega y la devolución del vehículo
            List<String[]> cliente = new ArrayList<>();
            cliente.add(new String[] { "Cliente titular", nombreCompleto(alq.getCliente()) });
            cliente.add(new String[] { "Documento", valor(alq.getCliente() != null ? alq.getCliente().getTipoDocumento() : null, "DNI") + " "
                    + valor(alq.getCliente() != null ? alq.getCliente().getNumeroDocumento() : null, "-") });
            doc.add(seccion("Cliente", cliente));
            espacio(doc, 6);

            List<String[]> veh = new ArrayList<>();
            veh.add(new String[] { "Vehículo", nombreVehiculo(alq.getVehiculoModelo())
                    + (alq.getVehiculoUnidad() != null ? "  ·  " + alq.getVehiculoUnidad().getPlaca() : "") });
            veh.add(new String[] { "Entrega de llaves", fechaHora(alq.getFechaEntrega()) });
            veh.add(new String[] { "Devolución pactada", fechaHora(pactadaDev) });
            veh.add(new String[] { "Devolución real", fechaHora(alq.getFechaCheckout()) + "  (" + diferencia(pactadaDev, alq.getFechaCheckout()) + ")" });
            doc.add(seccion("Alquiler", veh));
            espacio(doc, 6);

            List<String[]> estado = new ArrayList<>();
            estado.add(new String[] { "Kilometraje",
                    "Salida " + (alq.getKmEntrega() != null ? String.format(Locale.US, "%,d km", alq.getKmEntrega()) : "-") + "  -  Llegada "
                            + (alq.getKmDevolucion() != null ? String.format(Locale.US, "%,d km", alq.getKmDevolucion()) : "-")
                            + (alq.getKmEntrega() != null && alq.getKmDevolucion() != null
                                    ? "  (" + String.format(Locale.US, "%,d", Math.max(0, alq.getKmDevolucion() - alq.getKmEntrega())) + " km recorridos)" : "") });
            estado.add(new String[] { "Combustible", "Salida " + valor(alq.getCombustibleEntrega(), "-") + "  -  Llegada " + valor(alq.getCombustibleDevolucion(), "-") });
            estado.add(new String[] { "Recibido por", valor(alq.getEntregadoPor(), "Counter") });
            if (alq.getActaObservaciones() != null && !alq.getActaObservaciones().isBlank()) {
                estado.add(new String[] { "Observaciones", alq.getActaObservaciones().trim() });
            }
            doc.add(seccion("Estado del vehículo al devolverlo", estado));
            espacio(doc, 6);

            // Daños: lo que ya tenía al entregarlo y lo nuevo al recibirlo (solo lo nuevo puede cobrarse)
            boolean evaluado = DanoService.EVALUADO.equals(alq.getEstadoDanos());
            List<String[]> danosPrevios = filasDanos(danoService.listar(alq.getIdAlquiler(), "ENTREGA"), false);
            if (!danosPrevios.isEmpty()) {
                doc.add(seccion("Daños al entregar el vehículo", danosPrevios));
                espacio(doc, 6);
            }
            List<String[]> danosNuevos = filasDanos(danoService.listar(alq.getIdAlquiler(), "RECEPCION"), evaluado);
            if (danosNuevos.isEmpty()) {
                danosNuevos.add(new String[] { "Daños", "Ninguno nuevo: el vehículo se recibió en el mismo estado" });
            }
            doc.add(seccion("Daños al recibir el vehículo", danosNuevos));
            espacio(doc, 6);

            // Una sola cuenta, en orden: garantía retenida, lo que se descuenta y, al final, lo que se libera
            BigDecimal cCombustible = alq.getCargoCombustible() != null ? alq.getCargoCombustible() : BigDecimal.ZERO;
            BigDecimal cRetraso = alq.getCargoRetraso() != null ? alq.getCargoRetraso() : BigDecimal.ZERO;
            List<String[]> garantia = new ArrayList<>();
            garantia.add(new String[] { "Garantía retenida en la tarjeta", soles(alq.getGarantiaRetenida()) });
            if (cCombustible.signum() > 0) {
                garantia.add(new String[] { "Cargo por combustible faltante (salió con " + valor(alq.getCombustibleEntrega(), "-")
                        + ", regresó con " + valor(alq.getCombustibleDevolucion(), "-") + ")", "- " + soles(cCombustible) });
            }
            if (cRetraso.signum() > 0) {
                garantia.add(new String[] { "Cargo por retraso en la devolución"
                        + (alq.getMinutosRetraso() != null ? " (" + alq.getMinutosRetraso() + " min)" : ""), "- " + soles(cRetraso) });
            }
            if (DanoService.PENDIENTE.equals(alq.getEstadoDanos())) {
                garantia.add(new String[] { "Daños del vehículo", "Pendiente de evaluación" });
                doc.add(desglose("Garantía", garantia));
                doc.add(totalDestacado("GARANTÍA RETENIDA (EN EVALUACIÓN)", soles(alq.getGarantiaRetenida()), NAVY));
                espacio(doc, 4);
                doc.add(nota("La garantía permanece retenida hasta que el administrador evalúe los daños. Al terminar la evaluación "
                        + "se emite una nueva acta con el detalle de lo descontado y lo liberado."));
            } else {
                if (evaluado) {
                    BigDecimal costo = alq.getCostoDanos() != null ? alq.getCostoDanos() : BigDecimal.ZERO;
                    BigDecimal cubierto = alq.getMontoCubiertoSeguro() != null ? alq.getMontoCubiertoSeguro() : BigDecimal.ZERO;
                    BigDecimal cargoDanos = alq.getCargoDanos() != null ? alq.getCargoDanos() : BigDecimal.ZERO;
                    garantia.add(new String[] { "Costo de reparación de los daños", soles(costo) });
                    boolean conSeguro = TarifarioPaulCars.tieneSeguro(alq.getServiciosExtras());
                    boolean seguroAplico = conSeguro && (alq.getCoberturaSeguroPct() == null || alq.getCoberturaSeguroPct() > 0);
                    if (conSeguro && seguroAplico) {
                        garantia.add(new String[] { "Cubierto por el Seguro Todo Riesgo", soles(cubierto) });
                    } else if (conSeguro) {
                        garantia.add(new String[] { "Seguro Todo Riesgo", "No aplica (exclusión)" });
                    }
                    garantia.add(new String[] { seguroAplico ? "Deducible del seguro a cargo del cliente" : "Daños a cargo del cliente",
                            cargoDanos.signum() > 0 ? "- " + soles(cargoDanos) : soles(BigDecimal.ZERO) });
                }
                doc.add(desglose("Garantía", garantia));
                doc.add(totalDestacado("GARANTÍA LIBERADA", soles(alq.getGarantiaLiberada()), NAVY));
                BigDecimal deudaBruta = DanoService.deudaBruta(alq);
                if (deudaBruta.signum() > 0) {
                    BigDecimal pagada = danoService.deudaPagada(alq.getIdAlquiler());
                    List<String[]> deuda = new ArrayList<>();
                    deuda.add(new String[] { "Cargos que superan la garantía", soles(deudaBruta) });
                    if (pagada.signum() > 0) deuda.add(new String[] { "Pagado por el cliente", "- " + soles(pagada) });
                    espacio(doc, 6);
                    doc.add(desglose("Saldo por cobrar al cliente", deuda));
                    doc.add(totalDestacado("SALDO PENDIENTE", soles(danoService.deudaPendiente(alq)), NAVY));
                }
                if (evaluado && alq.getObservacionTasacion() != null && !alq.getObservacionTasacion().isBlank()) {
                    espacio(doc, 4);
                    doc.add(nota("Observación de la tasación: " + alq.getObservacionTasacion().trim()
                            + (alq.getTasadoPor() != null ? " (evaluado por " + alq.getTasadoPor() + ")" : "")));
                }
            }
        });
    }

    private static String etiquetaTipoDano(String t) {
        return switch (t == null ? "" : t) {
            case "RAYON" -> "Rayón";
            case "ABOLLADURA" -> "Abolladura";
            case "ROTURA" -> "Rotura";
            case "FALTANTE" -> "Faltante";
            default -> "Otro daño";
        };
    }

    /** Una fila por daño: zona, tipo y gravedad a la izquierda; descripción (y costo si ya se evaluó) a la derecha. */
    private List<String[]> filasDanos(List<com.example.alquiler_de_vehiculos.dto.DanosDTOs.DanoDTO> lista, boolean conCosto) {
        List<String[]> filas = new ArrayList<>();
        for (var d : lista) {
            String zona = DanoService.etiquetaZona(d.zona());
            zona = Character.toUpperCase(zona.charAt(0)) + zona.substring(1);
            String detalle = d.descripcion() == null ? "" : d.descripcion().trim();
            if (conCosto && d.costoReparacion() != null) detalle += "  (reparación " + soles(d.costoReparacion()) + ")";
            filas.add(new String[] { zona + " - " + etiquetaTipoDano(d.tipo()) + " (" + d.gravedad().toLowerCase(Locale.ROOT) + ")", detalle });
        }
        return filas;
    }

    private java.time.LocalTime horaOr(String hora, java.time.LocalTime defecto) {
        try {
            return hora == null || hora.isBlank() ? defecto : java.time.LocalTime.parse(hora.trim());
        } catch (java.time.format.DateTimeParseException e) {
            return defecto;
        }
    }

    /** "A tiempo", "20 min tarde" o "15 min antes" (tolerancia de 5 minutos). */
    private String diferencia(LocalDateTime pactada, LocalDateTime real) {
        if (pactada == null || real == null) return "-";
        long min = java.time.temporal.ChronoUnit.MINUTES.between(pactada, real);
        long abs = Math.abs(min);
        if (abs <= 5) return "A tiempo";
        String dur = abs >= 60 ? (abs / 60) + " h" + (abs % 60 == 0 ? "" : " " + (abs % 60) + " min") : abs + " min";
        return min > 0 ? dur + " tarde" : dur + " antes";
    }

    // ───────────────────────── COMPROBANTE DEL ALQUILER ─────────────────────────

    @Transactional
    public byte[] comprobanteAlquiler(Alquiler alq) {
        String estado = estado(alq);
        List<Pago> pagos = pagosDelContrato(alq);
        BigDecimal total = totalContrato(alq);
        BigDecimal pagado = sumaPagos(pagos);
        BigDecimal saldo = total.subtract(pagado).max(BigDecimal.ZERO);
        boolean pagadoTotal = pagado.signum() > 0 && saldo.signum() == 0;

        String titulo = switch (estado) {
            case "PENDIENTE" -> "SOLICITUD DE RESERVA";
            case "ACTIVO", "EN_CURSO" -> "COMPROBANTE DE ALQUILER EN CURSO";
            case "FINALIZADO" -> "LIQUIDACIÓN DE ALQUILER";
            case "CANCELADO", "RECHAZADO", "RECHAZADO_DOCUMENTACION", "CANCELADO_SIN_STOCK" -> "CONSTANCIA DE RESERVA ANULADA";
            default -> pagadoTotal ? "COMPROBANTE DE RESERVA PAGADA" : "ORDEN DE RESERVA";
        };

        return construir(titulo, codigo(alq), "Documento oficial del contrato de alquiler", doc -> {
            doc.add(metaStrip(
                    "Estado", etiquetaEstado(estado, pagadoTotal),
                    "Condición de pago", condicionPago(estado, pagado, saldo),
                    "Emitido", fechaHora(LocalDateTime.now())));
            espacio(doc, 8);

            doc.add(seccion("Datos del cliente", filasCliente(alq.getCliente())));
            espacio(doc, 6);
            doc.add(seccion("Datos del alquiler", filasAlquiler(alq)));
            espacio(doc, 6);

            List<String[]> traza = new ArrayList<>();
            traza.add(new String[] { "Fecha de registro", fechaHora(alq.getFechaRegistro()) });
            traza.add(new String[] { "Último cambio de estado", fechaHora(alq.getFechaUltimoEstado()) });
            if (alq.getFechaEntrega() != null) {
                traza.add(new String[] { "Entrega de llaves (recojo real)", fechaHora(alq.getFechaEntrega()) });
            }
            if (alq.getFechaCheckout() != null) {
                traza.add(new String[] { "Recepción (devolución real)", fechaHora(alq.getFechaCheckout()) });
            }
            doc.add(seccion("Trazabilidad del proceso", traza));
            espacio(doc, 8);

            List<String[]> lineas = new ArrayList<>();
            int dias = alq.getDias() != null ? alq.getDias() : 1;
            BigDecimal base = alq.getPrecioBase() != null ? bd(alq.getPrecioBase()) : total;
            lineas.add(new String[] { "Alquiler base (" + dias + (dias == 1 ? " día" : " días") + ")", soles(base) });
            if (alq.getMontoExtras() != null && alq.getMontoExtras() > 0) {
                lineas.add(new String[] { "Servicios adicionales (" + valor(alq.getServiciosExtras(), "extras") + ")",
                        soles(bd(alq.getMontoExtras())) });
            }
            lineas.add(new String[] { "Total del contrato", soles(total) });
            lineas.add(new String[] { "Pagado a la fecha", soles(pagado) });
            lineas.add(new String[] { "Saldo pendiente", soles(saldo) });
            doc.add(desglose("Desglose económico", lineas));

            if (!pagos.isEmpty()) {
                espacio(doc, 8);
                doc.add(tablaPagos(pagos));
            }

            espacio(doc, 6);
            doc.add(totalDestacado(pagadoTotal ? "TOTAL PAGADO" : "TOTAL DEL CONTRATO",
                    soles(pagadoTotal ? pagado : total), NAVY));

            BigDecimal cCombustible = alq.getCargoCombustible() != null ? alq.getCargoCombustible() : BigDecimal.ZERO;
            BigDecimal cRetraso = alq.getCargoRetraso() != null ? alq.getCargoRetraso() : BigDecimal.ZERO;
            BigDecimal cTotal = cCombustible.add(cRetraso);
            if (alq.getFechaCheckout() != null) {
                espacio(doc, 6);
                List<String[]> cargos = new ArrayList<>();
                cargos.add(new String[] { "Combustible faltante (entrega: " + valor(alq.getCombustibleEntrega(), "-")
                        + ", recepción: " + valor(alq.getCombustibleDevolucion(), "-") + ")", soles(cCombustible) });
                cargos.add(new String[] { alq.getMinutosRetraso() != null && alq.getMinutosRetraso() > 0
                        ? "Retraso en la devolución (" + alq.getMinutosRetraso() + " min)" : "Retraso en la devolución", soles(cRetraso) });
                cargos.add(new String[] { "Total de cargos", soles(cTotal) });
                doc.add(seccion("Cargos de la recepción", cargos));
            }

            if (alq.getGarantiaRetenida() != null) {
                espacio(doc, 6);
                List<String[]> garantia = new ArrayList<>();
                garantia.add(new String[] { "Garantía retenida", soles(alq.getGarantiaRetenida()) });
                if (alq.getGarantiaLiberada() != null) {
                    garantia.add(new String[] { "Garantía liberada", soles(alq.getGarantiaLiberada()) });
                }
                doc.add(seccion("Garantía", garantia));
            }

            if (alq.getFechaEntrega() != null) {
                espacio(doc, 8);
                doc.add(seccion("Acta de entrega", filasEntrega(alq)));
            }
            if (alq.getFechaCheckout() != null) {
                espacio(doc, 8);
                List<String[]> rec = new ArrayList<>();
                rec.add(new String[] { "Fecha de recepción", fechaHora(alq.getFechaCheckout()) });
                rec.add(new String[] { "Observaciones", valor(alq.getActaObservaciones(), "Sin observaciones") });
                doc.add(seccion("Recepción y liquidación", rec));
            }

            espacio(doc, 12);
            doc.add(nota("Documento generado electrónicamente por el sistema de Paul Car's. "
                    + "Las condiciones del servicio y la política de garantía se encuentran detalladas en el contrato de alquiler."));
        });
    }

    // ───────────────────────── ACTA DE ENTREGA ─────────────────────────

    @Transactional
    public byte[] actaEntrega(Alquiler alq) {
        return construir("ACTA DE ENTREGA DEL VEHÍCULO", codigo(alq), "Entrega de llaves e inicio de ruta", doc -> {
            doc.add(metaStrip(
                    "Fecha y hora de entrega", fechaHora(alq.getFechaEntrega()),
                    "Contrato", codigo(alq),
                    "Entregado por", valor(alq.getEntregadoPor(), "Counter")));
            espacio(doc, 8);

            Cliente c = alq.getCliente();
            VehiculoUnidad u = alq.getVehiculoUnidad();
            List<String[]> cliente = new ArrayList<>();
            cliente.add(new String[] { "Cliente titular", nombreCompleto(c) });
            cliente.add(new String[] { "DNI / Licencia", valor(c != null ? c.getNumeroDocumento() : null, "-") + "   /   "
                    + valor(c != null ? c.getLicenciaConducir() : null, "-") });
            cliente.add(new String[] { "Contacto", valor(c != null ? c.getTelefono() : null, "-") + "   |   "
                    + valor(c != null ? c.getEmail() : null, "-") });
            doc.add(seccion("Datos del cliente", cliente));
            espacio(doc, 6);

            List<String[]> veh = new ArrayList<>();
            veh.add(new String[] { "Vehículo", nombreVehiculo(alq.getVehiculoModelo()) + "   -   Placa " + (u != null ? u.getPlaca() : "-")
                    + "   -   " + TarifarioPaulCars.categoria(alq.getVehiculoModelo()) });
            veh.add(new String[] { "Recojo", fecha(alq.getFechaInicio()) + " " + valor(alq.getHoraRecojo(), "") + " hrs  -  "
                    + valor(alq.getSedeRecojo(), "Sede") });
            veh.add(new String[] { "Devolución", fecha(alq.getFechaFinEstimada()) + " " + valor(alq.getHoraDevolucion(), "") + " hrs  -  "
                    + valor(alq.getSedeDevolucion(), "Sede") });
            doc.add(seccion("Datos del alquiler", veh));
            espacio(doc, 6);

            List<String[]> estado = new ArrayList<>();
            estado.add(new String[] { "Kilometraje",
                    alq.getKmEntrega() != null ? String.format(Locale.US, "%,d km", alq.getKmEntrega()) : "-" });
            estado.add(new String[] { "Nivel de combustible", valor(alq.getCombustibleEntrega(), "-") });
            estado.add(new String[] { "Observaciones", valor(alq.getObservacionesEntrega(), "Sin observaciones") });
            doc.add(seccion("Estado del vehículo al momento de la entrega", estado));
            espacio(doc, 6);

            // Estado inicial: los daños que ya tenía no se le cobran al cliente al devolverlo
            List<String[]> previos = filasDanos(danoService.listar(alq.getIdAlquiler(), "ENTREGA"), false);
            if (previos.isEmpty()) previos.add(new String[] { "Daños existentes", "Ninguno: el vehículo se entrega sin daños visibles" });
            doc.add(seccion("Daños al entregar el vehículo", previos));
            espacio(doc, 6);

            List<String[]> checklist = new ArrayList<>();
            String guardados = alq.getChecklistEntrega() == null ? "" : alq.getChecklistEntrega();
            List<String> verificados = List.of(guardados.split("\\|"));
            for (String item : ITEMS_CHECKLIST) {
                checklist.add(new String[] { item, verificados.contains(item) ? "CONFORME" : "NO VERIFICADO" });
            }
            doc.add(seccion("Checklist de entrega", checklist, new float[] { 3f, 1f }));

            if (alq.getGarantiaRetenida() != null) {
                espacio(doc, 6);
                doc.add(seccion("Garantía",
                        List.<String[]>of(new String[] { "Depósito retenido en POS", soles(alq.getGarantiaRetenida()) })));
            }

            espacio(doc, 8);
            doc.add(nota("El cliente declara haber recibido el vehículo en las condiciones descritas, con su documentación y accesorios, "
                    + "y se compromete a devolverlo en la fecha y sede pactadas (" + fecha(alq.getFechaFinEstimada()) + " "
                    + valor(alq.getHoraDevolucion(), "") + " hrs, " + valor(alq.getSedeDevolucion(), "sede acordada") + ")."));
            espacio(doc, 30);
            doc.add(firmas("Firma del cliente", nombreCompleto(c),
                    "Firma del asesor", valor(alq.getEntregadoPor(), "Counter")));
        });
    }

    static final List<String> ITEMS_CHECKLIST = List.of(
            "Tarjeta de propiedad y SOAT vigentes",
            "Llanta de repuesto y gata",
            "Triángulo de seguridad y extintor",
            "Interior limpio y sin daños",
            "Carrocería revisada (rayones o abolladuras)",
            "Luces y accesorios en funcionamiento");

    // ───────────────────────── Datos reutilizables ─────────────────────────

    private List<String[]> filasCliente(Cliente c) {
        List<String[]> f = new ArrayList<>();
        f.add(new String[] { "Cliente titular", nombreCompleto(c) });
        f.add(new String[] { "Documento", valor(c != null ? c.getTipoDocumento() : null, "DNI") + " "
                + valor(c != null ? c.getNumeroDocumento() : null, "-") });
        f.add(new String[] { "Licencia de conducir", valor(c != null ? c.getLicenciaConducir() : null, "-") });
        f.add(new String[] { "Correo electrónico", valor(c != null ? c.getEmail() : null, "-") });
        f.add(new String[] { "Teléfono", valor(c != null ? c.getTelefono() : null, "-") });
        return f;
    }

    private List<String[]> filasAlquiler(Alquiler a) {
        VehiculoModelo m = a.getVehiculoModelo();
        VehiculoUnidad u = a.getVehiculoUnidad();
        List<String[]> f = new ArrayList<>();
        f.add(new String[] { "Contrato", codigo(a) });
        f.add(new String[] { "Vehículo", nombreVehiculo(m) });
        f.add(new String[] { "Placa", u != null ? u.getPlaca() : "Por asignar" });
        f.add(new String[] { "Categoría", TarifarioPaulCars.categoria(m) });
        f.add(new String[] { "Recojo", fecha(a.getFechaInicio()) + " " + valor(a.getHoraRecojo(), "") + " hrs  -  "
                + valor(a.getSedeRecojo(), "Sede") });
        f.add(new String[] { "Devolución", fecha(a.getFechaFinEstimada()) + " " + valor(a.getHoraDevolucion(), "") + " hrs  -  "
                + valor(a.getSedeDevolucion(), "Sede") });
        f.add(new String[] { "Duración", (a.getDias() != null ? a.getDias() : 1) + " día(s)" });
        return f;
    }

    private List<String[]> filasEntrega(Alquiler a) {
        List<String[]> f = new ArrayList<>();
        f.add(new String[] { "Hora real de entrega (recojo)", fechaHora(a.getFechaEntrega()) });
        f.add(new String[] { "Kilometraje", a.getKmEntrega() != null ? String.format(Locale.US, "%,d km", a.getKmEntrega()) : "-" });
        f.add(new String[] { "Nivel de combustible", valor(a.getCombustibleEntrega(), "-") });
        f.add(new String[] { "Observaciones", valor(a.getObservacionesEntrega(), "Sin observaciones") });
        f.add(new String[] { "Entregado por", valor(a.getEntregadoPor(), "Counter") });
        return f;
    }

    private List<Pago> pagosDelContrato(Alquiler a) {
        if (a == null || a.getIdAlquiler() == null) return List.of();
        return pagoRepository.findByAlquiler_IdAlquiler(a.getIdAlquiler()).stream()
                .filter(p -> p.getTipoPago() == null || "ALQUILER".equalsIgnoreCase(p.getTipoPago()))
                .sorted((x, y) -> fechaPago(x).compareTo(fechaPago(y)))
                .toList();
    }

    private BigDecimal sumaPagos(List<Pago> pagos) {
        return pagos.stream().map(Pago::getMontoTotal).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal totalContrato(Alquiler a) {
        if (a.getPrecioTotal() != null && a.getPrecioTotal() > 0) return bd(a.getPrecioTotal());
        int dias = a.getDias() != null && a.getDias() > 0 ? a.getDias() : 1;
        BigDecimal dia = a.getVehiculoModelo() != null ? TarifarioPaulCars.nz(a.getVehiculoModelo().getPrecioDia()) : BigDecimal.ZERO;
        return dia.multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime fechaPago(Pago p) {
        LocalDateTime exacta = p.getFechaHoraPago();
        if (exacta != null && !exacta.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) return exacta;
        if (p.getFechaValidacion() != null) return p.getFechaValidacion();
        return exacta != null ? exacta : LocalDateTime.now();
    }

    private String estado(Alquiler a) {
        return a.getEstado() != null && a.getEstado().getNombreEstado() != null ? a.getEstado().getNombreEstado().toUpperCase() : "";
    }

    private String etiquetaEstado(String estado, boolean pagado) {
        return switch (estado) {
            case "PENDIENTE" -> "PENDIENTE DE APROBACIÓN";
            case "ACTIVO", "EN_CURSO" -> "EN RUTA";
            case "FINALIZADO" -> "FINALIZADO";
            case "CANCELADO", "CANCELADO_SIN_STOCK" -> "CANCELADO";
            case "RECHAZADO", "RECHAZADO_DOCUMENTACION" -> "RECHAZADO";
            default -> pagado ? "RESERVADO - PAGADO" : "RESERVADO - POR PAGAR";
        };
    }

    private String condicionPago(String estado, BigDecimal pagado, BigDecimal saldo) {
        if ("PENDIENTE".equals(estado)) return "En evaluación (sin cobro)";
        if (pagado.signum() > 0 && saldo.signum() == 0) return "Pagado";
        if (pagado.signum() > 0) return "Pago parcial";
        return "Pendiente de pago";
    }

    private String nombreVehiculo(VehiculoModelo m) {
        if (m == null) return "Vehículo";
        String marca = m.getMarca() != null ? m.getMarca().getNombreMarca() : "";
        return (marca + " " + valor(m.getNombreModelo(), "")).trim();
    }

    private String nombreCompleto(Cliente c) {
        if (c == null) return "Cliente";
        return (valor(c.getNombres(), "") + " " + valor(c.getApellidos(), "")).trim();
    }

    private String codigo(Alquiler a) {
        return String.format("ALQ-%06d", a.getIdAlquiler() != null ? a.getIdAlquiler() : 0);
    }

    private BigDecimal bd(Double d) {
        return BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP);
    }

    private String soles(BigDecimal valor) {
        BigDecimal v = valor != null ? valor : BigDecimal.ZERO;
        return "S/ " + String.format(Locale.US, "%,.2f", v);
    }

    private String fecha(LocalDate f) {
        return f != null ? f.format(FECHA) : "-";
    }

    private String fechaHora(LocalDateTime f) {
        if (f == null) return "-";
        return f.toLocalTime().equals(java.time.LocalTime.MIDNIGHT) ? f.format(FECHA) : f.format(FECHA_HORA);
    }

    private String valor(String texto, String defecto) {
        return texto != null && !texto.isBlank() ? texto.trim() : defecto;
    }

    // ───────────────────────── Construcción del PDF ─────────────────────────

    @FunctionalInterface
    private interface Contenido {
        void agregar(Document doc) throws DocumentException;
    }

    private byte[] construir(String titulo, String numero, String subtitulo, Contenido contenido) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 30, 62);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PiePagina(numero, empresa));
            doc.addTitle(titulo + " " + numero);
            doc.addAuthor(empresa.getRazonSocial());
            doc.addCreator(empresa.getNombreComercial());
            doc.open();

            doc.add(cabecera(titulo, numero, subtitulo));
            espacio(doc, 10);
            contenido.agregar(doc);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
    }

    private PdfPTable cabecera(String titulo, String numero, String subtitulo) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] { 2.3f, 1.35f });
        t.setWidthPercentage(100);

        // Izquierda: logo y datos de la empresa
        PdfPCell emisor = new PdfPCell();
        emisor.setBackgroundColor(NAVY);
        emisor.setBorder(Rectangle.NO_BORDER);
        emisor.setPadding(10);
        emisor.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try (InputStream in = getClass().getResourceAsStream("/pdf/logo.png")) {
            if (in != null) {
                Image img = Image.getInstance(in.readAllBytes());
                img.scaleToFit(120, 50);
                emisor.addElement(img);
            } else {
                emisor.addElement(new Phrase(empresa.getNombreComercial().toUpperCase(), fuente(18, Font.BOLD, Color.WHITE)));
            }
        } catch (Exception e) {
            emisor.addElement(new Phrase(empresa.getNombreComercial().toUpperCase(), fuente(18, Font.BOLD, Color.WHITE)));
        }
        Paragraph razon = new Paragraph(empresa.getRazonSocial(), fuente(10, Font.BOLD, Color.WHITE));
        razon.setSpacingBefore(4);
        emisor.addElement(razon);
        Color claro = new Color(0xCB, 0xD5, 0xE1);
        emisor.addElement(new Paragraph(empresa.getDireccion(), fuente(8, Font.NORMAL, claro)));
        emisor.addElement(new Paragraph("Tel. " + empresa.getTelefono() + "  |  " + empresa.getEmail(), fuente(8, Font.NORMAL, claro)));
        t.addCell(emisor);

        // Derecha: recuadro del documento (RUC, tipo y número correlativo)
        PdfPCell caja = new PdfPCell();
        caja.setBorderColor(NAVY);
        caja.setBorderWidth(1.4f);
        caja.setPadding(10);
        caja.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph pr = new Paragraph(empresa.rucTexto(), fuente(10, Font.BOLD, NAVY));
        pr.setAlignment(Element.ALIGN_CENTER);
        Paragraph pt = new Paragraph(titulo, fuente(12, Font.BOLD, NAVY));
        pt.setAlignment(Element.ALIGN_CENTER);
        pt.setSpacingBefore(6);
        Paragraph pn = new Paragraph("N° " + numero, fuente(12, Font.BOLD, NAVY));
        pn.setAlignment(Element.ALIGN_CENTER);
        pn.setSpacingBefore(6);
        caja.addElement(pr);
        caja.addElement(pt);
        caja.addElement(pn);
        if (subtitulo != null && !subtitulo.isBlank()) {
            Paragraph ps = new Paragraph(subtitulo, fuente(7.5f, Font.NORMAL, GRIS_TEXTO));
            ps.setAlignment(Element.ALIGN_CENTER);
            ps.setSpacingBefore(4);
            caja.addElement(ps);
        }
        t.addCell(caja);

        PdfPCell linea = new PdfPCell(new Phrase(" ", fuente(2, Font.NORMAL, GOLD)));
        linea.setColspan(2);
        linea.setBackgroundColor(GOLD);
        linea.setBorder(Rectangle.NO_BORDER);
        linea.setFixedHeight(3);
        t.addCell(linea);
        return t;
    }

    private PdfPTable metaStrip(String l1, String v1, String l2, String v2, String l3, String v3) {
        PdfPTable t = new PdfPTable(3);
        t.setWidthPercentage(100);
        String[][] cols = { { l1, v1 }, { l2, v2 }, { l3, v3 } };
        for (String[] c : cols) {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(GRIS_SUAVE);
            cell.setBorderColor(GRIS_BORDE);
            cell.setBorderWidth(0.6f);
            cell.setPadding(7);
            cell.addElement(new Paragraph(c[0].toUpperCase(), fuente(7, Font.BOLD, GRIS_TEXTO)));
            Paragraph v = new Paragraph(c[1], fuente(10, Font.BOLD, NAVY));
            v.setSpacingBefore(2);
            cell.addElement(v);
            t.addCell(cell);
        }
        return t;
    }

    private PdfPTable seccion(String titulo, List<String[]> filas) {
        return seccion(titulo, filas, new float[] { 1.2f, 2.8f });
    }

    private PdfPTable seccion(String titulo, List<String[]> filas, float[] anchos) {
        PdfPTable t = new PdfPTable(anchos);
        t.setWidthPercentage(100);
        t.setKeepTogether(true);

        PdfPCell h = new PdfPCell(new Phrase(titulo.toUpperCase(), fuente(8.5f, Font.BOLD, Color.WHITE)));
        h.setColspan(2);
        h.setBackgroundColor(NAVY);
        h.setBorder(Rectangle.NO_BORDER);
        h.setPadding(5);
        h.setPaddingLeft(8);
        t.addCell(h);

        boolean zebra = false;
        for (String[] f : filas) {
            Color fondo = zebra ? GRIS_SUAVE : Color.WHITE;
            PdfPCell k = new PdfPCell(new Phrase(f[0], fuente(9, Font.NORMAL, GRIS_TEXTO)));
            PdfPCell v = new PdfPCell(new Phrase(f[1], fuente(9.5f, Font.BOLD, NAVY)));
            for (PdfPCell c : new PdfPCell[] { k, v }) {
                c.setBackgroundColor(fondo);
                c.setBorderColor(GRIS_BORDE);
                c.setBorderWidth(0.5f);
                c.setPadding(5);
                c.setPaddingLeft(8);
            }
            t.addCell(k);
            t.addCell(v);
            zebra = !zebra;
        }
        return t;
    }

    private PdfPTable desglose(String titulo, List<String[]> filas) {
        PdfPTable t = new PdfPTable(new float[] { 3f, 1.2f });
        t.setWidthPercentage(100);
        t.setKeepTogether(true);

        PdfPCell h = new PdfPCell(new Phrase(titulo.toUpperCase(), fuente(8.5f, Font.BOLD, Color.WHITE)));
        h.setColspan(2);
        h.setBackgroundColor(NAVY);
        h.setBorder(Rectangle.NO_BORDER);
        h.setPadding(5);
        h.setPaddingLeft(8);
        t.addCell(h);

        for (String[] f : filas) {
            PdfPCell k = new PdfPCell(new Phrase(f[0], fuente(9.5f, Font.NORMAL, GRIS_TEXTO)));
            PdfPCell v = new PdfPCell(new Phrase(f[1], fuente(9.5f, Font.BOLD, NAVY)));
            v.setHorizontalAlignment(Element.ALIGN_RIGHT);
            for (PdfPCell c : new PdfPCell[] { k, v }) {
                c.setBorderColor(GRIS_BORDE);
                c.setBorderWidth(0.5f);
                c.setPadding(5);
                c.setPaddingLeft(8);
                c.setPaddingRight(8);
            }
            t.addCell(k);
            t.addCell(v);
        }
        return t;
    }

    private PdfPTable tablaPagos(List<Pago> pagos) {
        PdfPTable t = new PdfPTable(new float[] { 0.9f, 1.4f, 1.3f, 1.8f, 1.1f });
        t.setWidthPercentage(100);
        t.setKeepTogether(true);

        PdfPCell h = new PdfPCell(new Phrase("PAGOS REGISTRADOS", fuente(8.5f, Font.BOLD, Color.WHITE)));
        h.setColspan(5);
        h.setBackgroundColor(NAVY);
        h.setBorder(Rectangle.NO_BORDER);
        h.setPadding(5);
        h.setPaddingLeft(8);
        t.addCell(h);

        String[] cab = { "Recibo", "Fecha y hora", "Método", "N° operación", "Monto" };
        for (String c : cab) {
            PdfPCell cell = new PdfPCell(new Phrase(c, fuente(8, Font.BOLD, GRIS_TEXTO)));
            cell.setBackgroundColor(GRIS_SUAVE);
            cell.setBorderColor(GRIS_BORDE);
            cell.setBorderWidth(0.5f);
            cell.setPadding(4);
            t.addCell(cell);
        }
        for (Pago p : pagos) {
            String[] fila = {
                    numeroRecibo(p),
                    fechaHora(fechaPago(p)),
                    p.getMetodoPago() != null ? p.getMetodoPago().getNombreMetodo() : "-",
                    valor(p.getNumeroOperacion(), "-"),
                    soles(p.getMontoTotal()) };
            for (int i = 0; i < fila.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(fila[i], fuente(8.5f, i == 4 ? Font.BOLD : Font.NORMAL, NAVY)));
                cell.setBorderColor(GRIS_BORDE);
                cell.setBorderWidth(0.5f);
                cell.setPadding(4);
                if (i == 4) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(cell);
            }
        }
        return t;
    }

    private PdfPTable totalDestacado(String etiqueta, String monto, Color color) {
        PdfPTable t = new PdfPTable(new float[] { 2.2f, 1.2f });
        t.setWidthPercentage(100);
        PdfPCell l = new PdfPCell(new Phrase(etiqueta, fuente(10, Font.BOLD, color)));
        PdfPCell r = new PdfPCell(new Phrase(monto, fuente(15, Font.BOLD, color)));
        r.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[] { l, r }) {
            c.setBackgroundColor(GRIS_SUAVE);
            c.setBorderColor(color);
            c.setBorderWidth(1.2f);
            c.setPadding(9);
            c.setPaddingLeft(10);
            c.setPaddingRight(10);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            t.addCell(c);
        }
        return t;
    }

    private PdfPTable firmas(String l1, String n1, String l2, String n2) {
        PdfPTable t = new PdfPTable(new float[] { 1f, 0.2f, 1f });
        t.setWidthPercentage(100);
        String[][] firmas = { { l1, n1 }, null, { l2, n2 } };
        for (String[] f : firmas) {
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.NO_BORDER);
            if (f != null) {
                c.setBorder(Rectangle.TOP);
                c.setBorderColor(NAVY);
                c.setBorderWidth(0.8f);
                Paragraph p1 = new Paragraph(f[0], fuente(8.5f, Font.BOLD, NAVY));
                p1.setAlignment(Element.ALIGN_CENTER);
                Paragraph p2 = new Paragraph(f[1], fuente(8.5f, Font.NORMAL, GRIS_TEXTO));
                p2.setAlignment(Element.ALIGN_CENTER);
                c.addElement(p1);
                c.addElement(p2);
            }
            t.addCell(c);
        }
        return t;
    }

    private Paragraph nota(String texto) {
        Paragraph p = new Paragraph(texto, fuente(8, Font.ITALIC, GRIS_TEXTO));
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        return p;
    }

    private void espacio(Document doc, float altura) throws DocumentException {
        Paragraph p = new Paragraph(" ", fuente(altura, Font.NORMAL, Color.WHITE));
        p.setLeading(altura);
        doc.add(p);
    }

    private static Font fuente(float tamano, int estilo, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, tamano, estilo, color);
    }

    // Pie de página con el número de documento y la paginación
    private static final class PiePagina extends PdfPageEventHelper {
        private final String numero;
        private final EmpresaProperties empresa;

        PiePagina(String numero, EmpresaProperties empresa) {
            this.numero = numero;
            this.empresa = empresa;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable pie = new PdfPTable(2);
            try {
                pie.setTotalWidth(document.right() - document.left());
                pie.setWidths(new float[] { 3f, 1f });
                PdfPCell izq = new PdfPCell(new Phrase(empresa.getRazonSocial() + "  |  " + empresa.rucTexto()
                        + "  |  Documento N° " + numero
                        + "\nEmitido el " + LocalDateTime.now().format(FECHA_HORA) + "  |  Documento generado electrónicamente"
                        + "\n" + empresa.lineaContacto(), fuente(7, Font.NORMAL, GRIS_TEXTO)));
                PdfPCell der = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), fuente(7.5f, Font.NORMAL, GRIS_TEXTO)));
                der.setHorizontalAlignment(Element.ALIGN_RIGHT);
                for (PdfPCell c : new PdfPCell[] { izq, der }) {
                    c.setBorder(Rectangle.TOP);
                    c.setBorderColor(GRIS_BORDE);
                    c.setPaddingTop(4);
                    pie.addCell(c);
                }
                pie.writeSelectedRows(0, -1, document.left(), document.bottom() - 8, writer.getDirectContent());
            } catch (DocumentException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
