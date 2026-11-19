package com.example.alquiler_de_vehiculos.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.AlquilerCounterRequest;
import com.example.alquiler_de_vehiculos.dto.CheckoutRequest;
import com.example.alquiler_de_vehiculos.dto.DisponibilidadHorariosDTO;
import com.example.alquiler_de_vehiculos.dto.EntregaRequest;
import com.example.alquiler_de_vehiculos.dto.LiquidacionResponse;
import com.example.alquiler_de_vehiculos.dto.RechazoReservaRequest;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.EstadoAlquiler;
import com.example.alquiler_de_vehiculos.model.EstadoVehiculo;
import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.ClienteRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoAlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoVehiculoRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoModeloRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoUnidadRepository;
import com.example.alquiler_de_vehiculos.security.SecurityUtils;
import com.example.alquiler_de_vehiculos.util.ComprobanteEvents;
import com.example.alquiler_de_vehiculos.util.TarifarioPaulCars;

import jakarta.transaction.Transactional;

@Service
public class AlquilerService {

    private static final Logger log = LoggerFactory.getLogger(AlquilerService.class);

    private final AlquilerRepository alquilerRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoModeloRepository vehiculoModeloRepository;
    private final VehiculoUnidadRepository vehiculoUnidadRepository;
    private final EstadoVehiculoRepository estadoVehiculoRepository;
    private final EstadoAlquilerRepository estadoAlquilerRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final com.example.alquiler_de_vehiculos.repository.HistorialEstadoAlquilerRepository historialRepository;
    private final EmailService emailService;
    private final com.example.alquiler_de_vehiculos.config.PoliticaCancelacionProperties politica;
    private final SecurityUtils securityUtils;
    private final ApplicationEventPublisher eventPublisher;
    private final CancelacionService cancelacionService;
    private final DanoService danoService;

    public AlquilerService(AlquilerRepository alquilerRepository,
                           ClienteRepository clienteRepository,
                           VehiculoModeloRepository vehiculoModeloRepository,
                           VehiculoUnidadRepository vehiculoUnidadRepository,
                           EstadoVehiculoRepository estadoVehiculoRepository,
                           EstadoAlquilerRepository estadoAlquilerRepository,
                           UsuarioRepository usuarioRepository,
                           PagoRepository pagoRepository,
                           com.example.alquiler_de_vehiculos.repository.HistorialEstadoAlquilerRepository historialRepository,
                           EmailService emailService,
                           com.example.alquiler_de_vehiculos.config.PoliticaCancelacionProperties politica,
                           SecurityUtils securityUtils,
                           ApplicationEventPublisher eventPublisher,
                           @org.springframework.context.annotation.Lazy CancelacionService cancelacionService,
                           DanoService danoService) {
        this.danoService = danoService;
        this.alquilerRepository = alquilerRepository;
        this.clienteRepository = clienteRepository;
        this.vehiculoModeloRepository = vehiculoModeloRepository;
        this.vehiculoUnidadRepository = vehiculoUnidadRepository;
        this.estadoVehiculoRepository = estadoVehiculoRepository;
        this.estadoAlquilerRepository = estadoAlquilerRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.historialRepository = historialRepository;
        this.emailService = emailService;
        this.politica = politica;
        this.securityUtils = securityUtils;
        this.eventPublisher = eventPublisher;
        this.cancelacionService = cancelacionService;
    }


    @Transactional
    public Alquiler crearAlquiler(Integer idCliente, Integer idModelo, LocalDate fechaInicio, LocalDate fechaFin,
                                  String sedeRecojo, String sedeDevolucion, String horaRecojo, String horaDevolucion,
                                  String serviciosExtras) {
        validarFechas(fechaInicio, fechaFin, true);
        validarAnticipacion(fechaInicio, horaRecojo);
        validarSedeDomingo(sedeRecojo, fechaInicio, sedeDevolucion, fechaFin);

        Cliente cliente = clienteDeLaSesion();
        if (cliente == null && idCliente != null) {
            cliente = clienteRepository.findById(idCliente).orElse(null);
        }
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado");
        }
        validarClienteHabilitado(cliente);
        validarDocumentosCliente(cliente);

        VehiculoModelo modelo = vehiculoModeloRepository.findById(idModelo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo de vehículo no encontrado"));

        if (Boolean.FALSE.equals(modelo.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El modelo de vehículo no se encuentra activo");
        }

        int stockDisponible = contarStockDisponibleFechas(idModelo, fechaInicio, fechaFin, sedeRecojo);
        if (stockDisponible <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin disponibilidad en la sede " + nombreSede(sedeRecojo)
                    + " para las fechas seleccionadas. Prueba con otra sede u otras fechas.");
        }

        boolean seguro = TarifarioPaulCars.tieneSeguro(serviciosExtras);
        boolean conductor = TarifarioPaulCars.tieneConductor(serviciosExtras);
        boolean silla = TarifarioPaulCars.tieneSilla(serviciosExtras);

        Alquiler alquiler = armarAlquiler(cliente, modelo, fechaInicio, fechaFin, sedeRecojo, sedeDevolucion,
                horaRecojo, horaDevolucion, seguro, conductor, silla);
        alquiler.setEstado(estadoAlquiler("PENDIENTE"));
        alquiler.setVehiculoUnidad(null);

        return alquilerRepository.save(alquiler);
    }


