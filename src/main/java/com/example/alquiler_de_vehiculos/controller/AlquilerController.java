package com.example.alquiler_de_vehiculos.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.AlquilerCounterRequest;
import com.example.alquiler_de_vehiculos.dto.CheckoutRequest;
import com.example.alquiler_de_vehiculos.dto.DanosDTOs;
import com.example.alquiler_de_vehiculos.dto.DisponibilidadHorariosDTO;
import com.example.alquiler_de_vehiculos.dto.EntregaRequest;
import com.example.alquiler_de_vehiculos.dto.LiquidacionResponse;
import com.example.alquiler_de_vehiculos.dto.RechazoReservaRequest;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.security.SecurityUtils;
import com.example.alquiler_de_vehiculos.service.AlquilerService;
import com.example.alquiler_de_vehiculos.service.ComprobanteService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/alquileres")
@Validated
public class AlquilerController {

    private final AlquilerService alquilerService;
    private final ComprobanteService comprobanteService;
    private final SecurityUtils securityUtils;
    private final com.example.alquiler_de_vehiculos.service.CancelacionService cancelacionService;
    private final com.example.alquiler_de_vehiculos.service.DanoService danoService;

    public AlquilerController(AlquilerService alquilerService, ComprobanteService comprobanteService,
                              SecurityUtils securityUtils,
                              com.example.alquiler_de_vehiculos.service.CancelacionService cancelacionService,
                              com.example.alquiler_de_vehiculos.service.DanoService danoService) {
        this.danoService = danoService;
        this.cancelacionService = cancelacionService;
        this.alquilerService = alquilerService;
        this.comprobanteService = comprobanteService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/horarios")
    public DisponibilidadHorariosDTO consultarHorarios(
            @RequestParam(required = false, defaultValue = "MIRAFLORES") String sede,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return alquilerService.consultarDisponibilidad(sede, fecha != null ? fecha : LocalDate.now());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<Alquiler> listarAlquileres() {
        // Las reservas sin pago cuyo plazo ya pasó se cancelan en este mismo momento
        cancelacionService.cancelarVencidasSiCorresponde();
        List<Alquiler> lista = alquilerService.listarAlquileres();
        lista.forEach(a -> a.setFechaLimitePago(cancelacionService.limitePago(a)));
        return lista;
    }

    @GetMapping("/historial-estados")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<com.example.alquiler_de_vehiculos.dto.HistorialEstadoDTO> historialEstados() {
        return alquilerService.listarHistorialEstados();
    }

    @GetMapping("/{idAlquiler}/historial")
    public List<com.example.alquiler_de_vehiculos.dto.HistorialEstadoDTO> historialDeAlquiler(@PathVariable Integer idAlquiler) {
        // El personal ve cualquiera; el cliente, solo el historial de sus propias reservas
        securityUtils.validarAccesoAlquiler(comprobanteService.buscarAlquiler(idAlquiler));
        return alquilerService.historialDeAlquiler(idAlquiler);
    }

    @GetMapping("/{idAlquiler}")
    public Alquiler obtenerAlquiler(@PathVariable Integer idAlquiler) {
        return alquilerService.obtenerAlquiler(idAlquiler);
    }

    @PostMapping
    public Alquiler crearAlquiler(@RequestParam @NotNull Integer idCliente,
                                  @RequestParam @NotNull Integer idVehiculo,
                                  @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                  @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                                  @RequestParam(required = false) String sedeRecojo,
                                  @RequestParam(required = false) String sedeDevolucion,
                                  @RequestParam(required = false) String horaRecojo,
                                  @RequestParam(required = false) String horaDevolucion,
                                  @RequestParam(required = false) String serviciosExtras) {
        return alquilerService.crearAlquiler(idCliente, idVehiculo, inicio, fin,
                sedeRecojo, sedeDevolucion, horaRecojo, horaDevolucion, serviciosExtras);
    }

    @PostMapping("/counter")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<Alquiler> crearAlquilerCounter(@Valid @RequestBody AlquilerCounterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alquilerService.crearAlquilerCounter(request));
    }

    @GetMapping("/mis-alquileres")
    public List<Alquiler> obtenerMisAlquileres(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        List<Alquiler> propias = alquilerService.obtenerAlquileresPorUsername(authentication.getName());
        // El cliente necesita saber hasta cuándo puede pagar sus reservas aprobadas
        propias.forEach(a -> a.setFechaLimitePago(cancelacionService.limitePago(a)));
        return propias;
    }

    @GetMapping("/cliente/{idCliente}")
    public List<Alquiler> obtenerAlquileresPorCliente(@PathVariable("idCliente") Integer idCliente) {
        return alquilerService.obtenerAlquileresPorCliente(idCliente);
    }

    @GetMapping("/vehiculo/{idVehiculo}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<Alquiler> obtenerAlquileresPorVehiculo(@PathVariable("idVehiculo") Integer idVehiculo) {
        return alquilerService.obtenerAlquileresPorVehiculo(idVehiculo);
    }

    @RequestMapping(value = "/{idAlquiler}/aprobar", method = { RequestMethod.POST, RequestMethod.PUT })
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public Alquiler aprobarReserva(@PathVariable Integer idAlquiler) {
        return alquilerService.aprobarReserva(idAlquiler);
    }

    @PutMapping("/{idAlquiler}/entregar-llaves")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public Alquiler entregarLlaves(@PathVariable Integer idAlquiler,
                                   @RequestBody(required = false) EntregaRequest request) {
        return alquilerService.entregarLlaves(idAlquiler, request);
    }

    // ── Comprobantes en PDF ──

    @GetMapping("/{idAlquiler}/comprobante")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Integer idAlquiler) {
        securityUtils.validarAccesoAlquiler(comprobanteService.buscarAlquiler(idAlquiler));
        return pdf(comprobanteService.comprobantePdf(idAlquiler),
                String.format("COMPROBANTE-ALQ-%06d.pdf", idAlquiler));
    }

