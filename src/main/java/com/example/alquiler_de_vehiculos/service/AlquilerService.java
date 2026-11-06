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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.AlquilerCounterRequest;
import com.example.alquiler_de_vehiculos.dto.CheckoutRequest;
import com.example.alquiler_de_vehiculos.dto.DisponibilidadHorariosDTO;
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
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    public AlquilerService(AlquilerRepository alquilerRepository,
                           ClienteRepository clienteRepository,
                           VehiculoModeloRepository vehiculoModeloRepository,
                           VehiculoUnidadRepository vehiculoUnidadRepository,
                           EstadoVehiculoRepository estadoVehiculoRepository,
                           EstadoAlquilerRepository estadoAlquilerRepository,
                           UsuarioRepository usuarioRepository,
                           PagoRepository pagoRepository,
                           EmailService emailService,
                           SecurityUtils securityUtils) {
        this.alquilerRepository = alquilerRepository;
        this.clienteRepository = clienteRepository;
        this.vehiculoModeloRepository = vehiculoModeloRepository;
        this.vehiculoUnidadRepository = vehiculoUnidadRepository;
        this.estadoVehiculoRepository = estadoVehiculoRepository;
        this.estadoAlquilerRepository = estadoAlquilerRepository;
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.emailService = emailService;
        this.securityUtils = securityUtils;
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

        int stockDisponible = contarStockDisponibleFechas(idModelo, fechaInicio, fechaFin);
        if (stockDisponible <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin unidades disponibles de este modelo para las fechas seleccionadas");
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
        validarDocumentosCliente(cliente);

        VehiculoModelo modelo = vehiculoModeloRepository.findById(req.idVehiculo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo de vehículo no encontrado"));

        VehiculoUnidad unidad = buscarUnidadLibreParaFechas(modelo.getIdModelo(), req.fechaInicio(), req.fechaFin(), req.sedeRecojo());
        if (unidad == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay unidades físicas disponibles de este modelo para las fechas seleccionadas");
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
                    "No hay unidades físicas disponibles de este modelo (" + modelo.getNombreModelo() + ") para el rango de fechas seleccionado.");
        }

        alquiler.setVehiculoUnidad(unidadAsignada);
        alquiler.setEstado(estadoAlquiler("RESERVADO"));
        Alquiler aprobado = alquilerRepository.save(alquiler);

        int stockRestante = contarStockDisponibleFechas(modelo.getIdModelo(), alquiler.getFechaInicio(), alquiler.getFechaFinEstimada());

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

                try {
                    emailService.enviarRechazoReserva(
                            pend.getCliente().getEmail(),
                            pend.getCliente().getNombres(),
                            modelo.getMarca().getNombreMarca() + " " + modelo.getNombreModelo(),
                            "Agotado por Alta Demanda",
                            pend.getObservacionRechazo()
                    );
                } catch (Exception ex) {
                    log.warn("No se pudo notificar por correo al cliente {}: {}", pend.getCliente().getEmail(), ex.getMessage());
                }
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

        Cliente c = alquiler.getCliente();
        emailService.enviarRechazoReserva(
                c.getEmail(),
                c.getNombres(),
                nombreVehiculo(alquiler.getVehiculoModelo()),
                motivoLegible(motivo),
                alquiler.getObservacionRechazo());

        return guardado;
    }


    @Transactional
    public Alquiler entregarLlaves(Integer idAlquiler) {
        Alquiler alquiler = buscar(idAlquiler);
        String estado = nombreEstado(alquiler);
        if (!"PAGADO".equalsIgnoreCase(estado) && !"RESERVADO".equalsIgnoreCase(estado) && !"CONFIRMADO".equalsIgnoreCase(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden entregar llaves a reservas en estado PAGADO, CONFIRMADO o RESERVADO");
        }

        if (alquiler.getVehiculoUnidad() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se ha asignado una unidad física a esta reserva");
        }

        LocalDate hoy = LocalDate.now();
        if (alquiler.getFechaInicio() != null && alquiler.getFechaInicio().isAfter(hoy)) {
            String horaMsg = (alquiler.getHoraRecojo() != null && !alquiler.getHoraRecojo().isBlank())
                    ? " a las " + alquiler.getHoraRecojo() + " hrs"
                    : "";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No es posible entregar llaves antes de la fecha pactada. Fecha programada: "
                            + alquiler.getFechaInicio() + horaMsg + ".");
        }

        if (alquiler.getGarantiaRetenida() == null && alquiler.getVehiculoModelo() != null) {
            alquiler.setGarantiaRetenida(TarifarioPaulCars.garantiaHoldPorCategoria(alquiler.getVehiculoModelo()));
        }

        alquiler.setEstado(estadoAlquiler("ACTIVO"));
        VehiculoUnidad unidad = alquiler.getVehiculoUnidad();
        unidad.setEstado(estadoVehiculo("EN_RUTA"));
        vehiculoUnidadRepository.save(unidad);

        return alquilerRepository.save(alquiler);
    }

    @Transactional
    public LiquidacionResponse checkoutInspeccion(Integer idAlquiler, CheckoutRequest req) {
        Alquiler alquiler = buscar(idAlquiler);
        String estadoActual = nombreEstado(alquiler);
        if (!"ACTIVO".equalsIgnoreCase(estadoActual) && !"PAGADO".equalsIgnoreCase(estadoActual) && !"RESERVADO".equalsIgnoreCase(estadoActual)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede finalizar un alquiler en curso (ACTIVO) o con llaves entregadas");
        }

        LocalDate fechaDevolucion = (req != null && req.fechaDevolucion() != null) ? req.fechaDevolucion() : LocalDate.now();
        VehiculoModelo modelo = alquiler.getVehiculoModelo();
        VehiculoUnidad unidad = alquiler.getVehiculoUnidad();

        BigDecimal garantia = alquiler.getGarantiaRetenida() != null
                ? alquiler.getGarantiaRetenida()
                : (modelo != null ? TarifarioPaulCars.garantiaHoldPorCategoria(modelo) : BigDecimal.valueOf(3500));

        if (unidad != null) {
            unidad.setEstado(estadoVehiculo("DISPONIBLE"));
            vehiculoUnidadRepository.save(unidad);
        }

        alquiler.setEstado(estadoAlquiler("FINALIZADO"));
        alquiler.setFechaFinReal(fechaDevolucion);
        alquiler.setGarantiaRetenida(TarifarioPaulCars.redondear(garantia));
        alquiler.setGarantiaLiberada(TarifarioPaulCars.redondear(garantia));
        alquiler.setFechaCheckout(LocalDateTime.now());
        alquiler.setActaObservaciones(req != null && req.observaciones() != null ? req.observaciones().trim() : null);
        alquilerRepository.save(alquiler);

        return armarActa(alquiler);
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
        alquiler.setFechaFinReal(LocalDate.now());
        alquiler.setEstado(estadoAlquiler("CANCELADO"));

        if (alquiler.getVehiculoUnidad() != null) {
            VehiculoUnidad u = alquiler.getVehiculoUnidad();
            u.setEstado(estadoVehiculo("DISPONIBLE"));
            vehiculoUnidadRepository.save(u);
            alquiler.setVehiculoUnidad(null);
        }
        return alquilerRepository.save(alquiler);
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

    public int contarStockDisponibleFechas(Integer idModelo, LocalDate inicio, LocalDate fin) {
        List<VehiculoUnidad> unidadesModelo = vehiculoUnidadRepository.findByVehiculoModelo_IdModelo(idModelo).stream()
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
                .sorted((u1, u2) -> {
                    boolean m1 = u1.getSede() != null && u1.getSede().equalsIgnoreCase(sedeRecojo);
                    boolean m2 = u2.getSede() != null && u2.getSede().equalsIgnoreCase(sedeRecojo);
                    if (m1 && !m2) return -1;
                    if (!m1 && m2) return 1;
                    return 0;
                })
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
        if (cliente.getNumeroDocumento() == null || cliente.getNumeroDocumento().isBlank()
                || cliente.getNumeroDocumento().startsWith("PEND")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes registrar tu documento de identidad (DNI) en Mi Perfil antes de solicitar una reserva.");
        }
        if (cliente.getLicenciaConducir() == null || cliente.getLicenciaConducir().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes registrar tu licencia de conducir en Mi Perfil antes de solicitar una reserva.");
        }
        if (cliente.getDireccion() == null || cliente.getDireccion().isBlank()) {
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

    private Alquiler armarAlquiler(Cliente cliente, VehiculoModelo modelo, LocalDate fechaInicio, LocalDate fechaFin,
                                   String sedeRecojo, String sedeDevolucion, String horaRecojo, String horaDevolucion,
                                   boolean seguro, boolean conductor, boolean silla) {
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
                alq.getFechaCheckout()
        );
    }

    private String nombreVehiculo(VehiculoModelo m) {
        return m != null && m.getMarca() != null
                ? m.getMarca().getNombreMarca() + " " + m.getNombreModelo()
                : "Vehículo";
    }

    private String motivoLegible(String motivo) {
        return switch (motivo) {
            case "DNI_NO_COINCIDE" -> "Documento de Identidad no coincide";
            case "LICENCIA_VENCIDA_INVALIDA" -> "Licencia de conducir no válida o vencida";
            case "HISTORIAL_NEGATIVO" -> "Observación crediticia o historial de flota";
            case "SIN_DISPONIBILIDAD_FLOTA", "SIN_STOCK_ALTA_DEMANDA" -> "Agotado por alta demanda";
            case "INCUMPLIMIENTO_POLITICA_EDAD" -> "Incumplimiento de política de edad (mínimo 21 años)";
            default -> motivo.replace("_", " ");
        };
    }
}
