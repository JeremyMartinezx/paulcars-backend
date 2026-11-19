package com.example.alquiler_de_vehiculos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.config.PoliticaCancelacionProperties;
import com.example.alquiler_de_vehiculos.dto.DevolucionDTO;
import com.example.alquiler_de_vehiculos.dto.DevolucionDTO.NotaCreditoRef;
import com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.CancelarReservaRequest;
import com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.PreviaCancelacion;
import com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.ResultadoCancelacion;
import com.example.alquiler_de_vehiculos.dto.CancelacionDTOs.UnidadDisponible;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoAlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoUnidadRepository;

/**
 * Cancelación de reservas ya aprobadas, con la política de reembolso de la empresa, y cambio de vehículo.
 * <ul>
 *   <li>Reserva sin pago: cancelar libera el vehículo y avisa al cliente.</li>
 *   <li>Reserva pagada: solo el administrador cancela; el dinero se devuelve por el mismo método con el que se pagó
 *       y cada devolución genera su nota de crédito electrónica.</li>
 *   <li>Si el problema es el vehículo, se puede cambiar por otra unidad del mismo modelo sin cancelar.</li>
 * </ul>
 */
@Service
public class CancelacionService {

    private static final Logger log = LoggerFactory.getLogger(CancelacionService.class);

    public static final Set<String> MOTIVOS = Set.of(
            "CLIENTE_SOLICITO", "NO_PAGO_EN_PLAZO", "VEHICULO_NO_DISPONIBLE", "NO_SE_PRESENTO");
    /** Motivos que son responsabilidad de la empresa: la devolución siempre es total. */
    private static final Set<String> CULPA_EMPRESA = Set.of("VEHICULO_NO_DISPONIBLE");
    private static final Set<String> ESTADOS_CANCELABLES = Set.of("RESERVADO", "PAGADO", "CONFIRMADO", "APROBADO");
    private static final Set<String> ESTADOS_QUE_LIBERAN = Set.of("CANCELADO", "RECHAZADO", "RECHAZADO_DOCUMENTACION",
            "CANCELADO_SIN_STOCK", "FINALIZADO");

    private final AlquilerRepository alquilerRepository;
    private final PagoRepository pagoRepository;
    private final VehiculoUnidadRepository unidadRepository;
    private final EstadoAlquilerRepository estadoAlquilerRepository;
    private final UsuarioRepository usuarioRepository;
    private final BoletaService boletaService;
    private final EmailService emailService;
    private final PoliticaCancelacionProperties politica;
    private final TransactionTemplate tx;

    public CancelacionService(AlquilerRepository alquilerRepository, PagoRepository pagoRepository,
                              VehiculoUnidadRepository unidadRepository, EstadoAlquilerRepository estadoAlquilerRepository,
                              UsuarioRepository usuarioRepository, BoletaService boletaService, EmailService emailService,
                              PoliticaCancelacionProperties politica, PlatformTransactionManager tm) {
        this.alquilerRepository = alquilerRepository;
        this.pagoRepository = pagoRepository;
        this.unidadRepository = unidadRepository;
        this.estadoAlquilerRepository = estadoAlquilerRepository;
        this.usuarioRepository = usuarioRepository;
        this.boletaService = boletaService;
        this.emailService = emailService;
        this.politica = politica;
        this.tx = new TransactionTemplate(tm);
    }

    // ───────────────────────── Vista previa ─────────────────────────

    /** Qué pasaría si se cancelara ahora con ese motivo: porcentaje, monto a devolver y regla aplicada. */
    public PreviaCancelacion previa(Integer idAlquiler, String motivo) {
        // Si el plazo de pago ya venció, se cancela en este momento (sin esperar al proceso automático)
        if (politica.isCancelarVencidasAuto() && cancelarSiVencida(idAlquiler)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La reserva ya no está activa: se canceló automáticamente por no pagar dentro del plazo.");
        }
        return tx.execute(st -> {
            Alquiler a = buscar(idAlquiler);
            exigirCancelable(a);
            String m = motivoValido(motivo, true);
            List<Pago> pagos = pagosDelServicio(a);
            BigDecimal pagado = suma(pagos);
            LocalDateTime recojo = recojo(a);
            long horas = Math.floorDiv(Duration.between(LocalDateTime.now(), recojo).toMinutes(), 60L);
            boolean hayPagos = pagado.signum() > 0;
            int pct = hayPagos ? porcentaje(m, horas) : 0;
            BigDecimal reembolso = pagado.multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            String metodos = pagos.stream()
                    .map(p -> p.getMetodoPago() != null ? p.getMetodoPago().getNombreMetodo() : "")
                    .filter(x -> !x.isBlank()).distinct().collect(Collectors.joining(" + "));
            LocalDateTime vence = !hayPagos ? vencimientoPago(a, recojo) : null;
            return new PreviaCancelacion(a.getIdAlquiler(), nombreEstado(a), hayPagos, pagado, metodos, recojo, horas, pct,
                    reembolso, pagado.subtract(reembolso), regla(m, horas, pct, hayPagos),
                    !hayPagos || esAdmin(), vence, vence != null && LocalDateTime.now().isAfter(vence),
                    plazoALaDistancia(pagos));
        });
    }