    // ── Devolución de dinero de una reserva cancelada ──

    @GetMapping("/{idAlquiler}/devolucion")
    public com.example.alquiler_de_vehiculos.dto.DevolucionDTO devolucion(@PathVariable Integer idAlquiler) {
        securityUtils.validarAccesoAlquiler(comprobanteService.buscarAlquiler(idAlquiler));
        return cancelacionService.devolucion(idAlquiler);
    }

    @GetMapping("/{idAlquiler}/notas-credito/{idBoleta}")
    public ResponseEntity<byte[]> descargarNotaCredito(@PathVariable Integer idAlquiler, @PathVariable Integer idBoleta) {
        securityUtils.validarAccesoAlquiler(comprobanteService.buscarAlquiler(idAlquiler));
        return pdf(comprobanteService.notaCreditoPdf(idAlquiler, idBoleta),
                String.format("NOTA-CREDITO-ALQ-%06d-%d.pdf", idAlquiler, idBoleta));
    }

    @GetMapping("/{idAlquiler}/acta-entrega")
    public ResponseEntity<byte[]> descargarActaEntrega(@PathVariable Integer idAlquiler) {
        securityUtils.validarAccesoAlquiler(comprobanteService.buscarAlquiler(idAlquiler));
        return pdf(comprobanteService.actaEntregaPdf(idAlquiler),
                String.format("ACTA-ENTREGA-ALQ-%06d.pdf", idAlquiler));
    }

    @GetMapping("/{idAlquiler}/acta-finalizacion")
    public ResponseEntity<byte[]> descargarActaFinalizacion(@PathVariable Integer idAlquiler) {
        securityUtils.validarAccesoAlquiler(comprobanteService.buscarAlquiler(idAlquiler));
        return pdf(comprobanteService.actaFinalizacionPdf(idAlquiler),
                String.format("ACTA-FINALIZACION-ALQ-%06d.pdf", idAlquiler));
    }