    @Transactional
    public Alquiler crearAlquilerCounter(AlquilerCounterRequest req) {
        validarFechas(req.fechaInicio(), req.fechaFin(), true);

        Cliente cliente = clienteRepository.findById(req.idCliente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        validarClienteHabilitado(cliente);
        validarDocumentosCliente(cliente, false);

        VehiculoModelo modelo = vehiculoModeloRepository.findById(req.idVehiculo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo de vehículo no encontrado"));

        VehiculoUnidad unidad = buscarUnidadLibreParaFechas(modelo.getIdModelo(), req.fechaInicio(), req.fechaFin(), req.sedeRecojo());
        if (unidad == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin disponibilidad en la sede " + nombreSede(req.sedeRecojo())
                    + " para las fechas seleccionadas. Prueba con otra sede u otras fechas.");
        }

        Alquiler alquiler = armarAlquiler(cliente, modelo, req.fechaInicio(), req.fechaFin(),
                req.sedeRecojo(), req.sedeDevolucion(), req.horaRecojo(), req.horaDevolucion(),
                Boolean.TRUE.equals(req.seguroTodoRiesgo()),
                Boolean.TRUE.equals(req.conductorAdicional()),
                Boolean.TRUE.equals(req.sillaBebe()));
        alquiler.setVehiculoUnidad(unidad);
        alquiler.setEstado(estadoAlquiler("RESERVADO"));

        return alquilerRepository.save(alquiler);
    }


    @Transactional
    public Alquiler aprobarReserva(Integer idAlquiler) {
        Alquiler alquiler = buscar(idAlquiler);
        if (!"PENDIENTE".equalsIgnoreCase(nombreEstado(alquiler))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden aprobar solicitudes en estado PENDIENTE");
        }
        validarClienteHabilitado(alquiler.getCliente());
        validarDocumentosCliente(alquiler.getCliente());

        VehiculoModelo modelo = alquiler.getVehiculoModelo();
        if (modelo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo de vehículo no encontrado en la solicitud");
        }

        VehiculoUnidad unidadAsignada = buscarUnidadLibreParaFechas(
                modelo.getIdModelo(), alquiler.getFechaInicio(), alquiler.getFechaFinEstimada(), alquiler.getSedeRecojo());

        if (unidadAsignada == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sin disponibilidad en la sede " + nombreSede(alquiler.getSedeRecojo()) + " para " + modelo.getNombreModelo()
                            + " en el rango de fechas seleccionado.");
        }

        alquiler.setVehiculoUnidad(unidadAsignada);
        alquiler.setEstado(estadoAlquiler("RESERVADO"));
        Alquiler aprobado = alquilerRepository.save(alquiler);
        avisarAprobacion(aprobado);

        int stockRestante = contarStockDisponibleFechas(modelo.getIdModelo(), alquiler.getFechaInicio(), alquiler.getFechaFinEstimada(), alquiler.getSedeRecojo());

        if (stockRestante <= 0) {
            log.info("Stock agotado para modelo {} entre {} y {}. Cancelando solicitudes pendientes solapadas...",
                    modelo.getNombreModelo(), alquiler.getFechaInicio(), alquiler.getFechaFinEstimada());

            List<Alquiler> solapadas = alquilerRepository.findSolicitudesPendientesSolapadas(
                    modelo.getIdModelo(), alquiler.getFechaInicio(), alquiler.getFechaFinEstimada(), alquiler.getIdAlquiler());

            EstadoAlquiler stCanceladoSinStock = estadoAlquiler("CANCELADO_SIN_STOCK");

            for (Alquiler pend : solapadas) {
                pend.setEstado(stCanceladoSinStock);
                pend.setMotivoRechazo("SIN_DISPONIBILIDAD_FLOTA");
                pend.setObservacionRechazo("Las unidades disponibles de " + modelo.getNombreModelo() + " se agotaron por alta demanda para las fechas solicitadas.");
                pend.setFechaFinReal(LocalDate.now());
                alquilerRepository.save(pend);

                // El correo sale en segundo plano: la aprobación no espera al servidor de correo
                final String correo = pend.getCliente().getEmail();
                final String nombre = pend.getCliente().getNombres();
                final String vehiculoTxt = modelo.getMarca().getNombreMarca() + " " + modelo.getNombreModelo();
                final String observacion = pend.getObservacionRechazo();
                enSegundoPlano("rechazo por falta de stock a " + correo,
                        () -> emailService.enviarRechazoReserva(correo, nombre, vehiculoTxt, "Agotado por Alta Demanda", observacion));
            }
        }

        return aprobado;
    }

    @Transactional
    public Alquiler rechazarReserva(Integer idAlquiler, RechazoReservaRequest req) {
        Alquiler alquiler = buscar(idAlquiler);
        String estado = nombreEstado(alquiler);
        if (!"PENDIENTE".equalsIgnoreCase(estado) && !"RESERVADO".equalsIgnoreCase(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se pueden rechazar solicitudes pendientes o reservadas");
        }
        if ("RESERVADO".equalsIgnoreCase(estado) && !pagoRepository.findByAlquiler_IdAlquiler(idAlquiler).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya tiene pagos registrados, no se puede rechazar");
        }
        String motivo = req.motivo() == null ? "" : req.motivo().trim().toUpperCase();
        if (!TarifarioPaulCars.MOTIVOS_RECHAZO.contains(motivo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Causal de rechazo no válida: " + req.motivo());
        }

        if ("HISTORIAL_NEGATIVO".equals(motivo)) {
            // Solo tiene sentido si el cliente ya tuvo alquileres: una cuenta nueva no puede tener obligaciones pendientes
            boolean tuvoAlquileres = alquiler.getCliente() != null && alquilerRepository.findByCliente_IdCliente(alquiler.getCliente().getIdCliente()).stream()
                    .filter(o -> !o.getIdAlquiler().equals(alquiler.getIdAlquiler()))
                    .anyMatch(o -> o.getEstado() != null && java.util.Set.of("RESERVADO", "PAGADO", "CONFIRMADO", "ACTIVO", "FINALIZADO",
                            "CERRADO_CON_DEUDA", "CANCELADO").contains(o.getEstado().getNombreEstado().toUpperCase()));
            if (!tuvoAlquileres) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Este cliente no tiene alquileres anteriores, por lo que no puede tener obligaciones pendientes. Elija otra causal.");
            }
        }
        alquiler.setMotivoRechazo(motivo);
        alquiler.setObservacionRechazo(req.observaciones() != null ? req.observaciones().trim() : null);
        alquiler.setFechaFinReal(LocalDate.now());

        if (motivo.contains("DNI") || motivo.contains("LICENCIA") || motivo.contains("DOCUMENTACION")) {
            alquiler.setEstado(estadoAlquiler("RECHAZADO_DOCUMENTACION"));
        } else {
            alquiler.setEstado(estadoAlquiler("RECHAZADO"));
        }

        alquiler.setVehiculoUnidad(null);

        Alquiler guardado = alquilerRepository.save(alquiler);

        // El correo sale en segundo plano: el rechazo se ve al instante
        Cliente c = alquiler.getCliente();
        final String correo = c.getEmail();
        final String nombre = c.getNombres();
        final String vehiculoTxt = nombreVehiculo(alquiler.getVehiculoModelo());
        final String motivoTxt = motivoRechazoParaCliente(motivo);
        final String observacion = alquiler.getObservacionRechazo();
        enSegundoPlano("rechazo a " + correo, () -> emailService.enviarRechazoReserva(correo, nombre, vehiculoTxt, motivoTxt, observacion));

        return guardado;
    }


    private static final java.util.Set<String> NIVELES_COMBUSTIBLE = java.util.Set.of("LLENO", "3/4", "1/2", "1/4");

    private String nivelCombustible(String valor) {
        String n = valor == null ? "" : valor.trim().toUpperCase();
        if (!NIVELES_COMBUSTIBLE.contains(n)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seleccione el nivel de combustible del vehículo (Lleno, 3/4, 1/2 o 1/4).");
        }
        return n;
    }

    private LocalDateTime horaPactadaDeRecojo(Alquiler alquiler) {
        java.time.LocalTime hora = java.time.LocalTime.of(10, 0);
        try {
            if (alquiler.getHoraRecojo() != null && !alquiler.getHoraRecojo().isBlank()) {
                hora = java.time.LocalTime.parse(alquiler.getHoraRecojo().trim());
            }
        } catch (java.time.format.DateTimeParseException ignorada) {
            // se usa la hora por defecto
        }
        return alquiler.getFechaInicio().atTime(hora);
    }

    /**
     * Ventana de entrega: desde N minutos antes de la hora pactada hasta que termine la espera al cliente.
     * Fuera de ella no se puede entregar.
     */
    private void exigirVentanaDeEntrega(Alquiler alquiler, LocalDateTime ahora) {
        if (alquiler.getFechaInicio() == null) return;
        LocalDateTime pactada = horaPactadaDeRecojo(alquiler);
        LocalDateTime desde = pactada.minusMinutes(politica.getEntregaAnticipadaMinutos());
        LocalDateTime hasta = pactada.plusMinutes(politica.getEsperaClienteMinutos());
        var formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
        if (ahora.isBefore(desde)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Todavía no se puede entregar: la entrega se habilita el " + desde.format(formato) + " ("
                            + politica.getEntregaAnticipadaMinutos() + " minutos antes de la hora pactada).");
        }
        if (ahora.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pasó el tiempo de espera (" + politica.getEsperaClienteMinutos() + " minutos desde la hora pactada): "
                            + "el cliente no se presentó y la reserva se cancela sin devolución.");
        }
    }