    // ───────────────────────── Cancelar sin pagos ─────────────────────────

    public ResultadoCancelacion cancelarSinPago(Integer idAlquiler, CancelarReservaRequest req) {
        Alquiler cancelada = tx.execute(st -> {
            Alquiler a = buscar(idAlquiler);
            exigirCancelable(a);
            if (!pagosDelServicio(a).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Esta reserva ya tiene pagos: cancélala con devolución (solo el administrador).");
            }
            String motivoSinPago = motivoValido(req.motivo(), false);
            if ("VEHICULO_NO_DISPONIBLE".equals(motivoSinPago)) {
                // Sin pago no se cancela por falta de vehículo: se le ofrece otro modelo y, si no acepta, cancela el propio cliente
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Esta reserva no tiene pagos: ofrece al cliente otro vehículo y, si no acepta, cancélala a su solicitud.");
            }
            marcarCancelada(a, motivoSinPago, req.observaciones(), nombreUsuarioActual());
            return alquilerRepository.save(a);
        });
        java.util.concurrent.CompletableFuture.runAsync(() -> avisarCliente(cancelada, BigDecimal.ZERO, ""));
        return new ResultadoCancelacion(idAlquiler, BigDecimal.ZERO, "", "Reserva cancelada. El vehículo quedó libre.");
    }

    // ───────────────────────── Cancelar con reembolso ─────────────────────────

    public ResultadoCancelacion cancelarConReembolso(Integer idAlquiler, CancelarReservaRequest req) {
        if (!esAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el administrador puede cancelar una reserva pagada y autorizar la devolución.");
        }
        // Cada pago necesita su boleta para poder emitir la nota de crédito que la modifica
        Alquiler previo = buscar(idAlquiler);
        for (Pago p : pagosDelServicio(previo)) {
            boletaService.obtenerOEmitir(p.getIdPago());
        }

        List<Integer> idsDevolucion = new ArrayList<>();
        BigDecimal[] total = { BigDecimal.ZERO };
        Alquiler cancelada = tx.execute(st -> {
            Alquiler a = buscar(idAlquiler);
            exigirCancelable(a);
            List<Pago> pagos = pagosDelServicio(a);
            if (pagos.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta reserva no tiene pagos: usa la cancelación simple.");
            }
            String motivo = motivoValido(req.motivo(), false);
            long horas = Math.floorDiv(Duration.between(LocalDateTime.now(), recojo(a)).toMinutes(), 60L);
            int pct = porcentaje(motivo, horas);

            int n = 0;
            for (Pago original : pagos) {
                BigDecimal monto = original.getMontoTotal().multiply(BigDecimal.valueOf(pct))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                if (monto.signum() <= 0) continue;
                n++;
                Pago dev = new Pago();
                dev.setAlquiler(a);
                dev.setMetodoPago(original.getMetodoPago());
                dev.setMontoTotal(monto.negate());
                dev.setFechaPago(LocalDate.now());
                dev.setEstadoPago("Devuelto");
                dev.setTipoPago("REEMBOLSO");
                dev.setCanal("COUNTER");
                dev.setIdPagoReferencia(original.getIdPago());
                dev.setRegistradoPor(nombreUsuarioActual());
                dev.setValidadoPor("REEMBOLSO_AUTORIZADO_ADMIN");
                dev.setFechaValidacion(LocalDateTime.now());
                dev.setUltimosDigitos(original.getUltimosDigitos());
                dev.setMarcaTarjeta(original.getMarcaTarjeta());
                dev.setBancoOrigen(original.getBancoOrigen());
                String metodo = original.getMetodoPago() != null && original.getMetodoPago().getNombreMetodo() != null
                        ? original.getMetodoPago().getNombreMetodo().toUpperCase(Locale.ROOT) : "";
                if (metodo.contains("EFECTIVO")) {
                    dev.setNumeroOperacion(String.format("DEV-ALQ-%06d-%d", a.getIdAlquiler(), n));
                    // En efectivo el dinero sale de caja en este mismo momento
                    dev.setEstadoDevolucion("DEVUELTA");
                    dev.setFechaDevuelta(LocalDateTime.now());
                } else {
                    // Con tarjeta, Yape, Plin o transferencia, el sistema envía la devolución por el mismo método y genera su referencia.
                    // (Conectado a una pasarela real, esta referencia sería el número de operación que devuelve el proveedor.)
                    dev.setNumeroOperacion(String.format("DEV-ALQ-%06d-%d", a.getIdAlquiler(), n));
                    // Queda emitida en este momento; si no es efectivo, el estado en tramite se calcula por la fecha estimada
                    dev.setEstadoDevolucion("DEVUELTA");
                    dev.setFechaDevuelta(LocalDateTime.now());
                }
                Pago guardado = pagoRepository.save(dev);
                idsDevolucion.add(guardado.getIdPago());
                total[0] = total[0].add(monto);
            }

            marcarCancelada(a, motivo, req.observaciones(), nombreUsuarioActual());
            return alquilerRepository.save(a);
        });

        // Las notas de crédito se emiten cuando la devolución ya quedó guardada
        List<String> notas = new ArrayList<>();
        String aviso = "";
        for (Integer id : idsDevolucion) {
            try {
                notas.add(boletaService.emitirNotaCredito(id).numero());
            } catch (RuntimeException e) {
                log.warn("No se pudo emitir la nota de crédito de la devolución {}: {}", id, e.getMessage());
                aviso = " Ojo: la nota de crédito no se pudo emitir (" + e.getMessage() + "). Emítela manualmente.";
            }
        }
        String notasTexto = String.join(", ", notas);
        java.util.concurrent.CompletableFuture.runAsync(() -> avisarCliente(cancelada, total[0], notasTexto));
        String mensaje = total[0].signum() > 0
                ? "Reserva cancelada. Quedó registrada la devolución de S/ " + String.format(Locale.US, "%,.2f", total[0]) + " por el mismo método de pago."
                : "Reserva cancelada. Según la política no corresponde devolución.";
        return new ResultadoCancelacion(idAlquiler, total[0], notasTexto, mensaje + aviso);
    }

