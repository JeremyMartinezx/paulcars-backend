package com.example.alquiler_de_vehiculos.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.config.PoliticaCancelacionProperties;
import com.example.alquiler_de_vehiculos.dto.NotificacionDTO;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.NotificacionLeida;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.ClienteRepository;
import com.example.alquiler_de_vehiculos.repository.NotificacionLeidaRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoModeloRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoUnidadRepository;

import jakarta.transaction.Transactional;

@Service
public class NotificacionService {

    private final AlquilerRepository alquilerRepository;
    private final NotificacionLeidaRepository leidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoModeloRepository vehiculoModeloRepository;
    private final VehiculoUnidadRepository vehiculoUnidadRepository;
    private final PagoRepository pagoRepository;
    private final PoliticaCancelacionProperties politica;
    private final DanoService danoService;

    public NotificacionService(AlquilerRepository alquilerRepository,
                               NotificacionLeidaRepository leidaRepository,
                               UsuarioRepository usuarioRepository,
                               ClienteRepository clienteRepository,
                               VehiculoModeloRepository vehiculoModeloRepository,
                               VehiculoUnidadRepository vehiculoUnidadRepository,
                               PagoRepository pagoRepository,
                               PoliticaCancelacionProperties politica,
                               DanoService danoService) {
        this.danoService = danoService;
        this.pagoRepository = pagoRepository;
        this.politica = politica;
        this.alquilerRepository = alquilerRepository;
        this.leidaRepository = leidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.vehiculoModeloRepository = vehiculoModeloRepository;
        this.vehiculoUnidadRepository = vehiculoUnidadRepository;
    }

    public List<NotificacionDTO> listar(String username) {
        Set<String> leidas = leidaRepository.findByUsername(username).stream()
                .map(NotificacionLeida::getClave)
                .collect(Collectors.toSet());

        Usuario usuario = buscarUsuario(username);
        if (usuario != null && usuario.getRol() != null) {
            String rol = usuario.getRol().getNombreRol().toUpperCase();
            if ("CLIENTE".equals(rol)) {
                return listarParaCliente(usuario, username, leidas);
            }
            if ("ADMIN".equals(rol)) {
                return listarParaAdministrador(leidas);
            }
            if ("EMPLEADO".equals(rol)) {
                return listarParaAsesor(leidas);
            }
        }
        return listarParaAsesor(leidas);
    }