    /**
     * Qué se registra como hora de entrega y de devolución:
     *  - Entrega anticipada (hasta N min antes): sale a la hora real y la devolución se adelanta los mismos minutos,
     *    para que el alquiler dure lo contratado.
     *  - A la hora pactada o con retraso (dentro de la espera): se registra la hora real y la devolución no se mueve,
     *    porque el vehículo esperó al cliente.
     */
    private void registrarHorasDeEntrega(Alquiler alquiler, LocalDateTime ahora) {
        LocalDateTime pactada = horaPactadaDeRecojo(alquiler);
        // La hora real es siempre la del momento en que se entregan las llaves
        alquiler.setFechaEntrega(ahora);
        if (!ahora.isBefore(pactada)) {
            return; // llegó a la hora o con retraso: la devolución no se mueve
        }
        long adelanto = java.time.Duration.between(ahora, pactada).toMinutes();
        if (adelanto > 0 && alquiler.getFechaFinEstimada() != null
                && alquiler.getHoraDevolucion() != null && !alquiler.getHoraDevolucion().isBlank()) {
            try {
                LocalDateTime devolucion = alquiler.getFechaFinEstimada()
                        .atTime(java.time.LocalTime.parse(alquiler.getHoraDevolucion().trim()))
                        .minusMinutes(adelanto);
                alquiler.setFechaFinEstimada(devolucion.toLocalDate());
                alquiler.setHoraDevolucion(devolucion.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            } catch (java.time.format.DateTimeParseException ignorada) {
                // hora de devolución con formato inesperado: no se ajusta
            }
        }
    }

    @Transactional
    public Alquiler entregarLlaves(Integer idAlquiler, EntregaRequest req) {
        Alquiler alquiler = buscar(idAlquiler);
        String estado = nombreEstado(alquiler);
        if (!"PAGADO".equalsIgnoreCase(estado) && !"RESERVADO".equalsIgnoreCase(estado) && !"CONFIRMADO".equalsIgnoreCase(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden entregar llaves a reservas en estado PAGADO, CONFIRMADO o RESERVADO");
        }

        if (alquiler.getVehiculoUnidad() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se ha asignado una unidad física a esta reserva");
        }

        // La hora es la del momento exacto en que se confirma la entrega (con segundos), no la de cuando se abrió la pantalla
        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        exigirVentanaDeEntrega(alquiler, ahora);

        garantizarUnidadEnSede(alquiler);
        VehiculoUnidad unidad = alquiler.getVehiculoUnidad();
        registrarActaEntrega(alquiler, unidad, req);
        registrarHorasDeEntrega(alquiler, ahora);

        if (alquiler.getGarantiaRetenida() == null && alquiler.getVehiculoModelo() != null) {
            alquiler.setGarantiaRetenida(TarifarioPaulCars.garantiaHoldPorCategoria(alquiler.getVehiculoModelo()));
        }

        alquiler.setEstado(estadoAlquiler("ACTIVO"));
        unidad.setEstado(estadoVehiculo("EN_RUTA"));
        vehiculoUnidadRepository.save(unidad);

        Alquiler guardado = alquilerRepository.save(alquiler);
        // Estado inicial del vehículo: los daños que ya tenía no se le cobran al cliente
        danoService.registrar(guardado, com.example.alquiler_de_vehiculos.model.DanoVehiculo.ENTREGA, req.danos());
        eventPublisher.publishEvent(new ComprobanteEvents.EntregaRegistrada(guardado.getIdAlquiler()));
        return guardado;
    }

    /**
     * Antes de entregar, la placa asignada debe estar de verdad en la sede de recojo y lista (no en ruta, mantenimiento ni fuera de
     * servicio). Si no lo está, se cambia sola por otra libre de la misma sede; si no hay ninguna, se bloquea la entrega y se indica
     * cancelar con «Vehículo no disponible» (devolución total).
     */
    private void garantizarUnidadEnSede(Alquiler alquiler) {
        VehiculoUnidad u = alquiler.getVehiculoUnidad();
        String sede = alquiler.getSedeRecojo();
        String estadoU = u.getEstado() != null ? u.getEstado().getNombreEstado().toUpperCase() : "";
        boolean listaEnSede = mismaSede(u, sede) && (estadoU.equals("DISPONIBLE") || estadoU.isEmpty());
        if (listaEnSede) return;

        List<Integer> ocupadas = alquilerRepository.findIdsUnidadesOcupadasEnFechas(alquiler.getFechaInicio(), alquiler.getFechaFinEstimada());
        VehiculoUnidad otra = vehiculoUnidadRepository.findByVehiculoModelo_IdModelo(alquiler.getVehiculoModelo().getIdModelo()).stream()
                .filter(x -> !x.getIdUnidad().equals(u.getIdUnidad()))
                .filter(x -> mismaSede(x, sede))
                .filter(x -> x.getEstado() != null && "DISPONIBLE".equalsIgnoreCase(x.getEstado().getNombreEstado()))
                .filter(x -> !ocupadas.contains(x.getIdUnidad()))
                .findFirst().orElse(null);
        if (otra != null) {
            log.info("ALQ-{} cambia de placa {} a {} (la anterior no estaba lista en {})", alquiler.getIdAlquiler(), u.getPlaca(), otra.getPlaca(), sede);
            alquiler.setVehiculoUnidad(otra);
            return;
        }
        String motivo = !mismaSede(u, sede)
                ? "la placa " + u.getPlaca() + " está en la sede " + nombreSede(u.getSede())
                : "la placa " + u.getPlaca() + " no está lista (" + estadoU + ")";
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No hay un vehículo listo en la sede " + nombreSede(sede) + " para entregar: " + motivo
                        + ". Cancela la reserva con el motivo «Vehículo no disponible» (devolución del 100 %) u ofrece otro vehículo al cliente.");
    }

    // Valida y guarda el acta de entrega: documentos, kilometraje, nivel de combustible y conformidad del vehículo
    private void registrarActaEntrega(Alquiler alquiler, VehiculoUnidad unidad, EntregaRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Completa el acta de entrega (documentos, kilometraje, combustible y conformidad) antes de entregar las llaves");
        }
        if (!Boolean.TRUE.equals(req.documentosVerificados())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debes confirmar que verificaste el DNI y la licencia de conducir del cliente");
        }
        // Kilometraje de salida: lectura del odómetro; no puede ser menor al último registrado ni absurdamente mayor
        Integer km = req.kilometraje();
        if (km == null || km < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingrese el kilometraje de salida (lectura del odómetro del vehículo).");
        }
        int kmUnidad = unidad.getKilometraje() != null ? unidad.getKilometraje() : 0;
        if (km < kmUnidad) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El kilometraje no puede ser menor al último registrado de la unidad (" + String.format(java.util.Locale.US, "%,d", kmUnidad) + " km).");
        }
        if (km - kmUnidad > politica.getKmMaximoSaltoEntrega()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El kilometraje ingresado supera en más de " + politica.getKmMaximoSaltoEntrega() + " km al último registrado ("
                            + String.format(java.util.Locale.US, "%,d", kmUnidad) + " km). Verifique la lectura del odómetro.");
        }
        if (unidad.kmRestantesMantenimiento() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La unidad alcanzó su kilometraje de mantenimiento (" + String.format(java.util.Locale.US, "%,d", unidad.kmProximoMantenimiento())
                            + " km) y no puede entregarse hasta que pase por mantenimiento.");
        }
        String combustible = nivelCombustible(req.combustible());