    // ───────────────────────── Estado de la devolución ─────────────────────────

    private List<Pago> devolucionesDe(Alquiler a) {
        return pagoRepository.findByAlquiler_IdAlquiler(a.getIdAlquiler()).stream()
                .filter(p -> "REEMBOLSO".equalsIgnoreCase(p.getTipoPago()))
                .toList();
    }

    /** Plazo en que llegaría la devolución para los pagos que no fueron en efectivo (vacío si todo fue en efectivo). */
    private String plazoALaDistancia(List<Pago> pagos) {
        List<Pago> aDistancia = pagos.stream()
                .filter(p -> !nombreMetodo(p).toUpperCase(Locale.ROOT).contains("EFECTIVO")).toList();
        return aDistancia.isEmpty() ? "" : plazoDe(aDistancia);
    }

    private static String nombreMetodo(Pago p) {
        return p.getMetodoPago() != null && p.getMetodoPago().getNombreMetodo() != null ? p.getMetodoPago().getNombreMetodo() : "";
    }

    /** Plazo que se le informa al cliente: una frase si es un solo método, o una por método si fueron varios. */
    private String plazoDe(List<Pago> devs) {
        List<String> metodos = devs.stream().map(CancelacionService::nombreMetodo).distinct().toList();
        if (metodos.isEmpty()) return "";
        if (metodos.size() == 1) return politica.plazoDevolucion(metodos.get(0));
        return metodos.stream().map(m -> capitalizar(m) + ": " + politica.plazoDevolucion(m)).collect(Collectors.joining("; "));
    }

