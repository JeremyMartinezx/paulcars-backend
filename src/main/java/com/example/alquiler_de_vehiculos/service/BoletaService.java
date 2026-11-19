package com.example.alquiler_de_vehiculos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.config.EmpresaProperties;
import com.example.alquiler_de_vehiculos.config.SunatProperties;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.BoletaElectronica;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.repository.BoletaElectronicaRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;

/**
 * Emite la boleta de venta electrónica de cada pago.
 * <ul>
 *   <li><b>Modo NUBEFACT</b> (hay URL y token): envía la boleta a SUNAT mediante el PSE y guarda hash, QR y enlaces.</li>
 *   <li><b>Modo DEMO</b> (sin credenciales): genera la boleta localmente con serie y correlativo, QR y hash,
 *       marcada como documento de demostración sin validez tributaria.</li>
 * </ul>
 */
@Service
public class BoletaService {

    private static final Logger log = LoggerFactory.getLogger(BoletaService.class);
    private static final DateTimeFormatter FECHA_NUBEFACT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_QR = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Object CANDADO = new Object();

    private final BoletaElectronicaRepository repository;
    private final PagoRepository pagoRepository;
    private final SunatProperties sunat;
    private final EmpresaProperties empresa;
    private final TransactionTemplate tx;

    public BoletaService(BoletaElectronicaRepository repository, PagoRepository pagoRepository,
                         SunatProperties sunat, EmpresaProperties empresa, PlatformTransactionManager tm) {
        this.repository = repository;
        this.pagoRepository = pagoRepository;
        this.sunat = sunat;
        this.empresa = empresa;
        this.tx = new TransactionTemplate(tm);
        // Transacción propia: la emisión puede dispararse desde una consulta de solo lectura (descarga del PDF)
        this.tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Boleta ya emitida para el pago, si existe (no emite nada). */
    public Optional<BoletaElectronica> porId(Integer idBoleta) {
        return repository.findById(idBoleta);
    }

    public Optional<BoletaElectronica> buscar(Integer idPago) {
        return idPago == null ? Optional.empty() : repository.findByPago_IdPago(idPago);
    }

    /** Devuelve la boleta del pago y, si todavía no existe, la emite. Es seguro llamarla varias veces. */
    public BoletaElectronica obtenerOEmitir(Integer idPago) {
        Optional<BoletaElectronica> existente = buscar(idPago);
        if (existente.isPresent()) return existente.get();
        // Un solo emisor a la vez: el correlativo no puede repetirse
        synchronized (CANDADO) {
            return tx.execute(status -> repository.findByPago_IdPago(idPago).orElseGet(() -> emitir(idPago)));
        }
    }

    private BoletaElectronica emitir(Integer idPago) {
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado"));
        if ("REEMBOLSO".equalsIgnoreCase(pago.getTipoPago())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Una devolución no genera boleta: genera una nota de crédito");
        }
        Alquiler alq = pago.getAlquiler();
        Cliente cliente = alq.getCliente();

        BigDecimal total = (pago.getMontoTotal() != null ? pago.getMontoTotal() : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(sunat.getIgv()).movePointLeft(2));
        BigDecimal gravada = total.divide(factor, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(gravada);

        BoletaElectronica b = new BoletaElectronica();
        b.setTipoComprobante("BOLETA");
        b.setPago(pago);
        b.setSerie(sunat.getSerieBoleta());
        b.setCorrelativo(repository.ultimoCorrelativo(sunat.getSerieBoleta()) + 1);
        b.setFechaEmision(pago.getFechaHoraPago() != null ? pago.getFechaHoraPago() : LocalDateTime.now());
        b.setOpGravada(gravada);
        b.setIgv(igv);
        b.setTotal(total);
        asignarCliente(b, cliente);

        if (sunat.emisionReal()) {
            enviarANubefact(b, alq, pago, cliente);
        } else {
            emitirDemo(b);
        }
        BoletaElectronica guardada = repository.save(b);
        log.info("Boleta {} emitida ({}) por el pago {}", guardada.numero(), guardada.getModo(), idPago);
        return guardada;
    }

    private void asignarCliente(BoletaElectronica b, Cliente c) {
        String doc = c != null && c.getNumeroDocumento() != null ? c.getNumeroDocumento().trim() : "";
        String tipo = c != null && c.getTipoDocumento() != null ? c.getTipoDocumento().trim().toUpperCase() : "";
        if (doc.isEmpty()) {
            b.setClienteTipoDoc("0");
            b.setClienteNumDoc("-");
        } else {
            b.setClienteTipoDoc(switch (tipo) {
                case "CE", "CARNET DE EXTRANJERIA", "CARNET DE EXTRANJERÍA" -> "4";
                case "PASAPORTE" -> "7";
                default -> "1";
            });
            b.setClienteNumDoc(doc);
        }
        String nombre = c != null ? ((c.getNombres() != null ? c.getNombres() : "") + " "
                + (c.getApellidos() != null ? c.getApellidos() : "")).trim() : "";
        b.setClienteNombre(nombre.isEmpty() ? "CLIENTES VARIOS" : nombre.toUpperCase());
    }

    // ───────── Modo demostración ─────────

    private void emitirDemo(BoletaElectronica b) {
        b.setModo(BoletaElectronica.MODO_DEMO);
        b.setEstado("EMITIDA_DEMO");
        b.setCadenaQr(cadenaQr(b));
        b.setCodigoHash(hash(b.getCadenaQr()));
        b.setMensajeSunat("Documento de demostración: no fue enviado a SUNAT y no tiene validez tributaria.");
    }

    /** Cadena del código QR con el formato de SUNAT: RUC|tipo|serie|número|IGV|total|fecha|tipo doc|número doc| */
    private String cadenaQr(BoletaElectronica b) {
        return String.join("|", empresa.getRuc() == null ? "" : empresa.getRuc().trim(), "03", b.getSerie(),
                String.valueOf(b.getCorrelativo()), b.getIgv().toPlainString(), b.getTotal().toPlainString(),
                b.getFechaEmision().format(FECHA_QR), b.getClienteTipoDoc(), b.getClienteNumDoc()) + "|";
    }

    private String hash(String texto) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest).substring(0, 28);
        } catch (Exception e) {
            return "";
        }
    }

    // ───────── Modo real: API JSON de Nubefact ─────────

    @SuppressWarnings("unchecked")
    private void enviarANubefact(BoletaElectronica b, Alquiler alq, Pago pago, Cliente cliente) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("unidad_de_medida", "ZZ"); // servicio
        item.put("codigo", String.format("ALQ-%06d", alq.getIdAlquiler()));
        item.put("descripcion", descripcion(alq, pago));
        item.put("cantidad", 1);
        item.put("valor_unitario", b.getOpGravada());
        item.put("precio_unitario", b.getTotal());
        item.put("subtotal", b.getOpGravada());
        item.put("tipo_de_igv", 1); // gravado - operación onerosa
        item.put("igv", b.getIgv());
        item.put("total", b.getTotal());
        item.put("anticipo_regularizacion", false);

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("operacion", "generar_comprobante");
        cuerpo.put("tipo_de_comprobante", 2); // 2 = boleta
        cuerpo.put("serie", b.getSerie());
        cuerpo.put("numero", b.getCorrelativo());
        cuerpo.put("sunat_transaction", 1);
        cuerpo.put("cliente_tipo_de_documento", "0".equals(b.getClienteTipoDoc()) ? "-" : b.getClienteTipoDoc());
        cuerpo.put("cliente_numero_de_documento", b.getClienteNumDoc());
        cuerpo.put("cliente_denominacion", b.getClienteNombre());
        cuerpo.put("cliente_direccion", cliente != null && cliente.getDireccion() != null ? cliente.getDireccion() : "");
        cuerpo.put("cliente_email", cliente != null && cliente.getEmail() != null ? cliente.getEmail() : "");
        cuerpo.put("fecha_de_emision", b.getFechaEmision().format(FECHA_NUBEFACT));
        cuerpo.put("moneda", 1); // soles
        cuerpo.put("porcentaje_de_igv", BigDecimal.valueOf(sunat.getIgv()).setScale(2));
        cuerpo.put("total_gravada", b.getOpGravada());
        cuerpo.put("total_igv", b.getIgv());
        cuerpo.put("total", b.getTotal());
        cuerpo.put("detraccion", false);
        cuerpo.put("enviar_automaticamente_a_la_sunat", true);
        cuerpo.put("enviar_automaticamente_al_cliente", false); // el correo lo envía Paul Car's
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item);
        cuerpo.put("items", items);

        Map<String, Object> respuesta;
        try {
            respuesta = RestClient.create().post()
                    .uri(sunat.getNubefactUrl())
                    .header("Authorization", sunat.getNubefactToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo emitir la boleta electrónica ante SUNAT: " + e.getMessage());
        }
        if (respuesta == null || respuesta.get("errors") != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El proveedor de facturación rechazó la boleta: " + (respuesta != null ? respuesta.get("errors") : "sin respuesta"));
        }

        b.setModo(BoletaElectronica.MODO_REAL);
        boolean aceptada = Boolean.TRUE.equals(respuesta.get("aceptada_por_sunat"));
        b.setEstado(aceptada ? "ACEPTADA" : "PENDIENTE_SUNAT");
        b.setCodigoHash(texto(respuesta.get("codigo_hash")));
        b.setCadenaQr(texto(respuesta.get("cadena_para_codigo_qr")));
        b.setEnlacePdf(texto(respuesta.get("enlace_del_pdf")));
        b.setEnlaceXml(texto(respuesta.get("enlace_del_xml")));
        b.setMensajeSunat(texto(respuesta.get("sunat_description")));
        if (b.getCadenaQr() == null || b.getCadenaQr().isBlank()) {
            b.setCadenaQr(cadenaQr(b));
        }
    }

    private String texto(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    // ───────── Nota de crédito (devoluciones) ─────────

    /**
     * Emite la nota de crédito electrónica de una devolución. Anula la boleta (código 01, anulación de la operación)
     * cuando se devuelve todo el importe, o la disminuye (código 09) cuando la devolución es parcial.
     * Es seguro llamarla varias veces: si ya existe, la devuelve.
     */
    public BoletaElectronica emitirNotaCredito(Integer idPagoReembolso) {
        Optional<BoletaElectronica> existente = buscar(idPagoReembolso);
        if (existente.isPresent()) return existente.get();
        synchronized (CANDADO) {
            return tx.execute(status -> repository.findByPago_IdPago(idPagoReembolso).orElseGet(() -> crearNotaCredito(idPagoReembolso)));
        }
    }

    private BoletaElectronica crearNotaCredito(Integer idPagoReembolso) {
        Pago reembolso = pagoRepository.findById(idPagoReembolso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Devolución no encontrada"));
        BoletaElectronica original = reembolso.getIdPagoReferencia() == null ? null
                : repository.findByPago_IdPago(reembolso.getIdPagoReferencia()).orElse(null);
        if (original == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago que se devuelve no tiene boleta que modificar");
        }

        BigDecimal total = reembolso.getMontoTotal().abs().setScale(2, RoundingMode.HALF_UP);
        BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(sunat.getIgv()).movePointLeft(2));
        BigDecimal gravada = total.divide(factor, 2, RoundingMode.HALF_UP);
        boolean total100 = total.compareTo(original.getTotal()) >= 0;

        BoletaElectronica nc = new BoletaElectronica();
        nc.setTipoComprobante("NOTA_CREDITO");
        nc.setPago(reembolso);
        nc.setSerie(sunat.getSerieNotaCredito());
        nc.setCorrelativo(repository.ultimoCorrelativo(sunat.getSerieNotaCredito()) + 1);
        nc.setFechaEmision(LocalDateTime.now());
        nc.setOpGravada(gravada);
        nc.setIgv(total.subtract(gravada));
        nc.setTotal(total);
        nc.setClienteTipoDoc(original.getClienteTipoDoc());
        nc.setClienteNumDoc(original.getClienteNumDoc());
        nc.setClienteNombre(original.getClienteNombre());
        nc.setDocumentoReferencia(original.numero());
        nc.setMotivoNota(total100 ? "01 - Anulación de la operación" : "09 - Disminución en el valor");

        if (sunat.emisionReal()) {
            enviarNotaCreditoANubefact(nc, original, total100);
        } else {
            nc.setModo(BoletaElectronica.MODO_DEMO);
            nc.setEstado("EMITIDA_DEMO");
            nc.setCadenaQr(String.join("|", empresa.getRuc() == null ? "" : empresa.getRuc().trim(), "07", nc.getSerie(),
                    String.valueOf(nc.getCorrelativo()), nc.getIgv().toPlainString(), nc.getTotal().toPlainString(),
                    nc.getFechaEmision().format(FECHA_QR), nc.getClienteTipoDoc(), nc.getClienteNumDoc()) + "|");
            nc.setCodigoHash(hash(nc.getCadenaQr()));
            nc.setMensajeSunat("Nota de crédito de demostración: no fue enviada a SUNAT.");
        }
        BoletaElectronica guardada = repository.save(nc);
        log.info("Nota de crédito {} emitida ({}) sobre la boleta {}", guardada.numero(), guardada.getModo(), original.numero());
        return guardada;
    }

    @SuppressWarnings("unchecked")
    private void enviarNotaCreditoANubefact(BoletaElectronica nc, BoletaElectronica original, boolean anulacionTotal) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("unidad_de_medida", "ZZ");
        item.put("codigo", "DEVOLUCION");
        item.put("descripcion", "Devolución por cancelación de servicio de alquiler de vehículo");
        item.put("cantidad", 1);
        item.put("valor_unitario", nc.getOpGravada());
        item.put("precio_unitario", nc.getTotal());
        item.put("subtotal", nc.getOpGravada());
        item.put("tipo_de_igv", 1);
        item.put("igv", nc.getIgv());
        item.put("total", nc.getTotal());
        item.put("anticipo_regularizacion", false);

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("operacion", "generar_comprobante");
        cuerpo.put("tipo_de_comprobante", 3); // 3 = nota de crédito
        cuerpo.put("serie", nc.getSerie());
        cuerpo.put("numero", nc.getCorrelativo());
        cuerpo.put("sunat_transaction", 1);
        cuerpo.put("cliente_tipo_de_documento", "0".equals(nc.getClienteTipoDoc()) ? "-" : nc.getClienteTipoDoc());
        cuerpo.put("cliente_numero_de_documento", nc.getClienteNumDoc());
        cuerpo.put("cliente_denominacion", nc.getClienteNombre());
        cuerpo.put("cliente_direccion", "");
        cuerpo.put("cliente_email", "");
        cuerpo.put("fecha_de_emision", nc.getFechaEmision().format(FECHA_NUBEFACT));
        cuerpo.put("moneda", 1);
        cuerpo.put("porcentaje_de_igv", BigDecimal.valueOf(sunat.getIgv()).setScale(2));
        cuerpo.put("total_gravada", nc.getOpGravada());
        cuerpo.put("total_igv", nc.getIgv());
        cuerpo.put("total", nc.getTotal());
        cuerpo.put("detraccion", false);
        // Boleta que se modifica y motivo según el catálogo 09 de SUNAT (1 = anulación de la operación, 9 = disminución en el valor)
        cuerpo.put("documento_que_se_modifica_tipo", 2);
        cuerpo.put("documento_que_se_modifica_serie", original.getSerie());
        cuerpo.put("documento_que_se_modifica_numero", original.getCorrelativo());
        cuerpo.put("tipo_de_nota_de_credito", anulacionTotal ? 1 : 9);
        cuerpo.put("enviar_automaticamente_a_la_sunat", true);
        cuerpo.put("enviar_automaticamente_al_cliente", false);
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(item);
        cuerpo.put("items", items);

        Map<String, Object> respuesta;
        try {
            respuesta = RestClient.create().post()
                    .uri(sunat.getNubefactUrl())
                    .header("Authorization", sunat.getNubefactToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo emitir la nota de crédito ante SUNAT: " + e.getMessage());
        }
        if (respuesta == null || respuesta.get("errors") != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "El proveedor de facturación rechazó la nota de crédito: " + (respuesta != null ? respuesta.get("errors") : "sin respuesta"));
        }
        nc.setModo(BoletaElectronica.MODO_REAL);
        nc.setEstado(Boolean.TRUE.equals(respuesta.get("aceptada_por_sunat")) ? "ACEPTADA" : "PENDIENTE_SUNAT");
        nc.setCodigoHash(texto(respuesta.get("codigo_hash")));
        nc.setCadenaQr(texto(respuesta.get("cadena_para_codigo_qr")));
        nc.setEnlacePdf(texto(respuesta.get("enlace_del_pdf")));
        nc.setEnlaceXml(texto(respuesta.get("enlace_del_xml")));
        nc.setMensajeSunat(texto(respuesta.get("sunat_description")));
    }

    /** Descripción del servicio que se factura. */
    public String descripcion(Alquiler alq, Pago pago) {
        String vehiculo = "vehículo";
        if (alq.getVehiculoModelo() != null) {
            String marca = alq.getVehiculoModelo().getMarca() != null ? alq.getVehiculoModelo().getMarca().getNombreMarca() : "";
            vehiculo = (marca + " " + alq.getVehiculoModelo().getNombreModelo()).trim();
        }
        String placa = alq.getVehiculoUnidad() != null ? " placa " + alq.getVehiculoUnidad().getPlaca() : "";
        int dias = alq.getDias() != null ? alq.getDias() : 1;
        String concepto = "LIQUIDACION".equalsIgnoreCase(pago.getTipoPago()) ? "Liquidación del alquiler" : "Servicio de alquiler";
        return String.format("%s de vehículo %s%s - %d %s (%s al %s) - Contrato ALQ-%06d", concepto, vehiculo, placa, dias,
                dias == 1 ? "día" : "días", alq.getFechaInicio(), alq.getFechaFinEstimada(), alq.getIdAlquiler());
    }
}