        String checklist = req.checklist() == null ? "" : req.checklist().stream()
                .filter(i -> i != null && !i.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining("|"));
        alquiler.setKmEntrega(km);
        alquiler.setCombustibleEntrega(combustible);
        alquiler.setChecklistEntrega(checklist);
        alquiler.setObservacionesEntrega(null);
        alquiler.setEntregadoPor(nombreUsuarioActual());

        unidad.actualizarKilometraje(km);
    }

    // Los correos nunca retrasan la operación: se envían aparte y, si fallan, solo se deja un aviso en el registro
    private void enSegundoPlano(String descripcion, Runnable envio) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                envio.run();
            } catch (RuntimeException e) {
                log.warn("No se pudo enviar el correo ({}): {}", descripcion, e.getMessage());
            }
        });
    }

    /** Avisa al cliente que su solicitud fue aprobada y hasta cuándo tiene para pagar. */
    private void avisarAprobacion(Alquiler a) {
        try {
            if (a.getCliente() == null || a.getCliente().getEmail() == null || a.getFechaInicio() == null) return;
            LocalTime hora = LocalTime.of(10, 0);
            try {
                if (a.getHoraRecojo() != null && !a.getHoraRecojo().isBlank()) hora = LocalTime.parse(a.getHoraRecojo().trim());
            } catch (DateTimeParseException ignorada) {
                // se usa la hora por defecto
            }
            LocalDateTime recojo = a.getFechaInicio().atTime(hora);
            LocalDateTime vence = politica.vencimientoPago(LocalDateTime.now(), recojo);
            var formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String fecha = vence.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm"));
            String total = a.getPrecioTotal() != null ? String.format(java.util.Locale.US, "S/ %,.2f", a.getPrecioTotal()) : "-";
            String cuerpo = emailService.parrafo("Hola <strong>" + emailService.escapar(a.getCliente().getNombres()) + "</strong>, aprobamos tu solicitud de reserva. "
                            + "Para asegurar el vehículo, realiza el pago antes de la fecha límite.")
                    + emailService.tabla(
                            new String[] { "Reserva", String.format("ALQ-%06d", a.getIdAlquiler()) },
                            new String[] { "Vehículo", nombreVehiculo(a.getVehiculoModelo()) },
                            new String[] { "Recojo", a.getFechaInicio().format(formato) + " - " + hora + " hrs" },
                            new String[] { "Devolución", a.getFechaFinEstimada() != null ? a.getFechaFinEstimada().format(formato) : "-" },
                            new String[] { "Total a pagar", total },
                            new String[] { "Paga hasta", fecha, "destacada" })
                    + emailService.aviso("Si no recibimos tu pago antes de la fecha límite, la reserva se <strong>cancela automáticamente</strong> y el vehículo se libera para otros clientes.")
                    + emailService.parrafo("<span style=\"color:#64748b;font-size:13px;\">Puedes pagar en línea con Yape, Plin o tarjeta desde <em>Mis reservas</em>, o en nuestro mostrador.</span>");
            String html = emailService.plantilla("RESERVA APROBADA", com.example.alquiler_de_vehiculos.service.EmailService.VERDE,
                    "Tu reserva fue aprobada", cuerpo, "Pagar mi reserva", "/cliente/reservas");
            final String correo = a.getCliente().getEmail();
            final String asunto = "Tu reserva fue aprobada: paga hasta el " + fecha + " - Paul Car's";
            enSegundoPlano("aprobación a " + correo, () -> emailService.enviarHtml(correo, asunto, html));
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar al cliente de la aprobación: {}", e.getMessage());
        }
    }

    private String nombreUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return "Counter";
        return usuarioRepository.findByUsernameIgnoreCase(auth.getName())
                .map(u -> u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank()
                        ? u.getNombreCompleto() : u.getUsername())
                .orElse(auth.getName());
    }

    @Transactional
    public LiquidacionResponse checkoutInspeccion(Integer idAlquiler, CheckoutRequest req) {
        Alquiler alquiler = buscar(idAlquiler);
        String estadoActual = nombreEstado(alquiler);
        if (!"ACTIVO".equalsIgnoreCase(estadoActual) && !"PAGADO".equalsIgnoreCase(estadoActual) && !"RESERVADO".equalsIgnoreCase(estadoActual)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede finalizar un alquiler en curso (ACTIVO) o con llaves entregadas");
        }

        // La devolución se registra con la hora exacta en que se confirma la recepción (no se digita)
        LocalDateTime momentoRecepcion = momentoValido(req.momentoRecepcion());
        LocalDate fechaDevolucion = momentoRecepcion.toLocalDate();
        VehiculoModelo modelo = alquiler.getVehiculoModelo();
        VehiculoUnidad unidad = alquiler.getVehiculoUnidad();
        // Daños nuevos encontrados al recibir el vehículo: los moderados y graves lo sacan de servicio hasta repararlo
        boolean hayDanos = req.danos() != null && !req.danos().isEmpty();
        boolean danoImportante = hayDanos && req.danos().stream().anyMatch(d -> d != null && d.gravedad() != null
                && ("MODERADO".equalsIgnoreCase(d.gravedad().trim()) || "GRAVE".equalsIgnoreCase(d.gravedad().trim())));

        BigDecimal garantia = alquiler.getGarantiaRetenida() != null
                ? alquiler.getGarantiaRetenida()
                : (modelo != null ? TarifarioPaulCars.garantiaHoldPorCategoria(modelo) : BigDecimal.valueOf(3500));

        if (unidad != null) {
            Integer km = req != null ? req.kilometraje() : null;
            if (km == null || km < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingrese el kilometraje al recibir (lectura del odómetro del vehículo).");
            }
            int kmBase = alquiler.getKmEntrega() != null ? alquiler.getKmEntrega() : (unidad.getKilometraje() != null ? unidad.getKilometraje() : 0);
            if (km < kmBase) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El kilometraje al recibir no puede ser menor al de la entrega (" + String.format(java.util.Locale.US, "%,d", kmBase) + " km).");
            }
            int dias = Math.max(1, alquiler.getDias() != null ? alquiler.getDias() : 1);
            int tope = dias * politica.getKmMaximoPorDia();
            if (km - kmBase > tope) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El kilometraje ingresado implica " + String.format(java.util.Locale.US, "%,d", km - kmBase) + " km recorridos, más de lo razonable para "
                                + dias + (dias == 1 ? " día" : " días") + " de alquiler (máximo " + String.format(java.util.Locale.US, "%,d", tope) + " km). Verifique la lectura del odómetro.");
            }
            String combustible = nivelCombustible(req.combustible());

            alquiler.setKmDevolucion(km);
            alquiler.setCombustibleDevolucion(combustible);

            // Cargos de la recepción: combustible faltante y retraso
            var cargos = calcularCargos(alquiler, combustible, momentoRecepcion);
            alquiler.setCargoCombustible(cargos.cargoCombustible());
            alquiler.setCargoRetraso(cargos.cargoRetraso());
            alquiler.setMinutosRetraso(cargos.minutosRetraso());

            unidad.actualizarKilometraje(km);
            // Si llegó al kilometraje de mantenimiento (o le falta muy poco) queda fuera de servicio: sus reservas futuras pasan
            // a otra unidad y solo el administrador la habilita cuando el mantenimiento se hizo. Si no, vuelve a estar disponible.
            if (unidad.kmRestantesMantenimiento() <= politica.getKmMargenMantenimiento()) {
                try {
                    cancelacionService.reasignarReservasDeUnidad(unidad,
                            LocalDateTime.now().plusDays(politica.getDiasReasignarPorBaja()));
                } catch (ResponseStatusException sinReemplazo) {
                    // Recibir el vehículo no puede fallar por esto: la reserva queda y el sistema reintenta pasarla a otra placa
                    log.warn("Unidad {} a mantenimiento sin reemplazo inmediato: {}", unidad.getPlaca(), sinReemplazo.getReason());
                }
                unidad.setEstado(estadoVehiculo("MANTENIMIENTO"));
            } else if (danoImportante) {
                try {
                    cancelacionService.reasignarReservasDeUnidad(unidad,
                            LocalDateTime.now().plusDays(politica.getDiasReasignarPorBaja()));
                } catch (ResponseStatusException sinReemplazo) {
                    log.warn("Unidad {} fuera de servicio por daños sin reemplazo inmediato: {}", unidad.getPlaca(), sinReemplazo.getReason());
                }
                unidad.setEstado(estadoVehiculo("NO DISPONIBLE"));
            } else {
                unidad.setEstado(estadoVehiculo("DISPONIBLE"));
            }
            vehiculoUnidadRepository.save(unidad);
        }

        alquiler.setFechaFinReal(fechaDevolucion);
        alquiler.setGarantiaRetenida(TarifarioPaulCars.redondear(garantia));
        if (hayDanos) {
            // Con daños nuevos la garantía queda retenida hasta que el administrador los evalúe
            alquiler.setEstado(estadoAlquiler("FINALIZADO"));
            alquiler.setEstadoDanos(DanoService.PENDIENTE);
            alquiler.setGarantiaLiberada(null);
        } else {
            // Los cargos se descuentan de la garantía; lo que no cubre queda como deuda por cobrar
            BigDecimal totalCargos = TarifarioPaulCars.nz(alquiler.getCargoCombustible())
                    .add(TarifarioPaulCars.nz(alquiler.getCargoRetraso()));
            alquiler.setGarantiaLiberada(TarifarioPaulCars.redondear(garantia.subtract(totalCargos).max(BigDecimal.ZERO)));
            alquiler.setEstado(estadoAlquiler(totalCargos.compareTo(garantia) > 0 ? "CERRADO_CON_DEUDA" : "FINALIZADO"));
        }
        alquiler.setFechaCheckout(momentoRecepcion);
        alquiler.setActaObservaciones(req != null && req.observaciones() != null ? req.observaciones().trim() : null);
        alquilerRepository.save(alquiler);
        if (hayDanos) {
            danoService.registrar(alquiler, com.example.alquiler_de_vehiculos.model.DanoVehiculo.RECEPCION, req.danos());
        }

        return armarActa(alquiler);
    }

    private static int cuartos(String nivel) {
        if (nivel == null) return 4;
        return switch (nivel.trim().toUpperCase()) {
            case "3/4" -> 3;
            case "1/2" -> 2;
            case "1/4" -> 1;
            default -> 4;
        };
    }

    /** Cargos de combustible y retraso si el vehículo se recibe en este momento con ese nivel de combustible. */
    public com.example.alquiler_de_vehiculos.dto.CargosPreviosResponse cargosPrevios(Integer idAlquiler, String combustible, LocalDateTime momento) {
        Alquiler alquiler = buscar(idAlquiler);
        return calcularCargos(alquiler, combustible, momentoValido(momento));
    }

    /**
     * La hora de devolución es la que quedó fija al abrir la pantalla de recepción. Solo se acepta si es razonable
     * (no futura y de hace menos de 12 horas); si no, se usa la hora actual del servidor.
     */
    private static LocalDateTime momentoValido(LocalDateTime pedido) {
        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        if (pedido == null || pedido.isAfter(ahora.plusMinutes(2)) || pedido.isBefore(ahora.minusHours(12))) {
            return ahora;
        }
        return pedido.withNano(0);
    }

    private com.example.alquiler_de_vehiculos.dto.CargosPreviosResponse calcularCargos(Alquiler alquiler, String combustible, LocalDateTime momento) {
        VehiculoModelo modelo = alquiler.getVehiculoModelo();
        BigDecimal garantia = alquiler.getGarantiaRetenida() != null ? alquiler.getGarantiaRetenida()
                : (modelo != null ? TarifarioPaulCars.garantiaHoldPorCategoria(modelo) : BigDecimal.valueOf(3500));

        int faltan = 0;
        BigDecimal cargoCombustible = BigDecimal.ZERO;
        if (combustible != null && !combustible.isBlank() && NIVELES_COMBUSTIBLE.contains(combustible.trim().toUpperCase())) {
            faltan = Math.max(0, cuartos(alquiler.getCombustibleEntrega()) - cuartos(combustible));
            cargoCombustible = politica.getCombustiblePorCuarto().multiply(BigDecimal.valueOf(faltan));
        }

        int minutosRetraso = 0;
        BigDecimal cargoRetraso = BigDecimal.ZERO;
        if (alquiler.getFechaFinEstimada() != null) {
            java.time.LocalTime hora = java.time.LocalTime.of(18, 0);
            try {
                if (alquiler.getHoraDevolucion() != null && !alquiler.getHoraDevolucion().isBlank()) {
                    hora = java.time.LocalTime.parse(alquiler.getHoraDevolucion().trim());
                }
            } catch (java.time.format.DateTimeParseException ignorada) {
                // hora con formato inesperado: se usa la de cierre
            }
            long tarde = java.time.Duration.between(alquiler.getFechaFinEstimada().atTime(hora), momento).toMinutes();
            if (tarde > politica.getRetrasoToleranciaMinutos()) {
                minutosRetraso = (int) tarde;
                BigDecimal horas = BigDecimal.valueOf((tarde + 59) / 60);
                BigDecimal porHora = modelo != null && modelo.getPrecioHora() != null ? modelo.getPrecioHora() : BigDecimal.valueOf(25);
                cargoRetraso = porHora.multiply(horas);
                // Nunca más que un día completo de alquiler
                if (modelo != null && modelo.getPrecioDia() != null && cargoRetraso.compareTo(modelo.getPrecioDia()) > 0) {
                    cargoRetraso = modelo.getPrecioDia();
                }
            }
        }
        return new com.example.alquiler_de_vehiculos.dto.CargosPreviosResponse(
                TarifarioPaulCars.redondear(cargoCombustible), faltan, TarifarioPaulCars.redondear(cargoRetraso), minutosRetraso, garantia);
    }

    public LiquidacionResponse obtenerActa(Integer idAlquiler) {
        Alquiler alquiler = buscar(idAlquiler);
        if (alquiler.getFechaCheckout() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Este alquiler todavía no tiene acta de recepción");
        }
        return armarActa(alquiler);
    }

    @Transactional
    public Alquiler cancelarAlquiler(Integer idAlquiler) {
        Alquiler alquiler = buscar(idAlquiler);
        securityUtils.validarAccesoAlquiler(alquiler);
        if (alquiler.getFechaFinReal() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El alquiler ya fue finalizado o cancelado");
        }
        boolean conPagos = pagoRepository.findByAlquiler_IdAlquiler(idAlquiler).stream()
                .anyMatch(p -> p.getTipoPago() == null || "ALQUILER".equalsIgnoreCase(p.getTipoPago()));
        if (conPagos) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta reserva ya está pagada. Para cancelarla y solicitar la devolución comunícate con Paul Car's.");
        }
        alquiler.setFechaFinReal(LocalDate.now());
        alquiler.setEstado(estadoAlquiler("CANCELADO"));

        // Queda registrado quién canceló y cuándo (el cliente desde su cuenta, o el personal)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esPersonal = auth != null && auth.getAuthorities().stream()
                .anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()) || "ROLE_EMPLEADO".equals(g.getAuthority()));
        boolean esAdmin = auth != null && auth.getAuthorities().stream().anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()));
        alquiler.setFechaCancelacion(LocalDateTime.now());
        if (esPersonal) {
            alquiler.setCanceladoPor(nombreUsuarioActual());
            alquiler.setCanceladoPorRol(esAdmin ? "ADMINISTRADOR" : "ASESOR");
        } else {
            alquiler.setMotivoCancelacion("CLIENTE_SOLICITO");
            alquiler.setCanceladoPor(alquiler.getCliente() != null
                    ? ((alquiler.getCliente().getNombres() != null ? alquiler.getCliente().getNombres() : "") + " "
                    + (alquiler.getCliente().getApellidos() != null ? alquiler.getCliente().getApellidos() : "")).trim()
                    : nombreUsuarioActual());
            alquiler.setCanceladoPorRol("CLIENTE");
        }

        if (alquiler.getVehiculoUnidad() != null) {
            VehiculoUnidad u = alquiler.getVehiculoUnidad();
            u.setEstado(estadoVehiculo("DISPONIBLE"));
            vehiculoUnidadRepository.save(u);
            alquiler.setVehiculoUnidad(null);
        }
        return alquilerRepository.save(alquiler);
    }


    /** Todos los cambios de estado registrados (para ordenar la tabla por llegada/salida de un estado). */
    public List<com.example.alquiler_de_vehiculos.dto.HistorialEstadoDTO> listarHistorialEstados() {
        return historialRepository.findTodosOrdenados().stream()
                .map(h -> new com.example.alquiler_de_vehiculos.dto.HistorialEstadoDTO(
                        h.getAlquiler().getIdAlquiler(), h.getEstadoAnterior(), h.getEstadoNuevo(), h.getFechaHora()))
                .toList();
    }

    public List<com.example.alquiler_de_vehiculos.dto.HistorialEstadoDTO> historialDeAlquiler(Integer idAlquiler) {
        return historialRepository.findByAlquilerOrdenado(idAlquiler).stream()
                .map(h -> new com.example.alquiler_de_vehiculos.dto.HistorialEstadoDTO(
                        idAlquiler, h.getEstadoAnterior(), h.getEstadoNuevo(), h.getFechaHora()))
                .toList();
    }

    public List<Alquiler> listarAlquileres() {
        List<Alquiler> lista = alquilerRepository.findAll();
        ordenarAlquileresInteligente(lista);
        return lista;
    }

    public Alquiler obtenerAlquiler(Integer idAlquiler) {
        Alquiler alquiler = buscar(idAlquiler);
        securityUtils.validarAccesoAlquiler(alquiler);
        return alquiler;
    }

    public List<Alquiler> obtenerAlquileresPorUsername(String username) {
        Usuario usuario = buscarUsuario(username);
        if (usuario == null || usuario.getCliente() == null) {
            return Collections.emptyList();
        }
        return obtenerAlquileresPorCliente(usuario.getCliente().getIdCliente());
    }

    public List<Alquiler> obtenerAlquileresPorCliente(Integer idCliente) {
        securityUtils.validarAccesoCliente(idCliente);
        List<Alquiler> lista = alquilerRepository.findByCliente_IdCliente(idCliente);
        lista.sort((a, b) -> (b.getIdAlquiler() != null ? b.getIdAlquiler() : 0)
                - (a.getIdAlquiler() != null ? a.getIdAlquiler() : 0));
        return lista;
    }

    public List<Alquiler> obtenerAlquileresPorVehiculo(Integer idModelo) {
        return alquilerRepository.findByVehiculoModelo_IdModelo(idModelo);
    }

    public DisponibilidadHorariosDTO consultarDisponibilidad(String sede, LocalDate fecha) {
        boolean esAeropuerto = "AEROPUERTO".equalsIgnoreCase(sede);
        boolean esDomingo = fecha.getDayOfWeek() == DayOfWeek.SUNDAY;

        if (!esAeropuerto && esDomingo) {
            return new DisponibilidadHorariosDTO(sede, fecha, false,
                    "La sede Miraflores no atiende los domingos.",
                    Collections.emptyList(), fecha.plusDays(1), "08:30");
        }

        List<String> turnos = new ArrayList<>();
        if (esAeropuerto) {
            for (int h = 0; h < 24; h++) turnos.add(String.format("%02d:00", h));
        } else {
            int fin = (fecha.getDayOfWeek() == DayOfWeek.SATURDAY) ? 17 : 18;
            for (int h = 8; h <= fin; h++) {
                if (h == 8) turnos.add("08:30");
                else if (h < fin) turnos.add(String.format("%02d:00", h));
            }
        }
        return new DisponibilidadHorariosDTO(sede, fecha, true, "Horarios disponibles", turnos, fecha, turnos.get(0));
    }

    private static String nombreSede(String sede) {
        if (sede == null) return "elegida";
        return sede.toUpperCase().startsWith("AERO") ? "Aeropuerto" : sede.toUpperCase().startsWith("MIRA") ? "Miraflores" : sede;
    }

    private static boolean mismaSede(VehiculoUnidad u, String sede) {
        if (sede == null || sede.isBlank()) return true;
        return u.getSede() != null && nombreSede(u.getSede()).equalsIgnoreCase(nombreSede(sede));
    }

    public int contarStockDisponibleFechas(Integer idModelo, LocalDate inicio, LocalDate fin) {
        return contarStockDisponibleFechas(idModelo, inicio, fin, null);
    }

    /** Unidades libres del modelo en esa sede (sin sede = todas). */
    public int contarStockDisponibleFechas(Integer idModelo, LocalDate inicio, LocalDate fin, String sede) {
        List<VehiculoUnidad> unidadesModelo = vehiculoUnidadRepository.findByVehiculoModelo_IdModelo(idModelo).stream()
                .filter(u -> mismaSede(u, sede))
                .filter(u -> u.getEstado() != null
                        && !"MANTENIMIENTO".equalsIgnoreCase(u.getEstado().getNombreEstado())
                        && !"LIMPIEZA".equalsIgnoreCase(u.getEstado().getNombreEstado())
                        && !"NO DISPONIBLE".equalsIgnoreCase(u.getEstado().getNombreEstado()))
                .toList();

        if (unidadesModelo.isEmpty()) return 0;

        List<Integer> ocupadas = alquilerRepository.findIdsUnidadesOcupadasEnFechas(inicio, fin);
        long libres = unidadesModelo.stream().filter(u -> !ocupadas.contains(u.getIdUnidad())).count();
        return (int) libres;
    }

    private VehiculoUnidad buscarUnidadLibreParaFechas(Integer idModelo, LocalDate inicio, LocalDate fin, String sedeRecojo) {
        List<Integer> ocupadas = alquilerRepository.findIdsUnidadesOcupadasEnFechas(inicio, fin);

        List<VehiculoUnidad> candidatas = vehiculoUnidadRepository.findByVehiculoModelo_IdModelo(idModelo).stream()
                .filter(u -> u.getEstado() != null
                        && !"MANTENIMIENTO".equalsIgnoreCase(u.getEstado().getNombreEstado())
                        && !"LIMPIEZA".equalsIgnoreCase(u.getEstado().getNombreEstado())
                        && !"NO DISPONIBLE".equalsIgnoreCase(u.getEstado().getNombreEstado()))
                .filter(u -> !ocupadas.contains(u.getIdUnidad()))
                .filter(u -> mismaSede(u, sedeRecojo))
                .toList();

        return candidatas.isEmpty() ? null : candidatas.get(0);
    }


    private void ordenarAlquileresInteligente(List<Alquiler> lista) {
        LocalDate hoy = LocalDate.now();
        lista.sort((a, b) -> {
            int scoreA = puntajePrioridadAlquiler(a, hoy);
            int scoreB = puntajePrioridadAlquiler(b, hoy);
            if (scoreA != scoreB) {
                return Integer.compare(scoreA, scoreB);
            }
            if (scoreA <= 3) {
                if (a.getFechaInicio() != null && b.getFechaInicio() != null) {
                    int c = a.getFechaInicio().compareTo(b.getFechaInicio());
                    if (c != 0) return c;
                }
                String horaA = a.getHoraRecojo() != null ? a.getHoraRecojo() : "00:00";
                String horaB = b.getHoraRecojo() != null ? b.getHoraRecojo() : "00:00";
                return horaA.compareTo(horaB);
            }
            if (scoreA == 4) {
                if (a.getFechaReserva() != null && b.getFechaReserva() != null) {
                    int c = a.getFechaReserva().compareTo(b.getFechaReserva());
                    if (c != 0) return c;
                }
                return Integer.compare(
                        a.getIdAlquiler() != null ? a.getIdAlquiler() : 0,
                        b.getIdAlquiler() != null ? b.getIdAlquiler() : 0
                );
            }
            return Integer.compare(
                    b.getIdAlquiler() != null ? b.getIdAlquiler() : 0,
                    a.getIdAlquiler() != null ? a.getIdAlquiler() : 0
            );
        });
    }

    private int puntajePrioridadAlquiler(Alquiler alq, LocalDate hoy) {
        String est = nombreEstado(alq).toUpperCase();
        LocalDate inicio = alq.getFechaInicio();
        LocalDate fin = alq.getFechaFinEstimada();

        if (inicio != null && inicio.isEqual(hoy) && ("RESERVADO".equals(est) || "PAGADO".equals(est) || "CONFIRMADO".equals(est) || "ACTIVO".equals(est))) {
            return 1;
        }
        if (fin != null && fin.isEqual(hoy) && "ACTIVO".equals(est)) {
            return 1;
        }
        if ("ACTIVO".equals(est)) {
            return 2;
        }
        if ("RESERVADO".equals(est) || "PAGADO".equals(est) || "CONFIRMADO".equals(est)) {
            return 3;
        }
        if ("PENDIENTE".equals(est)) {
            return 4;
        }
        return 6;
    }


    private Alquiler buscar(Integer idAlquiler) {
        return alquilerRepository.findById(idAlquiler)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado"));
    }

    private String nombreEstado(Alquiler alquiler) {
        return alquiler.getEstado() != null ? alquiler.getEstado().getNombreEstado() : "";
    }

    private EstadoAlquiler estadoAlquiler(String nombre) {
        return estadoAlquilerRepository.findByNombreEstadoIgnoreCase(nombre)
                .orElseGet(() -> estadoAlquilerRepository.save(new EstadoAlquiler(null, nombre)));
    }

    private EstadoVehiculo estadoVehiculo(String nombre) {
        return estadoVehiculoRepository.findByNombreEstadoIgnoreCase(nombre)
                .orElseGet(() -> estadoVehiculoRepository.save(new EstadoVehiculo(null, nombre)));
    }

    private Usuario buscarUsuario(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .or(() -> usuarioRepository.findByEmailRecuperacionIgnoreCase(username))
                .or(() -> usuarioRepository.findByCliente_EmailIgnoreCase(username))
                .orElse(null);
    }

    private Cliente clienteDeLaSesion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        Usuario usuario = buscarUsuario(auth.getName());
        if (usuario == null || usuario.getRol() == null || !"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombreRol())) {
            return null;
        }
        return usuario.getCliente();
    }

    private void validarClienteHabilitado(Cliente cliente) {
        if (TarifarioPaulCars.CLIENTE_LISTA_NEGRA.equalsIgnoreCase(cliente.getEstadoCliente())
                || "OBSERVADO_LISTA_NEGRA".equalsIgnoreCase(cliente.getEstadoCliente())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Cuenta suspendida en Lista Negra por deuda pendiente. Acércate a counter o salda tu deuda pendiente.");
        }
    }

    private void validarDocumentosCliente(Cliente cliente) {
        validarDocumentosCliente(cliente, true);
    }

    // En counter el asesor registra al cliente en persona (el formulario no pide dirección),
    // por eso la dirección solo se exige en el flujo de reservas en línea.
    private void validarDocumentosCliente(Cliente cliente, boolean exigirDireccion) {
        if (cliente.getNumeroDocumento() == null || cliente.getNumeroDocumento().isBlank()
                || cliente.getNumeroDocumento().startsWith("PEND")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes registrar tu documento de identidad (DNI) en Mi Perfil antes de solicitar una reserva.");
        }
        if (cliente.getLicenciaConducir() == null || cliente.getLicenciaConducir().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes registrar tu licencia de conducir en Mi Perfil antes de solicitar una reserva.");
        }
        if (exigirDireccion && (cliente.getDireccion() == null || cliente.getDireccion().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes registrar tu dirección de residencia en Mi Perfil antes de solicitar una reserva.");
        }
    }

    private void validarFechas(LocalDate fechaInicio, LocalDate fechaFin, boolean noPasado) {
        if (fechaInicio == null || fechaFin == null || !fechaFin.isAfter(fechaInicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de devolución debe ser al menos un día posterior a la fecha de recojo (mínimo 1 día de alquiler)");
        }
        if (noPasado && fechaInicio.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de inicio de la reserva no puede ser anterior a hoy");
        }
    }

    private void validarAnticipacion(LocalDate fechaInicio, String horaRecojo) {
        if (horaRecojo == null || horaRecojo.isBlank()) return;
        try {
            LocalDateTime recojo = LocalDateTime.of(fechaInicio, LocalTime.parse(horaRecojo));
            if (recojo.isBefore(LocalDateTime.now().plusMinutes(60))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Las reservas requieren un mínimo de 1 hora (60 minutos) de anticipación");
            }
        } catch (DateTimeParseException ignored) {}
    }

    private void validarSedeDomingo(String sedeRecojo, LocalDate fechaInicio, String sedeDevolucion, LocalDate fechaFin) {
        if ("MIRAFLORES".equalsIgnoreCase(sedeRecojo) && fechaInicio.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sede Miraflores se encuentra cerrada los domingos");
        }
        if ("MIRAFLORES".equalsIgnoreCase(sedeDevolucion) && fechaFin.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sede Miraflores no recibe devoluciones los domingos");
        }
    }

    /** Las horas de recojo y devolución se eligen en punto o y media (10:00, 10:30), como en el formulario. */
    private static void exigirHoraEnPuntoOMedia(String hora, String cual) {
        if (hora == null || hora.isBlank()) return;
        if (!hora.trim().matches("([01]\\d|2[0-3]):(00|30)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La hora de " + cual + " debe ser en punto o y media (por ejemplo 10:00 o 10:30).");
        }
    }

    private Alquiler armarAlquiler(Cliente cliente, VehiculoModelo modelo, LocalDate fechaInicio, LocalDate fechaFin,
                                   String sedeRecojo, String sedeDevolucion, String horaRecojo, String horaDevolucion,
                                   boolean seguro, boolean conductor, boolean silla) {
        exigirHoraEnPuntoOMedia(horaRecojo, "recojo");
        exigirHoraEnPuntoOMedia(horaDevolucion, "devolución");
        int dias = (int) Math.max(1, ChronoUnit.DAYS.between(fechaInicio, fechaFin));
        BigDecimal precioDia = TarifarioPaulCars.nz(modelo.getPrecioDia());
        BigDecimal base = precioDia.multiply(BigDecimal.valueOf(dias));
        BigDecimal extras = TarifarioPaulCars.extrasPorDia(seguro, conductor, silla).multiply(BigDecimal.valueOf(dias));
        BigDecimal total = base.add(extras);

        Alquiler alquiler = new Alquiler();
        alquiler.setCliente(cliente);
        alquiler.setVehiculoModelo(modelo);
        alquiler.setFechaReserva(LocalDate.now());
        alquiler.setFechaInicio(fechaInicio);
        alquiler.setFechaFinEstimada(fechaFin);
        String sedeR = sedeRecojo != null && !sedeRecojo.isBlank()
                ? sedeRecojo.toUpperCase()
                : (modelo.getSede() != null ? modelo.getSede() : "MIRAFLORES");
        alquiler.setSedeRecojo(sedeR);
        alquiler.setSedeDevolucion(sedeDevolucion != null && !sedeDevolucion.isBlank() ? sedeDevolucion.toUpperCase() : sedeR);
        alquiler.setHoraRecojo(horaRecojo != null && !horaRecojo.isBlank() ? horaRecojo : "09:00");
        alquiler.setHoraDevolucion(horaDevolucion != null && !horaDevolucion.isBlank() ? horaDevolucion : "18:00");
        alquiler.setDias(dias);
        alquiler.setPrecioBase(TarifarioPaulCars.redondear(base).doubleValue());
        alquiler.setMontoExtras(TarifarioPaulCars.redondear(extras).doubleValue());
        alquiler.setPrecioTotal(TarifarioPaulCars.redondear(total).doubleValue());
        alquiler.setServiciosExtras(TarifarioPaulCars.textoExtras(seguro, conductor, silla));
        return alquiler;
    }

    private LiquidacionResponse armarActa(Alquiler alq) {
        VehiculoModelo mod = alq.getVehiculoModelo();
        VehiculoUnidad uni = alq.getVehiculoUnidad();

        String nomVeh = mod != null ? (mod.getMarca().getNombreMarca() + " " + mod.getNombreModelo()) : "Vehículo";
        String placa = uni != null ? uni.getPlaca() : (alq.getPlaca() != null ? alq.getPlaca() : "SIN ASIGNAR");
        String cat = mod != null ? TarifarioPaulCars.categoria(mod) : "SEDAN";

        return new LiquidacionResponse(
                alq.getIdAlquiler(),
                alq.getCliente().getNombres() + " " + alq.getCliente().getApellidos(),
                alq.getCliente().getNumeroDocumento(),
                nomVeh,
                placa,
                cat,
                alq.getGarantiaRetenida(),
                alq.getGarantiaLiberada(),
                nombreEstado(alq),
                uni != null && uni.getEstado() != null ? uni.getEstado().getNombreEstado() : "DISPONIBLE",
                alq.getCliente().getEstadoCliente(),
                alq.getActaObservaciones(),
                alq.getFechaCheckout(),
                uni != null && uni.getEstado() != null && "MANTENIMIENTO".equalsIgnoreCase(uni.getEstado().getNombreEstado()) && alq.getKmDevolucion() != null
                        ? "La unidad " + uni.getPlaca() + " está a " + String.format(java.util.Locale.US, "%,d", Math.max(0, uni.kmRestantesMantenimiento()))
                                + " km de su mantenimiento y pasó a Mantenimiento (corresponde cada " + String.format(java.util.Locale.US, "%,d", uni.intervaloMantenimiento()) + " km)."
                        : null,
                alq.getCargoCombustible(), alq.getCargoRetraso(), alq.getMinutosRetraso(),
                DanoService.totalCargos(alq),
                DanoService.deudaBruta(alq),
                alq.getEstadoDanos(),
                TarifarioPaulCars.tieneSeguro(alq.getServiciosExtras()),
                alq.getCostoDanos(), alq.getCoberturaSeguroPct(), alq.getMontoCubiertoSeguro(), alq.getCargoDanos(),
                danoService.deudaPagada(alq.getIdAlquiler()),
                danoService.deudaPendiente(alq),
                danoService.listar(alq.getIdAlquiler(), com.example.alquiler_de_vehiculos.model.DanoVehiculo.ENTREGA),
                danoService.listar(alq.getIdAlquiler(), com.example.alquiler_de_vehiculos.model.DanoVehiculo.RECEPCION)
        );
    }

    private String nombreVehiculo(VehiculoModelo m) {
        return m != null && m.getMarca() != null
                ? m.getMarca().getNombreMarca() + " " + m.getNombreModelo()
                : "Vehículo";
    }

    /** Causal de rechazo escrita para el cliente: sin datos internos de la empresa. */
    public static String motivoRechazoParaCliente(String motivo) {
        if (motivo == null) return "Solicitud no aprobada";
        // Mismos títulos que ve el asesor al desestimar la solicitud
        return switch (motivo) {
            case "DNI_NO_COINCIDE" -> "Inconsistencia en Documento de Identidad";
            case "LICENCIA_VENCIDA_INVALIDA" -> "Licencia de Conducir No Vigente";
            case "HISTORIAL_NEGATIVO" -> "Obligaciones pendientes de un alquiler anterior";
            case "SIN_DISPONIBILIDAD_FLOTA", "SIN_STOCK_ALTA_DEMANDA" -> "Vehículo no disponible";
            case "INCUMPLIMIENTO_POLITICA_EDAD" -> "Criterio de Edad o Experiencia Requerida";
            default -> motivo.replace("_", " ");
        };
    }
}