    private List<NotificacionDTO> listarParaAdministrador(Set<String> leidas) {
        List<NotificacionDTO> lista = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        List<Alquiler> todos = alquilerRepository.findAll();
        long enRuta = todos.stream().filter(a -> a.getEstado() != null && "ACTIVO".equalsIgnoreCase(a.getEstado().getNombreEstado())).count();
        long pendientes = todos.stream().filter(a -> a.getEstado() != null && "PENDIENTE".equalsIgnoreCase(a.getEstado().getNombreEstado())).count();

        String claveResumen = "ADM-RESUMEN-" + hoy;
        lista.add(new NotificacionDTO(
                claveResumen,
                "RESUMEN_EJECUTIVO",
                "Resumen Operativo del Día",
                "Flota activa: " + enRuta + " vehículos en circulación y " + pendientes + " solicitudes en espera de atención en counter.",
                "Hoy",
                null,
                leidas.contains(claveResumen)));

        List<VehiculoModelo> modelos = vehiculoModeloRepository.findAll();
        for (VehiculoModelo m : modelos) {
            long disponibles = m.getUnidades() != null ? m.getUnidades().stream()
                    .filter(u -> u.getEstado() != null && "DISPONIBLE".equalsIgnoreCase(u.getEstado().getNombreEstado()))
                    .count() : 0;
            if (disponibles == 0) {
                String claveStock = "ADM-STOCK-" + m.getIdModelo();
                String nombreCompleto = (m.getMarca() != null ? m.getMarca().getNombreMarca() + " " : "") + m.getNombreModelo();
                lista.add(new NotificacionDTO(
                        claveStock,
                        "ALERTA_STOCK",
                        "Stock Crítico: " + nombreCompleto,
                        "El modelo " + nombreCompleto + " no cuenta con unidades disponibles en patio (100% en contratos activos).",
                        "Hoy",
                        null,
                        leidas.contains(claveStock)));
            }
        }

        List<VehiculoUnidad> enTaller = vehiculoUnidadRepository.findByEstado_NombreEstadoIgnoreCase("MANTENIMIENTO");
        if (!enTaller.isEmpty()) {
            String claveMantenimiento = "ADM-TALLER-" + hoy;
            lista.add(new NotificacionDTO(
                    claveMantenimiento,
                    "MANTENIMIENTO_FLOTA",
                    "Flota en Mantenimiento Técnico",
                    enTaller.size() + " unidades físicas derivadas a revisión mecánica preventiva o taller.",
                    "Hoy",
                    null,
                    leidas.contains(claveMantenimiento)));
        }

        List<Alquiler> liquidadosConObservacion = todos.stream()
                .filter(a -> a.getEstado() != null && "FINALIZADO".equalsIgnoreCase(a.getEstado().getNombreEstado()))
                .filter(a -> a.getGarantiaRetenida() != null && a.getGarantiaRetenida().compareTo(java.math.BigDecimal.ZERO) > 0)
                .limit(2)
                .toList();

        for (Alquiler alq : liquidadosConObservacion) {
            String claveAuditoria = "ADM-AUDITORIA-" + alq.getIdAlquiler();
            lista.add(new NotificacionDTO(
                    claveAuditoria,
                    "AUDITORIA_LIQUIDACION",
                    "Auditoría: Liquidación con Cargos ALQ-" + String.format("%06d", alq.getIdAlquiler()),
                    "Contrato para " + vehiculo(alq) + " finalizó con retención de garantía de S/ " + alq.getGarantiaRetenida() + " por observaciones técnicas.",
                    alq.getFechaFinReal() != null ? alq.getFechaFinReal().toString() : "Reciente",
                    alq.getIdAlquiler(),
                    leidas.contains(claveAuditoria)));
        }

        // Daños recibidos que el administrador debe evaluar (la garantía sigue retenida)
        todos.stream()
                .filter(a -> DanoService.PENDIENTE.equals(a.getEstadoDanos()))
                .limit(5)
                .forEach(alq -> {
                    String claveDanos = "ADM-DANOS-" + alq.getIdAlquiler();
                    lista.add(new NotificacionDTO(
                            claveDanos,
                            "DANOS_PENDIENTES",
                            "Daños por evaluar: ALQ-" + String.format("%06d", alq.getIdAlquiler()),
                            vehiculo(alq) + " fue recibido con daños nuevos. La garantía de S/ " + alq.getGarantiaRetenida()
                                    + " está retenida hasta que los evalúe.",
                            alq.getFechaFinReal() != null ? alq.getFechaFinReal().toString() : "Reciente",
                            alq.getIdAlquiler(),
                            leidas.contains(claveDanos)));
                });

        lista.sort(Comparator.comparing(NotificacionDTO::leida)
                .thenComparing(NotificacionDTO::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return lista;
    }

    private List<NotificacionDTO> listarParaAsesor(Set<String> leidas) {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        List<NotificacionDTO> lista = new ArrayList<>();
        List<Alquiler> todos = alquilerRepository.findAll();

        for (Alquiler a : todos) {
            if (a.getEstado() == null) continue;
            String estado = a.getEstado().getNombreEstado().toUpperCase();
            String cliNom = cliente(a);
            String vehNom = vehiculo(a);
            String sedeRec = a.getSedeRecojo() != null ? a.getSedeRecojo() : "Counter";
            String sedeDev = a.getSedeDevolucion() != null ? a.getSedeDevolucion() : "Counter";
            String fechaReg = a.getFechaReserva() != null ? a.getFechaReserva().toString() : (a.getFechaInicio() != null ? a.getFechaInicio().toString() : "Reciente");
            String placaTxt = (a.getVehiculoUnidad() != null && a.getVehiculoUnidad().getPlaca() != null)
                    ? " (" + a.getVehiculoUnidad().getPlaca() + ")"
                    : (a.getPlaca() != null && !a.getPlaca().isBlank() ? " (" + a.getPlaca() + ")" : "");

            if ("PENDIENTE".equals(estado)) {
                String claveSolicitud = "ASE-SOLICITUD-" + a.getIdAlquiler();
                lista.add(new NotificacionDTO(
                        claveSolicitud,
                        "NUEVA_RESERVA",
                        "Solicitud Pendiente: " + vehNom,
                        cliNom + " solicitó " + vehNom + " del " + a.getFechaInicio() + " al " + a.getFechaFinEstimada() + " en sede " + sedeRec + ". Requiere validación de licencia y asignación de placa.",
                        fechaReg,
                        a.getIdAlquiler(),
                        leidas.contains(claveSolicitud)));
            }

            if ("ACTIVO".equals(estado) || "EN_CURSO".equals(estado)) {
                LocalDate vence = a.getFechaFinEstimada();
                String horaDevStr = a.getHoraDevolucion() != null ? a.getHoraDevolucion() : "18:00";

                if (vence != null && vence.equals(hoy)) {
                    int diffMin = calcularMinutosRestantes(horaDevStr, ahora);
                    String claveDev = "ASE-RET-HOY-" + a.getIdAlquiler() + "-" + hoy;

                    if (diffMin < 0) {
                        int atraso = Math.abs(diffMin);
                        String atrasoTxt = (atraso / 60 > 0) ? (atraso / 60) + "h " + (atraso % 60) + "m" : atraso + " min";
                        lista.add(new NotificacionDTO(
                                claveDev,
                                "DEVOLUCION_VENCIDA",
                                "Retorno con Retraso: " + vehNom,
                                vehNom + placaTxt + " de " + cliNom + " debía retornar hoy a las " + horaDevStr + " hrs en sede " + sedeDev + " (" + atrasoTxt + " de retraso).",
                                "Hoy • " + horaDevStr + " hrs",
                                a.getIdAlquiler(),
                                leidas.contains(claveDev)));
                    } else if (diffMin <= 60) {
                        lista.add(new NotificacionDTO(
                                claveDev,
                                "DEVOLUCION_1H",
                                "Retorno Próximo (< 1h): " + vehNom,
                                vehNom + placaTxt + " de " + cliNom + " retorna en ~" + diffMin + " min (" + horaDevStr + " hrs) en sede " + sedeDev + ". Prepara la recepción de llaves.",
                                "Hoy • " + horaDevStr + " hrs",
                                a.getIdAlquiler(),
                                leidas.contains(claveDev)));
                    } else {
                        lista.add(new NotificacionDTO(
                                claveDev,
                                "DEVOLUCION_HOY",
                                "Retorno Programado Hoy: " + vehNom,
                                vehNom + placaTxt + " de " + cliNom + " retorna hoy a las " + horaDevStr + " hrs en sede " + sedeDev + ". Prepara el Check-Out.",
                                "Hoy • " + horaDevStr + " hrs",
                                a.getIdAlquiler(),
                                leidas.contains(claveDev)));
                    }
                }
            }

            boolean esReservaParaHoy = ("RESERVADO".equals(estado) || "PAGADO".equals(estado) || "CONFIRMADO".equals(estado) || "APROBADO".equals(estado))
                    && hoy.equals(a.getFechaInicio());
            if (esReservaParaHoy) {
                String claveEntregaHoy = "ASE-ENTREGA-" + a.getIdAlquiler() + "-" + hoy;
                String horaRec = a.getHoraRecojo() != null ? a.getHoraRecojo() : "10:00";
                lista.add(new NotificacionDTO(
                        claveEntregaHoy,
                        "RECOJO_HOY",
                        "Entrega de Llaves Hoy: " + vehNom,
                        cliNom + " tiene programado retirar la unidad hoy a las " + horaRec + " hrs en sede " + sedeRec + ".",
                        "Hoy • " + horaRec + " hrs",
                        a.getIdAlquiler(),
                        leidas.contains(claveEntregaHoy)));
            }
        }

        lista.sort(Comparator.comparing(NotificacionDTO::leida)
                .thenComparing(NotificacionService::prioridadNotificacion)
                .thenComparing(NotificacionDTO::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return lista;
    }

    private List<NotificacionDTO> listarParaCliente(Usuario usuario, String username, Set<String> leidas) {
        Cliente cliente = usuario.getCliente();
        if (cliente == null) {
            String email = usuario.getEmailRecuperacion() != null ? usuario.getEmailRecuperacion() : username;
            if (email != null && email.contains("@")) {
                cliente = clienteRepository.findByEmailIgnoreCase(email.trim()).orElse(null);
            }
        }
        if (cliente == null) {
            return Collections.emptyList();
        }

        List<Alquiler> misAlquileres = alquilerRepository.findByCliente_IdCliente(cliente.getIdCliente());
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        List<NotificacionDTO> lista = new ArrayList<>();

        // Bienvenida: la ven todos los clientes (registrados por el personal o por el portal) y queda como leída al abrirla
        String primerNombre = cliente.getNombres() != null && !cliente.getNombres().isBlank()
                ? cliente.getNombres().trim().split("\s+")[0] : "";
        // Solo en la primera sesión de una cuenta nueva y mientras no haya hecho ningún alquiler
        // (al iniciar sesión por segunda vez se registra CLI-BIENVENIDA-FIN y deja de mostrarse)
        String claveBienvenida = "CLI-BIENVENIDA-" + cliente.getIdCliente();
        if (misAlquileres.isEmpty() && !leidas.contains(AuthService.CLAVE_BIENVENIDA_FIN)) {
            lista.add(new NotificacionDTO(
                    claveBienvenida,
                    "BIENVENIDA",
                    "¡Bienvenido a Paul Car's" + (primerNombre.isEmpty() ? "" : ", " + primerNombre) + "!",
                    "Ya puede ver nuestro catálogo y elegir su próximo vehículo. Cuando lo tenga, solicite su reserva y nuestro equipo la atenderá.",
                    "Bienvenida",
                    null,
                    leidas.contains(claveBienvenida)));
        }

        boolean faltaDni = cliente.getNumeroDocumento() == null || cliente.getNumeroDocumento().isBlank() || cliente.getNumeroDocumento().startsWith("PEND-");
        boolean faltaLicencia = cliente.getLicenciaConducir() == null || cliente.getLicenciaConducir().isBlank();
        boolean faltaDireccion = cliente.getDireccion() == null || cliente.getDireccion().isBlank();

        if (faltaDni || faltaLicencia || faltaDireccion) {
            String clave = "CLI-PERFIL-INCOMPLETO-" + cliente.getIdCliente();
            lista.add(new NotificacionDTO(
                    clave,
                    "ACTUALIZA_PERFIL",
                    "Completa tus datos en Mi Perfil",
                    "Falta registrar en Mi Perfil: " + faltantes(faltaDni, faltaLicencia, faltaDireccion) + ". Complételo para solicitar reservas sin demoras.",
                    "Importante",
                    null,
                    leidas.contains(clave)));
        }

        for (Alquiler a : misAlquileres) {
            if (a.getEstado() == null) continue;
            String estado = a.getEstado().getNombreEstado().toUpperCase();
            String vehiculoNom = vehiculo(a);
            String sedeDev = a.getSedeDevolucion() != null ? a.getSedeDevolucion() : "Sede Counter";
            String sedeRec = a.getSedeRecojo() != null ? a.getSedeRecojo() : "Sede Counter";
            String fechaReg = a.getFechaReserva() != null ? a.getFechaReserva().toString() : (a.getFechaInicio() != null ? a.getFechaInicio().toString() : "Reciente");

            if ("PENDIENTE".equals(estado)) {
                String claveCreacion = "CLI-PENDIENTE-" + a.getIdAlquiler();
                lista.add(new NotificacionDTO(
                        claveCreacion,
                        "SOLICITUD_PENDIENTE",
                        "Solicitud en Evaluación: " + vehiculoNom,
                        "Tu solicitud para " + vehiculoNom + " (del " + a.getFechaInicio() + " al " + a.getFechaFinEstimada() + ") está siendo evaluada por nuestro equipo en counter.",
                        fechaReg,
                        a.getIdAlquiler(),
                        leidas.contains(claveCreacion)));
            } else if ("RESERVADO".equals(estado)) {
                String claveAprobada = "CLI-RESERVADO-" + a.getIdAlquiler();
                // Reserva registrada por el asesor o el administrador: se avisa hasta cuándo puede pagarse
                String limite = "";
                if (a.getFechaInicio() != null) {
                    LocalTime horaRec = LocalTime.of(10, 0);
                    try {
                        if (a.getHoraRecojo() != null && !a.getHoraRecojo().isBlank()) horaRec = LocalTime.parse(a.getHoraRecojo().trim());
                    } catch (java.time.format.DateTimeParseException ignorada) {
                        // hora con formato inesperado: se usa la hora por defecto
                    }
                    java.time.LocalDateTime aprobada = a.getFechaUltimoEstado() != null ? a.getFechaUltimoEstado() : java.time.LocalDateTime.now();
                    java.time.LocalDateTime vence = politica.vencimientoPago(aprobada, a.getFechaInicio().atTime(horaRec));
                    if (vence != null) {
                        limite = " Tiene disponible para pagar hasta el " + vence.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm"))
                                + ". Si no se paga hasta esa hora, la reserva se cancelará.";
                    }
                }
                lista.add(new NotificacionDTO(
                        claveAprobada,
                        "RESERVA_APROBADA",
                        "Reserva registrada: " + vehiculoNom,
                        "Su reserva fue registrada por nuestro equipo para el recojo el " + (a.getFechaInicio() != null ? a.getFechaInicio().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-") + " en sede " + sedeRec + "." + limite,
                        a.getFechaInicio() != null ? a.getFechaInicio().toString() : fechaReg,
                        a.getIdAlquiler(),
                        leidas.contains(claveAprobada)));
            } else if ("PAGADO".equals(estado) || "CONFIRMADO".equals(estado)) {
                // Aviso de recojo: el día anterior ("mañana") y de nuevo el mismo día ("hoy"), con la hora que el cliente eligió
                if (a.getFechaInicio() != null && (a.getFechaInicio().equals(hoy) || a.getFechaInicio().equals(hoy.plusDays(1)))) {
                    boolean esHoy = a.getFechaInicio().equals(hoy);
                    String horaRecojo = a.getHoraRecojo() != null && !a.getHoraRecojo().isBlank() ? a.getHoraRecojo().trim() : "10:00";
                    String sedeNombre = sedeRec.length() > 1 ? sedeRec.substring(0, 1).toUpperCase() + sedeRec.substring(1).toLowerCase() : sedeRec;
                    String claveRecojo = "CLI-RECOJO-" + (esHoy ? "HOY-" : "MANANA-") + a.getIdAlquiler();
                    lista.add(new NotificacionDTO(
                            claveRecojo,
                            "RECOJO_CLIENTE",
                            (esHoy ? "Su vehículo está listo: " : "Mañana recoge su vehículo: ") + vehiculoNom,
                            "Puede recogerlo " + (esHoy ? "hoy" : "mañana") + " a partir de las " + horaRecojo + " en la sede "
                                    + sedeNombre + ". Lleve su DNI y su licencia de conducir.",
                            a.getFechaInicio().toString(),
                            a.getIdAlquiler(),
                            leidas.contains(claveRecojo)));
                }
                String clavePago = "CLI-PAGO-" + a.getIdAlquiler();
                lista.add(new NotificacionDTO(
                        clavePago,
                        "PAGO_CONFIRMADO",
                        "Pago Confirmado: " + vehiculoNom,
                        "Tu pago para " + vehiculoNom + " fue acreditado con éxito. Tu Voucher Oficial de Reserva se encuentra disponible para consulta.",
                        a.getFechaInicio() != null ? a.getFechaInicio().toString() : fechaReg,
                        a.getIdAlquiler(),
                        leidas.contains(clavePago)));
            } else if ("ACTIVO".equals(estado) || "EN_CURSO".equals(estado)) {
                if (a.getFechaFinEstimada() != null && a.getFechaFinEstimada().equals(hoy)) {
                    String horaDevStr = a.getHoraDevolucion() != null ? a.getHoraDevolucion() : "18:00";
                    int diffMin = calcularMinutosRestantes(horaDevStr, ahora);

                    if (diffMin < 0) {
                        String clave = "CLI-RET-VENCIDO-" + a.getIdAlquiler() + "-" + hoy;
                        lista.add(new NotificacionDTO(
                                clave,
                                "DEVOLUCION_VENCIDA",
                                "Retorno con Retraso Hoy: " + vehiculoNom,
                                "El horario pactado (" + horaDevStr + " hrs) fue superado. Acércate de inmediato a sede " + sedeDev + " para la entrega del vehículo.",
                                "Hoy • " + horaDevStr + " hrs",
                                a.getIdAlquiler(),
                                leidas.contains(clave)));
                    } else {
                        String clave = "CLI-RET-HOY-" + a.getIdAlquiler() + "-" + hoy;
                        lista.add(new NotificacionDTO(
                                clave,
                                "DEVOLUCION_HOY",
                                "Retorno Programado Hoy: " + vehiculoNom,
                                "Recuerda devolver tu vehículo hoy a las " + horaDevStr + " hrs en sede " + sedeDev + ".",
                                "Hoy • " + horaDevStr + " hrs",
                                a.getIdAlquiler(),
                                leidas.contains(clave)));
                    }
                } else {
                    String claveRuta = "CLI-ENRUTA-" + a.getIdAlquiler();
                    lista.add(new NotificacionDTO(
                            claveRuta,
                            "ALQUILER_EN_CURSO",
                            "Vehículo en Ruta: " + vehiculoNom,
                            "¡Llaves entregadas! Su contrato está activo. Devolución programada para el "
                                    + (a.getFechaFinEstimada() != null ? a.getFechaFinEstimada().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-")
                                    + " a las " + (a.getHoraDevolucion() != null && !a.getHoraDevolucion().isBlank() ? a.getHoraDevolucion().trim() : "18:00")
                                    + " en la sede " + (sedeDev.length() > 1 ? sedeDev.substring(0, 1).toUpperCase() + sedeDev.substring(1).toLowerCase() : sedeDev) + ".",
                            a.getFechaInicio() != null ? a.getFechaInicio().toString() : "En ruta",
                            a.getIdAlquiler(),
                            leidas.contains(claveRuta)));
                }
            } else if ("FINALIZADO".equals(estado) || "CERRADO_CON_DEUDA".equals(estado)) {
                String claveFin = "CLI-FINALIZADO-" + a.getIdAlquiler();
                String fechaFin = a.getFechaFinReal() != null ? a.getFechaFinReal().toString() : (a.getFechaFinEstimada() != null ? a.getFechaFinEstimada().toString() : "Finalizado");
                java.util.function.Function<java.math.BigDecimal, String> soles = v -> String.format(java.util.Locale.US, "S/ %,.2f", v == null ? java.math.BigDecimal.ZERO : v);
                if (DanoService.PENDIENTE.equals(a.getEstadoDanos())) {
                    String claveDanos = "CLI-DANOS-" + a.getIdAlquiler();
                    lista.add(new NotificacionDTO(
                            claveDanos,
                            "DANOS_EN_EVALUACION",
                            "Evaluando daños del vehículo: " + vehiculoNom,
                            "Recibimos el vehículo con observaciones. Su garantía de " + soles.apply(a.getGarantiaRetenida())
                                    + " permanece retenida mientras evaluamos los daños; le avisaremos el resultado. Puede ver el detalle en su reserva.",
                            fechaFin,
                            a.getIdAlquiler(),
                            leidas.contains(claveDanos)));
                } else if (DanoService.EVALUADO.equals(a.getEstadoDanos())) {
                    java.math.BigDecimal pendiente = danoService.deudaPendiente(a);
                    String detalle = "Evaluamos los daños de " + vehiculoNom + ": costo de reparación " + soles.apply(a.getCostoDanos());
                    if (a.getMontoCubiertoSeguro() != null && a.getMontoCubiertoSeguro().signum() > 0) {
                        detalle += ", seguro cubrió " + soles.apply(a.getMontoCubiertoSeguro());
                    }
                    detalle += ", a su cargo " + soles.apply(a.getCargoDanos()) + ". Garantía liberada: " + soles.apply(a.getGarantiaLiberada()) + ".";
                    if (pendiente.signum() > 0) {
                        detalle += " Queda un saldo de " + soles.apply(pendiente) + " por pagar en nuestro mostrador.";
                    }
                    lista.add(new NotificacionDTO(
                            claveFin,
                            pendiente.signum() > 0 ? "CERRADO_CON_DEUDA" : "ALQUILER_FINALIZADO",
                            "Evaluación de daños: " + vehiculoNom,
                            detalle,
                            fechaFin,
                            a.getIdAlquiler(),
                            leidas.contains(claveFin)));
                } else {
                    boolean conDeuda = "CERRADO_CON_DEUDA".equals(estado);
                    String texto = conDeuda
                            ? "Su alquiler de " + vehiculoNom + " finalizó con cargos que superan la garantía. Saldo por pagar: "
                                    + soles.apply(danoService.deudaPendiente(a)) + "."
                            : "Tu alquiler para " + vehiculoNom + " finalizó conforme en sede " + sedeDev + ". "
                                    + (a.getGarantiaLiberada() != null && a.getGarantiaRetenida() != null
                                            && a.getGarantiaLiberada().compareTo(a.getGarantiaRetenida()) < 0
                                            ? "Se liberó " + soles.apply(a.getGarantiaLiberada()) + " de su garantía (se descontaron cargos del cierre)."
                                            : "Depósito de garantía liberado al 100%.");
                    lista.add(new NotificacionDTO(
                            claveFin,
                            conDeuda ? "CERRADO_CON_DEUDA" : "ALQUILER_FINALIZADO",
                            "Alquiler Concluido: " + vehiculoNom,
                            texto,
                            fechaFin,
                            a.getIdAlquiler(),
                            leidas.contains(claveFin)));
                }
            } else if ("CANCELADO".equals(estado)) {
                agregarAvisosDeCancelacion(lista, a, vehiculoNom, leidas);
            } else if ("RECHAZADO".equals(estado) || "RECHAZADO_DOCUMENTACION".equals(estado)) {
                String claveRechazo = "CLI-RECHAZADO-" + a.getIdAlquiler();
                String mot = a.getMotivoRechazo() != null ? AlquilerService.motivoRechazoParaCliente(a.getMotivoRechazo()) : "Validación de counter";
                lista.add(new NotificacionDTO(
                        claveRechazo,
                        "SOLICITUD_RECHAZADA",
                        "Solicitud no aprobada: " + vehiculoNom,
                        "Tu solicitud para " + vehiculoNom + " no fue aprobada (" + mot + ").",
                        fechaReg,
                        a.getIdAlquiler(),
                        leidas.contains(claveRechazo)));
            }
        }

        // Orden cronológico (lo más reciente arriba), sin separar leídas de no leídas:
        // en un mismo día, el aviso de recojo va antes que el pago confirmado, y este antes que la reserva
        lista.sort(Comparator.comparing(NotificacionDTO::fecha, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparingInt(NotificacionService::ordenDelCicloCliente));
        List<NotificacionDTO> visibles = limitar(lista, 8);
        // La bienvenida fue la primera que recibió el cliente: queda siempre al final, debajo de las más nuevas
        List<NotificacionDTO> ordenada = new ArrayList<>();
        visibles.stream().filter(x -> !"BIENVENIDA".equals(x.tipo())).forEach(ordenada::add);
        visibles.stream().filter(x -> "BIENVENIDA".equals(x.tipo())).forEach(ordenada::add);
        return ordenada;
    }

    /** Si hay muchas, se conservan todas las no leídas y las leídas más recientes; las leídas más antiguas se ocultan. */
    private static List<NotificacionDTO> limitar(List<NotificacionDTO> lista, int maximo) {
        if (lista.size() <= maximo) return lista;
        boolean[] conservar = new boolean[lista.size()];
        int total = 0;
        for (int k = 0; k < lista.size(); k++) {
            if (!lista.get(k).leida() || "BIENVENIDA".equals(lista.get(k).tipo())) {
                conservar[k] = true;
                total++;
            }
        }
        for (int k = lista.size() - 1; k >= 0 && total < maximo; k--) {
            if (!conservar[k]) {
                conservar[k] = true;
                total++;
            }
        }
        List<NotificacionDTO> resultado = new ArrayList<>();
        for (int k = 0; k < lista.size(); k++) {
            if (conservar[k]) resultado.add(lista.get(k));
        }
        return resultado;
    }

    // Reserva cancelada: avisa del estado de la devolución (en proceso o realizada) o de por qué no hay devolución
    private void agregarAvisosDeCancelacion(List<NotificacionDTO> lista, Alquiler a, String vehiculoNom, Set<String> leidas) {
        List<Pago> todos = pagoRepository.findByAlquiler_IdAlquiler(a.getIdAlquiler());
        List<Pago> devoluciones = todos.stream().filter(p -> "REEMBOLSO".equalsIgnoreCase(p.getTipoPago())).toList();
        boolean tuvoPago = todos.stream().anyMatch(p -> p.getTipoPago() == null || "ALQUILER".equalsIgnoreCase(p.getTipoPago()));
        String fecha = a.getFechaCancelacion() != null ? a.getFechaCancelacion().toLocalDate().toString() : "Reciente";
        Integer id = a.getIdAlquiler();

        if (devoluciones.isEmpty()) {
            String clave = "CLI-CANCELADA-" + id;
            String detalle = "NO_PAGO_EN_PLAZO".equals(a.getMotivoCancelacion())
                    ? "Se canceló porque no se pagó dentro del plazo."
                    : tuvoPago ? "Según la política de cancelación, esta reserva no tiene devolución de dinero."
                    : "";
            lista.add(new NotificacionDTO(clave, "RESERVA_CANCELADA", "Reserva cancelada: " + vehiculoNom,
                    ("Tu reserva ALQ-" + String.format("%06d", id) + " fue cancelada. " + detalle).trim(), fecha, id, leidas.contains(clave)));
            return;
        }

        java.math.BigDecimal monto = devoluciones.stream().map(p -> p.getMontoTotal().abs())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        String metodo = devoluciones.stream()
                .map(p -> p.getMetodoPago() != null ? p.getMetodoPago().getNombreMetodo() : "")
                .filter(x -> x != null && !x.isBlank()).distinct().collect(Collectors.joining(" + "));
        String montoTxt = "S/ " + String.format(java.util.Locale.US, "%,.2f", monto);
        boolean efectivo = metodo.toUpperCase().contains("EFECTIVO");
        java.time.LocalDate limite = politica.fechaEstimadaDevolucion(
                a.getFechaCancelacion() != null ? a.getFechaCancelacion().toLocalDate() : java.time.LocalDate.now(), metodo);
        boolean enTramite = !efectivo && !java.time.LocalDate.now().isAfter(limite);
        String clave = "CLI-DEVOL-" + (enTramite ? "TRAMITE-" : "REALIZADA-") + id;
        String texto = efectivo
                ? "Le entregamos " + montoTxt + " en efectivo."
                : enTramite
                    ? "Iniciamos la devolución de " + montoTxt + " por " + metodo.toLowerCase() + ". Llegará a su cuenta, a más tardar, el "
                        + limite.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "."
                    : "Devolvimos " + montoTxt + " por " + metodo.toLowerCase() + ".";
        lista.add(new NotificacionDTO(clave, enTramite ? "REEMBOLSO_EN_TRAMITE" : "REEMBOLSO_REALIZADO",
                (enTramite ? "Devolución en trámite: " : "Devolución realizada: ") + vehiculoNom,
                texto + " Puede ver el detalle y su nota de crédito en Mis reservas.",
                fecha, id, leidas.contains(clave)));
    }

    /** Menor número = ocurrió después dentro de la vida de una reserva del cliente. */
    private static int ordenDelCicloCliente(NotificacionDTO n) {
        if (n == null || n.tipo() == null) return 9;
        return switch (n.tipo()) {
            case "RECOJO_CLIENTE" -> 0;
            case "PAGO_CONFIRMADO" -> 1;
            case "RESERVA_APROBADA" -> 2;
            case "SOLICITUD_PENDIENTE" -> 3;
            default -> 5;
        };
    }

    private static int prioridadNotificacion(NotificacionDTO n) {
        if (n == null || n.tipo() == null) return 5;
        return switch (n.tipo()) {
            case "DEVOLUCION_VENCIDA", "DEVOLUCION_1H", "DEVOLUCION_2H", "DEVOLUCION_HOY" -> 1;
            case "RECOJO_HOY", "NUEVA_RESERVA", "ALERTA_STOCK" -> 2;
            case "RESUMEN_EJECUTIVO", "MANTENIMIENTO_FLOTA" -> 3;
            default -> 4;
        };
    }

    private int calcularMinutosRestantes(String horaDevStr, LocalTime ahora) {
        int hRet = 18, mRet = 0;
        try {
            String[] partes = horaDevStr.split(":");
            hRet = Integer.parseInt(partes[0].trim());
            mRet = partes.length > 1 ? Integer.parseInt(partes[1].trim()) : 0;
        } catch (Exception ignored) {}
        return (hRet * 60 + mRet) - (ahora.getHour() * 60 + ahora.getMinute());
    }

    @Transactional
    public void marcarLeidas(String username, Collection<String> claves) {
        for (String clave : claves) {
            if (clave == null || clave.isBlank() || clave.length() > 60) continue;
            if (!leidaRepository.existsByUsernameAndClave(username, clave)) {
                leidaRepository.save(new NotificacionLeida(username, clave));
            }
        }
    }

    private Usuario buscarUsuario(String username) {
        if (username == null || username.isBlank()) return null;
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .or(() -> usuarioRepository.findByEmailRecuperacionIgnoreCase(username))
                .or(() -> usuarioRepository.findByCliente_EmailIgnoreCase(username))
                .orElse(null);
    }

    private String cliente(Alquiler a) {
        return a.getCliente() != null ? a.getCliente().getNombres() + " " + a.getCliente().getApellidos() : "Cliente";
    }

    private String vehiculo(Alquiler a) {
        if (a.getVehiculoModelo() != null) {
            String marca = (a.getVehiculoModelo().getMarca() != null && a.getVehiculoModelo().getMarca().getNombreMarca() != null)
                    ? a.getVehiculoModelo().getMarca().getNombreMarca() : "";
            String mod = a.getVehiculoModelo().getNombreModelo() != null ? a.getVehiculoModelo().getNombreModelo() : "";
            return (marca + " " + mod).trim();
        }
        return "un vehículo";
    }

    /** "DNI, licencia de conducir y dirección": solo lo que realmente falta. */
    private static String faltantes(boolean dni, boolean licencia, boolean direccion) {
        List<String> f = new ArrayList<>();
        if (dni) f.add("el DNI");
        if (licencia) f.add("la licencia de conducir");
        if (direccion) f.add("la dirección");
        if (f.size() <= 1) return String.join("", f);
        return String.join(", ", f.subList(0, f.size() - 1)) + " y " + f.get(f.size() - 1);
    }
}
