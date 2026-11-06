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

import com.example.alquiler_de_vehiculos.dto.NotificacionDTO;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.NotificacionLeida;
import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.ClienteRepository;
import com.example.alquiler_de_vehiculos.repository.NotificacionLeidaRepository;
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

    public NotificacionService(AlquilerRepository alquilerRepository,
                               NotificacionLeidaRepository leidaRepository,
                               UsuarioRepository usuarioRepository,
                               ClienteRepository clienteRepository,
                               VehiculoModeloRepository vehiculoModeloRepository,
                               VehiculoUnidadRepository vehiculoUnidadRepository) {
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

        boolean faltaDni = cliente.getNumeroDocumento() == null || cliente.getNumeroDocumento().isBlank() || cliente.getNumeroDocumento().startsWith("PEND-");
        boolean faltaLicencia = cliente.getLicenciaConducir() == null || cliente.getLicenciaConducir().isBlank();
        boolean faltaDireccion = cliente.getDireccion() == null || cliente.getDireccion().isBlank();

        if (faltaDni || faltaLicencia || faltaDireccion) {
            String clave = "CLI-PERFIL-INCOMPLETO-" + cliente.getIdCliente();
            lista.add(new NotificacionDTO(
                    clave,
                    "ACTUALIZA_PERFIL",
                    "Completa tus datos en Mi Perfil",
                    "Registra tu documento de identidad (DNI), licencia de conducir y dirección en Mi Perfil para poder solicitar reservas sin demoras.",
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
                lista.add(new NotificacionDTO(
                        claveAprobada,
                        "RESERVA_APROBADA",
                        "¡Reserva Aprobada!: " + vehiculoNom,
                        "Tu reserva fue aprobada para retiro el " + a.getFechaInicio() + " en sede " + sedeRec + ". Puedes completar tu pago con anticipación o abonar en counter.",
                        a.getFechaInicio() != null ? a.getFechaInicio().toString() : fechaReg,
                        a.getIdAlquiler(),
                        leidas.contains(claveAprobada)));
            } else if ("PAGADO".equals(estado) || "CONFIRMADO".equals(estado)) {
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
                            "¡Llaves entregadas! Tu contrato está activo. Devolución programada para el " + a.getFechaFinEstimada() + " en sede " + sedeDev + ".",
                            a.getFechaInicio() != null ? a.getFechaInicio().toString() : "En ruta",
                            a.getIdAlquiler(),
                            leidas.contains(claveRuta)));
                }
            } else if ("FINALIZADO".equals(estado)) {
                String claveFin = "CLI-FINALIZADO-" + a.getIdAlquiler();
                lista.add(new NotificacionDTO(
                        claveFin,
                        "ALQUILER_FINALIZADO",
                        "Alquiler Concluido: " + vehiculoNom,
                        "Tu alquiler para " + vehiculoNom + " finalizó conforme en sede " + sedeDev + ". Depósito de garantía liberado al 100%.",
                        a.getFechaFinReal() != null ? a.getFechaFinReal().toString() : (a.getFechaFinEstimada() != null ? a.getFechaFinEstimada().toString() : "Finalizado"),
                        a.getIdAlquiler(),
                        leidas.contains(claveFin)));
            } else if ("RECHAZADO".equals(estado)) {
                String claveRechazo = "CLI-RECHAZADO-" + a.getIdAlquiler();
                String mot = a.getMotivoRechazo() != null ? a.getMotivoRechazo() : "Validación de counter";
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

        lista.sort(Comparator.comparing(NotificacionDTO::leida)
                .thenComparing(NotificacionService::prioridadNotificacion)
                .thenComparing(NotificacionDTO::fecha, Comparator.nullsLast(Comparator.reverseOrder())));
        return lista;
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
}
