package com.example.alquiler_de_vehiculos.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.example.alquiler_de_vehiculos.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alquiler_de_vehiculos.dto.PlinPagoRequest;
import com.example.alquiler_de_vehiculos.dto.RegistrarPagoRequest;
import com.example.alquiler_de_vehiculos.dto.TarjetaPagoRequest;
import com.example.alquiler_de_vehiculos.dto.TransaccionPagoResponse;
import com.example.alquiler_de_vehiculos.dto.YapePagoRequest;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.service.ComprobanteService;
import com.example.alquiler_de_vehiculos.service.PagoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final AlquilerRepository alquilerRepository;
    private final SecurityUtils securityUtils;
    private final ComprobanteService comprobanteService;

    public PagoController(PagoService pagoService, AlquilerRepository alquilerRepository, SecurityUtils securityUtils,
                          ComprobanteService comprobanteService) {
        this.pagoService = pagoService;
        this.alquilerRepository = alquilerRepository;
        this.securityUtils = securityUtils;
        this.comprobanteService = comprobanteService;
    }

    private String validarPropietario(Authentication auth) {
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "CLIENTE";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public Pago registrarPago(@Valid @RequestBody RegistrarPagoRequest request) {
        return pagoService.registrarPago(request);
    }

    @PostMapping("/yape")
    public TransaccionPagoResponse pagarConYape(@Valid @RequestBody YapePagoRequest request, Authentication auth) {
        return pagoService.pagarConYape(request, validarPropietario(auth));
    }

    @PostMapping("/plin")
    public TransaccionPagoResponse pagarConPlin(@Valid @RequestBody PlinPagoRequest request, Authentication auth) {
        return pagoService.pagarConPlin(request, validarPropietario(auth));
    }

    @PostMapping("/tarjeta")
    public TransaccionPagoResponse pagarConTarjeta(@Valid @RequestBody TarjetaPagoRequest request, Authentication auth) {
        return pagoService.pagarConTarjeta(request, validarPropietario(auth));
    }

    @GetMapping("/alquiler/{idAlquiler}")
    public List<Pago> obtenerPagosPorAlquiler(@PathVariable("idAlquiler") Integer idAlquiler) {
        alquilerRepository.findById(idAlquiler).ifPresent(securityUtils::validarAccesoAlquiler);
        return pagoService.obtenerPagosPorAlquiler(idAlquiler);
    }

    @GetMapping("/alquiler/{idAlquiler}/saldo")
    public Map<String, BigDecimal> saldo(@PathVariable("idAlquiler") Integer idAlquiler) {
        alquilerRepository.findById(idAlquiler).ifPresent(securityUtils::validarAccesoAlquiler);
        return alquilerRepository.findById(idAlquiler)
                .map(a -> Map.of(
                        "saldoContrato", pagoService.saldoContrato(a),
                        "saldoDeuda", BigDecimal.ZERO))
                .orElse(Map.of("saldoContrato", BigDecimal.ZERO, "saldoDeuda", BigDecimal.ZERO));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<Pago> listarPagos() {
        return pagoService.listarPagos();
    }

    // ── Recibo de pago en PDF ──

    @GetMapping("/{idPago}/recibo")
    public ResponseEntity<byte[]> descargarRecibo(@PathVariable("idPago") Integer idPago) {
        Pago pago = comprobanteService.buscarPago(idPago);
        securityUtils.validarAccesoAlquiler(pago.getAlquiler());
        byte[] contenido = comprobanteService.reciboPdf(idPago); // emite la boleta si todavía no existe
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(comprobanteService.nombreArchivoRecibo(pago)).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(contenido);
    }

    @PostMapping("/{idPago}/recibo/enviar")
    public Map<String, Object> enviarRecibo(@PathVariable("idPago") Integer idPago) {
        Pago pago = comprobanteService.buscarPago(idPago);
        securityUtils.validarAccesoAlquiler(pago.getAlquiler());
        boolean enviado = comprobanteService.enviarRecibo(idPago);
        String correo = pago.getAlquiler().getCliente() != null ? pago.getAlquiler().getCliente().getEmail() : "";
        return Map.of("enviado", enviado, "correo", correo == null ? "" : correo);
    }
}