    @PostMapping("/{idAlquiler}/comprobante/enviar")
    public Map<String, Object> enviarComprobante(@PathVariable Integer idAlquiler) {
        Alquiler alquiler = comprobanteService.buscarAlquiler(idAlquiler);
        securityUtils.validarAccesoAlquiler(alquiler);
        boolean enviado = comprobanteService.enviarComprobanteAlquiler(idAlquiler);
        String correo = alquiler.getCliente() != null ? alquiler.getCliente().getEmail() : "";
        return Map.of("enviado", enviado, "correo", correo == null ? "" : correo);
    }

    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(contenido);
    }

    // ── Cancelación de reservas aprobadas y cambio de vehículo ──

    @GetMapping("/{idAlquiler}/cancelacion/previa")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.PreviaCancelacion previaCancelacion(
            @PathVariable Integer idAlquiler, @RequestParam(required = false) String motivo) {
        return cancelacionService.previa(idAlquiler, motivo);
    }

    @PostMapping("/{idAlquiler}/cancelar-reserva")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.ResultadoCancelacion cancelarReserva(
            @PathVariable Integer idAlquiler,
            @RequestBody com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.CancelarReservaRequest request) {
        // Sin pagos la cancela el asesor; con pagos solo el administrador (se valida en el servicio)
        boolean tienePagos = cancelacionService.previa(idAlquiler, request.motivo()).tienePagos();
        return tienePagos
                ? cancelacionService.cancelarConReembolso(idAlquiler, request)
                : cancelacionService.cancelarSinPago(idAlquiler, request);
    }

    @GetMapping("/{idAlquiler}/unidades-disponibles")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public java.util.List<com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.UnidadDisponible> unidadesDisponibles(
            @PathVariable Integer idAlquiler) {
        return cancelacionService.unidadesDisponibles(idAlquiler);
    }

    @PutMapping("/{idAlquiler}/cambiar-unidad")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.ResultadoCancelacion cambiarUnidad(
            @PathVariable Integer idAlquiler,
            @RequestBody com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.CambiarUnidadRequest request) {
        return cancelacionService.cambiarUnidad(idAlquiler, request.placa(), request.motivo());
    }

    @PostMapping("/{idAlquiler}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public Alquiler rechazarReserva(@PathVariable Integer idAlquiler,
                                    @Valid @RequestBody RechazoReservaRequest request) {
        return alquilerService.rechazarReserva(idAlquiler, request);
    }

    @PostMapping("/{idAlquiler}/checkout-inspeccion")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public LiquidacionResponse checkoutInspeccion(@PathVariable Integer idAlquiler,
                                                  @RequestBody CheckoutRequest request) {
        return alquilerService.checkoutInspeccion(idAlquiler, request);
    }

    // ───────── Daños del vehículo ─────────

    @PostMapping(value = "/danos/fotos", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public java.util.Map<String, String> subirFotoDano(@org.springframework.web.bind.annotation.RequestPart("archivo") org.springframework.web.multipart.MultipartFile archivo) {
        return java.util.Map.of("url", danoService.guardarFoto(archivo));
    }

    @GetMapping("/{idAlquiler}/danos")
    public DanosDTOs.DanosAlquilerDTO danos(@PathVariable Integer idAlquiler) {
        return danoService.resumen(idAlquiler);
    }

    @PostMapping("/{idAlquiler}/danos/vista-previa")
    @PreAuthorize("hasRole('ADMIN')")
    public DanosDTOs.TasacionResultado vistaPreviaTasacion(@PathVariable Integer idAlquiler,
                                                           @RequestBody DanosDTOs.TasacionRequest request) {
        return danoService.vistaPrevia(idAlquiler, request);
    }

    @PostMapping("/{idAlquiler}/danos/tasacion")
    @PreAuthorize("hasRole('ADMIN')")
    public DanosDTOs.DanosAlquilerDTO tasarDanos(@PathVariable Integer idAlquiler,
                                                 @RequestBody DanosDTOs.TasacionRequest request) {
        danoService.tasar(idAlquiler, request);
        return danoService.resumen(idAlquiler);
    }

    @PostMapping("/{idAlquiler}/deuda/pago")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public DanosDTOs.DanosAlquilerDTO cobrarDeuda(@PathVariable Integer idAlquiler,
                                                  @RequestBody DanosDTOs.PagoDeudaRequest request) {
        return danoService.cobrarDeuda(idAlquiler, request);
    }

    @GetMapping("/{idAlquiler}/cargos-previos")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public com.example.alquiler_de_vehiculos.dto.CargosPreviosResponse cargosPrevios(
            @PathVariable Integer idAlquiler, @RequestParam(required = false) String combustible,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime momento) {
        return alquilerService.cargosPrevios(idAlquiler, combustible, momento);
    }

    @GetMapping("/{idAlquiler}/acta")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public LiquidacionResponse obtenerActa(@PathVariable Integer idAlquiler) {
        return alquilerService.obtenerActa(idAlquiler);
    }

    @PutMapping("/{idAlquiler}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CLIENTE')")
    public Alquiler cancelarAlquiler(@PathVariable Integer idAlquiler) {
        return alquilerService.cancelarAlquiler(idAlquiler);
    }
}