    private static String capitalizar(String t) {
        return t == null || t.isEmpty() ? "" : t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1).toLowerCase(Locale.ROOT);
    }

    /** Qué pasó con el dinero de una reserva cancelada: monto devuelto, retenido, método, plazo y notas de crédito. */
    public DevolucionDTO devolucion(Integer idAlquiler) {
        return tx.execute(st -> {
            Alquiler a = buscar(idAlquiler);
            List<Pago> pagos = pagosDelServicio(a);
            List<Pago> devs = devolucionesDe(a);
            BigDecimal pagado = suma(pagos);
            BigDecimal devuelto = suma(devs).abs();
            BigDecimal retenido = pagado.subtract(devuelto).max(BigDecimal.ZERO);
            boolean cancelada = "CANCELADO".equalsIgnoreCase(nombreEstado(a));

            // Lo que no es efectivo tarda en llegar: "en trámite" hasta la fecha estimada, y desde entonces "devuelta"
            LocalDate hoyDev = LocalDate.now();
            LocalDate estimada = devs.stream()
                    .filter(p -> !nombreMetodo(p).toUpperCase(Locale.ROOT).contains("EFECTIVO"))
                    .map(p -> politica.fechaEstimadaDevolucion(
                            (p.getFechaDevuelta() != null ? p.getFechaDevuelta() : p.getFechaHoraPago()).toLocalDate(), nombreMetodo(p)))
                    .max(LocalDate::compareTo).orElse(null);
            boolean enTramite = estimada != null && !hoyDev.isAfter(estimada);
            String estado = devs.isEmpty() ? "SIN_DEVOLUCION" : enTramite ? "EN_TRAMITE" : "DEVUELTA";
            String metodos = (devs.isEmpty() ? pagos : devs).stream().map(CancelacionService::nombreMetodo)
                    .filter(x -> !x.isBlank()).distinct().collect(Collectors.joining(" + "));
            LocalDateTime solicitud = devs.stream().map(Pago::getFechaHoraPago).filter(java.util.Objects::nonNull)
                    .min(LocalDateTime::compareTo).orElse(null);
            LocalDateTime entregada = devs.stream().map(p -> p.getFechaDevuelta() != null ? p.getFechaDevuelta() : p.getFechaHoraPago())
                    .filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
            List<NotaCreditoRef> notas = new ArrayList<>();
            for (Pago d : devs) {
                boletaService.buscar(d.getIdPago()).ifPresent(n ->
                        notas.add(new NotaCreditoRef(n.getIdBoleta(), n.numero(), n.getTotal())));
            }
            // Lo que tarda es el banco o la billetera del cliente en reflejarlo; en efectivo se entrega en el momento
            List<Pago> aDistancia = devs.stream()
                    .filter(p -> !nombreMetodo(p).toUpperCase(Locale.ROOT).contains("EFECTIVO")).toList();
            String plazo = aDistancia.isEmpty() ? "" : plazoDe(aDistancia);
            String mensaje;
            if (devs.isEmpty()) {
                mensaje = pagado.signum() > 0 && cancelada ? "Según la política de cancelación no corresponde devolución." : "";
            } else {
                String monto = "S/ " + String.format(Locale.US, "%,.2f", devuelto);
                String cuando = entregada != null
                        ? " el " + entregada.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")) : "";
                if (aDistancia.isEmpty()) {
                    mensaje = "Le entregamos " + monto + " en efectivo en nuestra sede" + cuando + ".";
                } else {
                    String porMetodo = devs.stream().map(CancelacionService::nombreMetodo).filter(x -> !x.isBlank()).distinct()
                            .map(CancelacionService::capitalizar).collect(Collectors.joining(" + "));
                    mensaje = (enTramite ? "Iniciamos la devolución de " : "Devolvimos ") + monto + " por " + porMetodo + cuando
                            + (enTramite ? ". Llegará a su cuenta, a más tardar, el "
                            + estimada.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "." : ".");
                }
            }
            return new DevolucionDTO(a.getIdAlquiler(), cancelada, pagado, devuelto, retenido, metodos, estado, plazo,
                    solicitud, entregada, notas, mensaje, estimada);
        });
    }

    // ───────────────────────── Cambio de vehículo ─────────────────────────

    /** Unidades del mismo modelo, libres y sin cruces de fechas, para reemplazar la asignada. */
    public List<UnidadDisponible> unidadesDisponibles(Integer idAlquiler) {
        return tx.execute(st -> {
            Alquiler a = buscar(idAlquiler);
            exigirCancelable(a);
            return candidatas(a).stream()
                    .map(u -> new UnidadDisponible(u.getPlaca(), u.getColor(), u.getSede()))
                    .toList();
        });
    }

    public ResultadoCancelacion cambiarUnidad(Integer idAlquiler, String placa, String motivo) {
        return tx.execute(st -> {
            Alquiler a = buscar(idAlquiler);
            exigirCancelable(a);
            VehiculoUnidad nueva = candidatas(a).stream()
                    .filter(u -> u.getPlaca().equalsIgnoreCase(placa == null ? "" : placa.trim()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Esa unidad no está disponible para las fechas de esta reserva."));
            String anterior = a.getVehiculoUnidad() != null ? a.getVehiculoUnidad().getPlaca() : "sin asignar";
            a.setVehiculoUnidad(nueva);
            alquilerRepository.save(a);
            log.info("Cambio de unidad en ALQ-{}: {} -> {} (motivo: {}) por {}", a.getIdAlquiler(), anterior,
                    nueva.getPlaca(), motivo, nombreUsuarioActual());
            return new ResultadoCancelacion(idAlquiler, BigDecimal.ZERO, "",
                    "Vehículo cambiado: " + anterior + " → " + nueva.getPlaca() + ". La reserva y el pago se mantienen.");
        });
    }

    private List<VehiculoUnidad> candidatas(Alquiler a) {
        if (a.getVehiculoModelo() == null) return List.of();
        Integer modelo = a.getVehiculoModelo().getIdModelo();
        Integer actual = a.getVehiculoUnidad() != null ? a.getVehiculoUnidad().getIdUnidad() : null;
        // Para un recojo cercano la placa debe estar lista ya; para uno lejano también sirve una que hoy está en limpieza
        boolean cercano = a.getFechaInicio() != null
                && recojo(a).isBefore(LocalDateTime.now().plusHours(politica.getHorasReservaBloqueaLimpieza()));
        return unidadRepository.findAll().stream()
                .filter(u -> u.getVehiculoModelo() != null && modelo.equals(u.getVehiculoModelo().getIdModelo()))
                .filter(u -> !u.getIdUnidad().equals(actual))
                .filter(u -> a.getSedeRecojo() == null || (u.getSede() != null
                        && u.getSede().toUpperCase(Locale.ROOT).startsWith(a.getSedeRecojo().toUpperCase(Locale.ROOT).substring(0, Math.min(4, a.getSedeRecojo().length())))))
                .filter(u -> {
                    String e = u.getEstado() != null ? u.getEstado().getNombreEstado().toUpperCase(Locale.ROOT) : "";
                    return "DISPONIBLE".equals(e) || (!cercano && "LIMPIEZA".equals(e));
                })
                .filter(u -> !ocupada(u, a))
                .toList();
    }

    /** La unidad tiene otro alquiler vigente que se cruza con las fechas de esta reserva. */
    private boolean ocupada(VehiculoUnidad u, Alquiler a) {
        return alquilerRepository.findAll().stream()
                .filter(o -> !o.getIdAlquiler().equals(a.getIdAlquiler()))
                .filter(o -> o.getVehiculoUnidad() != null && u.getIdUnidad().equals(o.getVehiculoUnidad().getIdUnidad()))
                .filter(o -> !ESTADOS_QUE_LIBERAN.contains(nombreEstado(o).toUpperCase(Locale.ROOT)))
                .anyMatch(o -> o.getFechaInicio() != null && o.getFechaFinEstimada() != null
                        && !o.getFechaInicio().isAfter(a.getFechaFinEstimada())
                        && !o.getFechaFinEstimada().isBefore(a.getFechaInicio()));
    }

    // ───────────────────────── Baja de una unidad: reasignación automática ─────────────────────────

    /**
     * Cuando una unidad pasa a mantenimiento o deja de estar disponible, sus reservas todavía no entregadas se pasan solas
     * a otra unidad libre del mismo modelo. Si alguna no tiene reemplazo se lanza un error y no se cambia nada.
     * Debe llamarse dentro de la transacción de quien cambia el estado de la unidad.
     */
    public List<String> reasignarReservasDeUnidad(VehiculoUnidad unidad) {
        return reasignarReservasDeUnidad(unidad, null);
    }

    /** Las reservas todavía sin entregar de esa unidad que se recogen hasta la fecha indicada (sin fecha: todas). */
    private List<Alquiler> reservasPendientesDe(VehiculoUnidad unidad, LocalDateTime hasta) {
        return alquilerRepository.findAll().stream()
                .filter(a -> a.getVehiculoUnidad() != null && unidad.getIdUnidad().equals(a.getVehiculoUnidad().getIdUnidad()))
                .filter(a -> a.getFechaFinReal() == null)
                .filter(a -> ESTADOS_CANCELABLES.contains(nombreEstado(a).toUpperCase(Locale.ROOT)))
                .filter(a -> hasta == null || (a.getFechaInicio() != null && !recojo(a).isAfter(hasta)))
                .toList();
    }

    /** ¿Todas las reservas próximas de esa unidad tienen otra placa libre que las reemplace? */
    public boolean hayReemplazo(VehiculoUnidad unidad, LocalDateTime hasta) {
        return reservasPendientesDe(unidad, hasta).stream().noneMatch(a -> candidatas(a).isEmpty());
    }

    /** Igual que la anterior, pero solo con las reservas que se recogen hasta la fecha indicada: las lejanas no se tocan. */
    public List<String> reasignarReservasDeUnidad(VehiculoUnidad unidad, LocalDateTime hasta) {
        List<Alquiler> afectadas = reservasPendientesDe(unidad, hasta);
        List<String> avisos = new ArrayList<>();
        List<String> sinReemplazo = new ArrayList<>();
        for (Alquiler a : afectadas) {
            List<VehiculoUnidad> libres = candidatas(a);
            if (libres.isEmpty()) {
                sinReemplazo.add(String.format("ALQ-%06d", a.getIdAlquiler()));
                continue;
            }
            VehiculoUnidad nueva = libres.get(0);
            a.setVehiculoUnidad(nueva);
            alquilerRepository.save(a);
            avisos.add(String.format("ALQ-%06d: %s → %s", a.getIdAlquiler(), unidad.getPlaca(), nueva.getPlaca()));
            avisarCambioDeVehiculo(a, unidad.getPlaca(), nueva.getPlaca());
        }
        if (!sinReemplazo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No hay otra unidad libre del mismo modelo para reemplazar la reserva " + String.join(", ", sinReemplazo)
                            + ". Ofrece al cliente otro vehículo; si ya pagó, puedes cancelarla con devolución total. "
                            + "También puedes liberar una unidad antes de sacar esta de servicio.");
        }
        return avisos;
    }

    /**
     * Revisa las reservas próximas cuya placa quedó fuera de servicio (mantenimiento o no disponible) y las pasa solas a otra
     * placa libre del mismo modelo. Si no hay reemplazo, la reserva queda como está (se reintenta en la próxima revisión).
     * Devuelve cuántas reasignó.
     */
    public int reasignarReservasSinUnidadApta() {
        LocalDateTime limite = LocalDateTime.now().plusDays(politica.getDiasReasignarPorBaja());
        List<Integer> ids = alquilerRepository.findAll().stream()
                .filter(a -> a.getVehiculoUnidad() != null && a.getFechaFinReal() == null && a.getFechaEntrega() == null)
                .filter(a -> ESTADOS_CANCELABLES.contains(nombreEstado(a).toUpperCase(Locale.ROOT)))
                .filter(a -> {
                    String e = a.getVehiculoUnidad().getEstado() != null
                            ? a.getVehiculoUnidad().getEstado().getNombreEstado().toUpperCase(Locale.ROOT) : "";
                    return Set.of("MANTENIMIENTO", "NO DISPONIBLE").contains(e);
                })
                .filter(a -> a.getFechaInicio() != null && !recojo(a).isAfter(limite))
                .map(Alquiler::getIdAlquiler)
                .toList();
        int movidas = 0;
        for (Integer id : ids) {
            Boolean ok = tx.execute(st -> {
                Alquiler a = alquilerRepository.findById(id).orElse(null);
                if (a == null || a.getVehiculoUnidad() == null) return false;
                List<VehiculoUnidad> libres = candidatas(a);
                if (libres.isEmpty()) return false;
                String anterior = a.getVehiculoUnidad().getPlaca();
                VehiculoUnidad nueva = libres.get(0);
                a.setVehiculoUnidad(nueva);
                alquilerRepository.save(a);
                avisarCambioDeVehiculo(a, anterior, nueva.getPlaca());
                return true;
            });
            if (Boolean.TRUE.equals(ok)) movidas++;
        }
        return movidas;
    }

    private void avisarCambioDeVehiculo(Alquiler a, String placaAnterior, String placaNueva) {
        try {
            if (a.getCliente() == null || a.getCliente().getEmail() == null) return;
            String codigo = String.format("ALQ-%06d", a.getIdAlquiler());
            String cuerpo = emailService.parrafo("Hola <strong>" + emailService.escapar(a.getCliente().getNombres()) + "</strong>, "
                            + "por una razón operativa asignamos otro vehículo <strong>del mismo modelo</strong> a tu reserva <strong>" + codigo + "</strong>.")
                    + emailService.tabla(
                            new String[] { "Vehículo", nombreVehiculo(a) },
                            new String[] { "Placa anterior", placaAnterior },
                            new String[] { "Placa nueva", placaNueva, "destacada" })
                    + emailService.aviso("Tus fechas, horario y pago <strong>no cambian</strong>. Gracias por tu comprensión.");
            String html = emailService.plantilla("CAMBIO DE VEHÍCULO", EmailService.AZUL, "Actualizamos el vehículo de tu reserva", cuerpo,
                    "Ver mi reserva", "/cliente/reservas");
            emailService.enviarHtml(a.getCliente().getEmail(), "Cambio de vehículo en tu reserva " + codigo + " - Paul Car's", html);
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar al cliente del cambio de vehículo: {}", e.getMessage());
        }
    }

    // ───────────────────────── Reservas sin pago vencidas ─────────────────────────

    /** Igual que cancelarVencidas (y cancelarNoPresentadas), pero solo si la política lo tiene activado. */
    public void cancelarVencidasSiCorresponde() {
        if (politica.isCancelarVencidasAuto()) cancelarVencidas();
        if (politica.isCancelarNoPresentadasAuto()) cancelarNoPresentadas();
    }

    /**
     * Cancela las reservas pagadas cuyo cliente no se presentó: pasó la hora pactada más la espera y las llaves
     * no se entregaron. No corresponde devolución (queda retenido lo pagado). Devuelve cuántas canceló.
     */
    public int cancelarNoPresentadas() {
        List<Alquiler> candidatas = alquilerRepository.findAll().stream()
                .filter(this::estaNoPresentada)
                .toList();
        int canceladas = 0;
        for (Alquiler c : candidatas) {
            if (cancelarSiNoPresentada(c.getIdAlquiler())) canceladas++;
        }
        return canceladas;
    }

    private boolean estaNoPresentada(Alquiler a) {
        String estado = nombreEstado(a).toUpperCase(Locale.ROOT);
        if (!Set.of("RESERVADO", "PAGADO", "CONFIRMADO").contains(estado)) return false;
        if (a.getFechaEntrega() != null || a.getFechaInicio() == null) return false;
        // Sin pago, la reserva vence por el plazo de pago (otra regla): aquí solo las pagadas
        if (pagosDelServicio(a).isEmpty()) return false;
        return LocalDateTime.now().isAfter(recojo(a).plusMinutes(politica.getEsperaClienteMinutos()));
    }

    private boolean cancelarSiNoPresentada(Integer id) {
        Alquiler cancelada = tx.execute(st -> {
            Alquiler a = alquilerRepository.findById(id).orElse(null);
            if (a == null || !estaNoPresentada(a)) return null;
            marcarCancelada(a, "NO_SE_PRESENTO",
                    "Cancelada automáticamente: el cliente no se presentó dentro de los " + politica.getEsperaClienteMinutos()
                            + " minutos de espera. Lo pagado queda retenido.", "Sistema");
            return alquilerRepository.save(a);
        });
        if (cancelada == null) return false;
        java.util.concurrent.CompletableFuture.runAsync(() -> avisarCliente(cancelada, BigDecimal.ZERO, ""));
        return true;
    }

    /** Cancela las reservas aprobadas que no se pagaron dentro del plazo. Devuelve cuántas canceló. */
    public int cancelarVencidas() {
        List<Alquiler> vencidas = alquilerRepository.findAll().stream()
                .filter(this::estaVencida)
                .toList();
        int canceladas = 0;
        for (Alquiler v : vencidas) {
            if (cancelarSiVencida(v.getIdAlquiler())) canceladas++;
        }
        return canceladas;
    }

    private boolean estaVencida(Alquiler a) {
        if (!"RESERVADO".equalsIgnoreCase(nombreEstado(a)) || !pagosDelServicio(a).isEmpty()) return false;
        LocalDateTime vence = vencimientoPago(a, recojo(a));
        return vence != null && LocalDateTime.now().isAfter(vence);
    }

    /** Cancela esa reserva si sigue sin pago y ya pasó su plazo. Devuelve true si la canceló. */
    private boolean cancelarSiVencida(Integer id) {
        Alquiler cancelada = tx.execute(st -> {
            Alquiler a = alquilerRepository.findById(id).orElse(null);
            if (a == null || !estaVencida(a)) return null;
            marcarCancelada(a, "NO_PAGO_EN_PLAZO", "Cancelada automáticamente por no pagar dentro del plazo.", "Sistema");
            return alquilerRepository.save(a);
        });
        if (cancelada == null) return false;
        // El correo sale en segundo plano para que la cancelación sea inmediata
        java.util.concurrent.CompletableFuture.runAsync(() -> avisarCliente(cancelada, BigDecimal.ZERO, ""));
        return true;
    }

    /** Fecha límite de pago de una reserva aprobada sin pagos (null si no aplica). */
    public LocalDateTime limitePago(Alquiler a) {
        if (!"RESERVADO".equalsIgnoreCase(nombreEstado(a)) || !pagosDelServicio(a).isEmpty()) return null;
        return vencimientoPago(a, recojo(a));
    }

    /** Vence a las N horas de aprobada o X horas antes del recojo, lo que ocurra primero. */
    private LocalDateTime vencimientoPago(Alquiler a, LocalDateTime recojo) {
        if (!"RESERVADO".equalsIgnoreCase(nombreEstado(a))) return null;
        LocalDateTime aprobada = a.getFechaUltimoEstado() != null ? a.getFechaUltimoEstado() : LocalDateTime.now();
        return politica.vencimientoPago(aprobada, recojo);
    }

    // ───────────────────────── Reglas ─────────────────────────

    private int porcentaje(String motivo, long horas) {
        if ("NO_SE_PRESENTO".equals(motivo) || "NO_PAGO_EN_PLAZO".equals(motivo)) return 0;
        if (CULPA_EMPRESA.contains(motivo)) return 100;
        if (horas >= politica.getHorasReembolsoTotal()) return 100;
        if (horas >= politica.getHorasReembolsoParcial()) return politica.getPorcentajeParcial();
        return 0;
    }

    private String regla(String motivo, long horas, int pct, boolean hayPagos) {
        if (!hayPagos) return "La reserva no tiene pagos: se cancela sin devolución ni boleta.";
        if ("NO_SE_PRESENTO".equals(motivo)) return "El cliente no se presentó: no corresponde devolución.";
        if (CULPA_EMPRESA.contains(motivo)) return "Cancelación por causa de la empresa: devolución del 100%.";
        String cuando = horas >= 0 ? "Faltan " + horas + " h para el recojo" : "El recojo era hace " + Math.abs(horas) + " h";
        if (pct == 100) return cuando + ": devolución del 100% (" + politica.getHorasReembolsoTotal() + " h o más de anticipación).";
        if (pct > 0) return cuando + ": devolución del " + pct + "% (entre " + politica.getHorasReembolsoParcial()
                + " y " + politica.getHorasReembolsoTotal() + " h de anticipación).";
        return cuando + ": no corresponde devolución (menos de " + politica.getHorasReembolsoParcial() + " h de anticipación).";
    }

    // ───────────────────────── Utilitarios ─────────────────────────

    private void marcarCancelada(Alquiler a, String motivo, String observaciones, String quien) {
        a.setMotivoCancelacion(motivo);
        a.setObservacionCancelacion(observaciones == null || observaciones.isBlank() ? null : observaciones.trim());
        a.setFechaCancelacion(LocalDateTime.now());
        a.setCanceladoPor(quien);
        a.setCanceladoPorRol("Sistema".equals(quien) ? "SISTEMA" : rolActualParaCancelar());
        a.setFechaFinReal(LocalDate.now());
        a.setEstado(estadoAlquilerRepository.findByNombreEstadoIgnoreCase("CANCELADO").orElseThrow());
        if (a.getGarantiaRetenida() != null) {
            a.setGarantiaLiberada(a.getGarantiaRetenida()); // la retención en el POS se libera
        }
        a.setVehiculoUnidad(null);
    }

    private void avisarCliente(Alquiler a, BigDecimal devuelto, String notas) {
        try {
            if (a.getCliente() == null || a.getCliente().getEmail() == null) return;
            String codigo = String.format("ALQ-%06d", a.getIdAlquiler());
            List<Pago> devs = devolucionesDe(a);
            boolean hayDevolucion = devuelto.signum() > 0;
            boolean teniaPagos = !pagosDelServicio(a).isEmpty();

            String cuerpo = emailService.parrafo("Hola <strong>" + emailService.escapar(a.getCliente().getNombres()) + "</strong>, "
                    + "te confirmamos que tu reserva <strong>" + codigo + "</strong> (" + emailService.escapar(nombreVehiculo(a)) + ") fue cancelada.");
            if ("VEHICULO_NO_DISPONIBLE".equals(a.getMotivoCancelacion())) {
                cuerpo += emailService.parrafo("Lamentamos informarle que no podremos entregarle el vehículo reservado. "
                        + "Le pedimos disculpas por el inconveniente.");
            }
            cuerpo += emailService.tabla(new String[] { "Motivo", motivoParaCliente(a.getMotivoCancelacion()) });
            String boton = null, ruta = null;
            if (hayDevolucion) {
                String metodos = devs.stream().map(CancelacionService::nombreMetodo).filter(x -> !x.isBlank()).distinct()
                        .map(CancelacionService::capitalizar).collect(Collectors.joining(" + "));
                List<Pago> aDistancia = devs.stream()
                        .filter(p -> !nombreMetodo(p).toUpperCase(Locale.ROOT).contains("EFECTIVO")).toList();
                List<String[]> filas = new ArrayList<>();
                filas.add(new String[] { "Monto devuelto", "S/ " + String.format(Locale.US, "%,.2f", devuelto), "destacada" });
                filas.add(new String[] { "Método de devolución", metodos });
                if (!aDistancia.isEmpty()) {
                    String limite = aDistancia.stream()
                            .map(p -> politica.fechaEstimadaDevolucion(LocalDate.now(), nombreMetodo(p)))
                            .max(LocalDate::compareTo).get()
                            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    filas.add(new String[] { "Llegará a su cuenta, a más tardar, el", limite });
                }
                if (!notas.isEmpty()) filas.add(new String[] { "Nota de crédito electrónica", notas });
                cuerpo += emailService.tabla(filas.toArray(new String[0][]))
                        + emailService.aviso((aDistancia.isEmpty()
                                ? "Le entregamos la devolución en efectivo."
                                : "Ya iniciamos la devolución por el mismo método de pago.")
                                + " Puede ver el seguimiento y descargar su nota de crédito en <em>Mis reservas</em>.");
                boton = "Ver el detalle";
                ruta = "/cliente/reservas";
            } else if (teniaPagos) {
                cuerpo += emailService.aviso("Según la política de cancelación, esta reserva <strong>no tiene devolución</strong> de dinero.");
            }
            String html = emailService.plantilla("RESERVA CANCELADA", EmailService.ROJO, "Tu reserva fue cancelada", cuerpo, boton, ruta);
            emailService.enviarHtml(a.getCliente().getEmail(), "Reserva cancelada " + codigo + " - Paul Car's", html);
        } catch (RuntimeException e) {
            log.warn("No se pudo avisar al cliente de la cancelación: {}", e.getMessage());
        }
    }

    private static String motivoParaCliente(String motivo) {
        if (motivo == null) return "Reserva cancelada";
        // Mismos títulos que ve el cliente en el detalle de su reserva
        return switch (motivo) {
            case "CLIENTE_SOLICITO" -> "Cancelada a solicitud del cliente";
            case "NO_PAGO_EN_PLAZO" -> "No se pagó dentro del plazo";
            case "VEHICULO_NO_DISPONIBLE" -> "Vehículo no disponible";
            case "NO_SE_PRESENTO" -> "El cliente no se presentó al recojo";
            default -> "Reserva cancelada";
        };
    }

    private String nombreVehiculo(Alquiler a) {
        var m = a.getVehiculoModelo();
        return m != null && m.getMarca() != null ? m.getMarca().getNombreMarca() + " " + m.getNombreModelo() : "Vehículo";
    }

    private void exigirCancelable(Alquiler a) {
        if (!ESTADOS_CANCELABLES.contains(nombreEstado(a).toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede cancelar una reserva aprobada que todavía no se entregó.");
        }
    }

    private String motivoValido(String motivo, boolean permitirVacio) {
        String m = motivo == null ? "" : motivo.trim().toUpperCase(Locale.ROOT);
        if (m.isEmpty() && permitirVacio) return "CLIENTE_SOLICITO";
        if (!MOTIVOS.contains(m)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona un motivo de cancelación válido.");
        }
        return m;
    }

    private List<Pago> pagosDelServicio(Alquiler a) {
        return pagoRepository.findByAlquiler_IdAlquiler(a.getIdAlquiler()).stream()
                .filter(p -> p.getTipoPago() == null || "ALQUILER".equalsIgnoreCase(p.getTipoPago()))
                .toList();
    }

    private BigDecimal suma(List<Pago> pagos) {
        return pagos.stream().map(Pago::getMontoTotal).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime recojo(Alquiler a) {
        LocalTime hora = LocalTime.of(10, 0);
        try {
            if (a.getHoraRecojo() != null && !a.getHoraRecojo().isBlank()) hora = LocalTime.parse(a.getHoraRecojo().trim());
        } catch (DateTimeParseException ignorada) {
            // hora con formato inesperado: se usa la hora por defecto
        }
        return a.getFechaInicio().atTime(hora);
    }

    private Alquiler buscar(Integer id) {
        return alquilerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado"));
    }

    private String nombreEstado(Alquiler a) {
        return a.getEstado() != null && a.getEstado().getNombreEstado() != null ? a.getEstado().getNombreEstado() : "";
    }

    /** CLIENTE, ADMINISTRADOR o ASESOR según quien está autenticado. */
    private String rolActualParaCancelar() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "SISTEMA";
        boolean admin = auth.getAuthorities().stream().anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()));
        if (admin) return "ADMINISTRADOR";
        boolean cliente = auth.getAuthorities().stream().anyMatch(g -> "ROLE_CLIENTE".equals(g.getAuthority()));
        return cliente ? "CLIENTE" : "ASESOR";
    }

    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()));
    }

    private String nombreUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return "Sistema";
        return usuarioRepository.findByUsernameIgnoreCase(auth.getName())
                .map(u -> u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank() ? u.getNombreCompleto() : u.getUsername())
                .orElse(auth.getName());
    }
}
