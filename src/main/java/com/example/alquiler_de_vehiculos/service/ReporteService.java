package com.example.alquiler_de_vehiculos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO.FilaMonto;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO.FilaVehiculo;
import com.example.alquiler_de_vehiculos.dto.ReporteOperativoDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO.FilaSolicitud;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO.FilaVehiculoSolicitado;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO.Resumen;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoModeloRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoUnidadRepository;
import com.example.alquiler_de_vehiculos.util.TarifarioPaulCars;

@Service
public class ReporteService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("RESERVADO", "PAGADO", "CONFIRMADO", "ACTIVO", "FINALIZADO");

    private final AlquilerRepository alquilerRepository;
    private final PagoRepository pagoRepository;
    private final VehiculoModeloRepository vehiculoModeloRepository;
    private final VehiculoUnidadRepository vehiculoUnidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final BoletaService boletaService;

    public ReporteService(AlquilerRepository alquilerRepository,
                          PagoRepository pagoRepository,
                          VehiculoModeloRepository vehiculoModeloRepository,
                          VehiculoUnidadRepository vehiculoUnidadRepository,
                          UsuarioRepository usuarioRepository,
                          BoletaService boletaService) {
        this.boletaService = boletaService;
        this.usuarioRepository = usuarioRepository;
        this.alquilerRepository = alquilerRepository;
        this.pagoRepository = pagoRepository;
        this.vehiculoModeloRepository = vehiculoModeloRepository;
        this.vehiculoUnidadRepository = vehiculoUnidadRepository;
    }

    /** Unidades de la flota para el selector "Vehículo" del resumen ejecutivo. */
    public List<ReporteEjecutivoDTO.UnidadOpcion> unidadesParaSelector() {
        return vehiculoUnidadRepository.findAll().stream()
                .map(u -> {
                    VehiculoModelo m = u.getVehiculoModelo();
                    String marca = m != null && m.getMarca() != null ? m.getMarca().getNombreMarca() : "";
                    String modelo = m != null ? m.getNombreModelo() : "";
                    return new ReporteEjecutivoDTO.UnidadOpcion(u.getPlaca(), (marca + " " + modelo).trim(),
                            m != null ? TarifarioPaulCars.categoria(m) : "SEDAN",
                            u.getSede() != null ? u.getSede() : "");
                })
                .sorted(Comparator.comparing(ReporteEjecutivoDTO.UnidadOpcion::nombre)
                        .thenComparing(ReporteEjecutivoDTO.UnidadOpcion::placa))
                .toList();
    }

    public ReporteEjecutivoDTO ejecutivo(LocalDate desde, LocalDate hasta, String sede, String categoria, String modelo, String placa) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        if (fin.isBefore(inicio)) {
            LocalDate tmp = inicio;
            inicio = fin;
            fin = tmp;
        }
        final LocalDate rangoInicio = inicio;
        final LocalDate rangoFin = fin;
        String sedeFiltro = normalizar(sede);
        String categoriaFiltro = normalizar(categoria);
        String placaFiltro = placa == null ? "" : placa.trim().toUpperCase();
        String modeloFiltro = modelo == null ? "" : modelo.trim();
        long diasRango = Math.max(1, ChronoUnit.DAYS.between(rangoInicio, rangoFin) + 1);

        List<VehiculoUnidad> flotaUnidades = vehiculoUnidadRepository.findAll().stream()
                .filter(u -> coincideUnidad(u, sedeFiltro, categoriaFiltro))
                .filter(u -> placaFiltro.isEmpty() || placaFiltro.equalsIgnoreCase(u.getPlaca()))
                .filter(u -> modeloFiltro.isEmpty() || modeloFiltro.equalsIgnoreCase(nombreModelo(u.getVehiculoModelo())))
                .toList();
        List<VehiculoModelo> flotaModelos = vehiculoModeloRepository.findAll().stream()
                .filter(m -> coincideModelo(m, sedeFiltro, categoriaFiltro))
                .toList();

        List<Alquiler> todos = alquilerRepository.findAll().stream()
                .filter(a -> coincideAlquiler(a, sedeFiltro, categoriaFiltro))
                .filter(a -> placaFiltro.isEmpty()
                        || (a.getVehiculoUnidad() != null && placaFiltro.equalsIgnoreCase(a.getVehiculoUnidad().getPlaca())))
                .filter(a -> modeloFiltro.isEmpty() || modeloFiltro.equalsIgnoreCase(nombreModelo(a.getVehiculoModelo())))
                .toList();
        List<Alquiler> validosEnRango = todos.stream()
                .filter(a -> ESTADOS_VALIDOS.contains(estado(a)))
                .filter(a -> diasSolapados(a, rangoInicio, rangoFin) > 0)
                .toList();

        List<Pago> pagos = pagoRepository.findByFechaPagoBetween(rangoInicio, rangoFin).stream()
                .filter(p -> p.getAlquiler() != null && coincideAlquiler(p.getAlquiler(), sedeFiltro, categoriaFiltro))
                .filter(p -> placaFiltro.isEmpty()
                        || (p.getAlquiler().getVehiculoUnidad() != null
                                && placaFiltro.equalsIgnoreCase(p.getAlquiler().getVehiculoUnidad().getPlaca())))
                .filter(p -> modeloFiltro.isEmpty() || modeloFiltro.equalsIgnoreCase(nombreModelo(p.getAlquiler().getVehiculoModelo())))
                .toList();
        BigDecimal ingresos = suma(pagos.stream().map(Pago::getMontoTotal).toList());

        // Pagos pendientes: reservas confirmadas que aún no se han pagado completo
        List<Alquiler> pendientesDePago = todos.stream()
                .filter(a -> "RESERVADO".equals(estado(a)))
                .filter(a -> totalContrato(a).subtract(suma(pagoRepository.findByAlquiler_IdAlquiler(a.getIdAlquiler()).stream()
                        .map(Pago::getMontoTotal).toList())).signum() > 0)
                .toList();
        BigDecimal montoPendiente = pendientesDePago.stream()
                .map(a -> totalContrato(a).subtract(suma(pagoRepository.findByAlquiler_IdAlquiler(a.getIdAlquiler()).stream()
                        .map(Pago::getMontoTotal).toList())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        long diasAlquilados = validosEnRango.stream().mapToLong(a -> diasSolapados(a, rangoInicio, rangoFin)).sum();
        long totalUnidades = !flotaUnidades.isEmpty() ? flotaUnidades.size() : Math.max(1, flotaModelos.size());
        BigDecimal capacidad = BigDecimal.valueOf(Math.max(1, totalUnidades) * diasRango);
        BigDecimal ocupacion = BigDecimal.valueOf(diasAlquilados).multiply(BigDecimal.valueOf(100))
                .divide(capacidad, 2, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100));

        List<Alquiler> iniciadosEnRango = validosEnRango.stream()
                .filter(a -> a.getFechaInicio() != null && !a.getFechaInicio().isBefore(rangoInicio) && !a.getFechaInicio().isAfter(rangoFin))
                .toList();
        BigDecimal ticket = iniciadosEnRango.isEmpty()
                ? BigDecimal.ZERO
                : suma(iniciadosEnRango.stream().map(this::totalContrato).toList())
                        .divide(BigDecimal.valueOf(iniciadosEnRango.size()), 2, RoundingMode.HALF_UP);

        long finalizadosEnRango = validosEnRango.stream()
                .filter(a -> "FINALIZADO".equals(estado(a)))
                .count();

        List<FilaVehiculo> top = topVehiculos(flotaUnidades, flotaModelos, validosEnRango, pagos, rangoInicio, rangoFin, diasRango);

        Map<String, List<Pago>> porSede = agrupar(pagos, p -> sedeDe(p.getAlquiler()));
        Map<String, List<Pago>> porCategoria = agrupar(pagos, p -> categoriaDe(p.getAlquiler()));
        Map<String, List<Pago>> porMetodo = agrupar(pagos,
                p -> p.getMetodoPago() != null ? p.getMetodoPago().getNombreMetodo() : "SIN MÉTODO");

        return new ReporteEjecutivoDTO(
                rangoInicio, rangoFin,
                sedeFiltro.isEmpty() ? "TODAS" : sedeFiltro,
                categoriaFiltro.isEmpty() ? "TODAS" : categoriaFiltro,
                ingresos, ocupacion, ticket,
                iniciadosEnRango.size(),
                finalizadosEnRango,
                diasAlquilados,
                pendientesDePago.size(),
                montoPendiente,
                top,
                filas(porSede), filas(porCategoria), filas(porMetodo),
                !placaFiltro.isEmpty() ? etiquetaVehiculo(placaFiltro) : (modeloFiltro.isEmpty() ? "TODOS" : modeloFiltro),
                placaFiltro.isEmpty() && modeloFiltro.isEmpty() ? List.of() : alquileresDelVehiculo(todos, rangoInicio, rangoFin));
    }

    /** "Marca Modelo", por ejemplo "Ford Ranger". */
    private String nombreModelo(VehiculoModelo m) {
        if (m == null) return "";
        String marca = m.getMarca() != null && m.getMarca().getNombreMarca() != null ? m.getMarca().getNombreMarca() : "";
        return (marca + " " + (m.getNombreModelo() != null ? m.getNombreModelo() : "")).trim();
    }

    private String etiquetaVehiculo(String placa) {
        return vehiculoUnidadRepository.findByPlacaIgnoreCase(placa)
                .map(u -> {
                    VehiculoModelo m = u.getVehiculoModelo();
                    String marca = m != null && m.getMarca() != null ? m.getMarca().getNombreMarca() : "";
                    String modelo = m != null ? m.getNombreModelo() : "";
                    return (marca + " " + modelo).trim() + " (" + u.getPlaca() + ")";
                })
                .orElse(placa);
    }

    /** Alquileres de un vehículo que se cruzan con el periodo, con su estado y lo pagado. */
    private List<ReporteEjecutivoDTO.FilaAlquilerVehiculo> alquileresDelVehiculo(List<Alquiler> alquileres,
                                                                                LocalDate inicio, LocalDate fin) {
        return alquileres.stream()
                .filter(a -> a.getFechaInicio() != null && a.getFechaFinEstimada() != null
                        && !a.getFechaInicio().isAfter(fin) && !a.getFechaFinEstimada().isBefore(inicio))
                .sorted(Comparator.comparing(Alquiler::getFechaInicio).reversed())
                .map(a -> {
                    String cliente = a.getCliente() != null
                            ? ((a.getCliente().getNombres() != null ? a.getCliente().getNombres() : "") + " "
                            + (a.getCliente().getApellidos() != null ? a.getCliente().getApellidos() : "")).trim() : "-";
                    BigDecimal pagado = suma(pagoRepository.findByAlquiler_IdAlquiler(a.getIdAlquiler()).stream()
                            .filter(p -> p.getTipoPago() == null || "ALQUILER".equalsIgnoreCase(p.getTipoPago()))
                            .map(Pago::getMontoTotal).toList());
                    return new ReporteEjecutivoDTO.FilaAlquilerVehiculo(
                            String.format("ALQ-%06d", a.getIdAlquiler()), cliente,
                            a.getFechaInicio(), a.getFechaFinEstimada(),
                            a.getDias() != null ? a.getDias() : 1, estado(a),
                            totalContrato(a).setScale(2, RoundingMode.HALF_UP), pagado);
                })
                .toList();
    }

    public ReporteOperativoDTO operativoDelDia() {
        LocalDate hoy = LocalDate.now();
        List<Alquiler> todos = alquilerRepository.findAll();
        List<VehiculoUnidad> flota = vehiculoUnidadRepository.findAll();
        List<Pago> pagosHoy = pagoRepository.findByFechaPagoBetween(hoy, hoy);

        long pendientes = todos.stream().filter(a -> "PENDIENTE".equals(estado(a))).count();
        long recojos = todos.stream()
                .filter(a -> ("RESERVADO".equals(estado(a)) || "PAGADO".equals(estado(a))) && hoy.equals(a.getFechaInicio())).count();
        long devoluciones = todos.stream()
                .filter(a -> "ACTIVO".equals(estado(a)) && hoy.equals(a.getFechaFinEstimada())).count();
        long vencidas = todos.stream()
                .filter(a -> "ACTIVO".equals(estado(a)) && a.getFechaFinEstimada().isBefore(hoy)).count();

        return new ReporteOperativoDTO(
                pendientes, recojos, devoluciones, vencidas,
                pagosHoy.size(),
                suma(pagosHoy.stream().map(Pago::getMontoTotal).toList()),
                contarEstadoUnidad(flota, "DISPONIBLE"),
                contarEstadoUnidad(flota, "MANTENIMIENTO"),
                contarEstadoUnidad(flota, "LIMPIEZA"));
    }


    // ───────────────────── Reporte de solicitudes y alquileres ─────────────────────

    /**
     * Detalle de las solicitudes/alquileres REGISTRADOS en el rango (por defecto, el día de hoy),
     * con su estado, vehículo, situación de pago y fechas de auditoría.
     */
    private static String filtrosExtra(String metodo, String asesor, String modelo, String placa) {
        StringBuilder sb = new StringBuilder();
        if (metodo != null) sb.append("   |   Método de pago: ").append(metodo);
        if (asesor != null) sb.append("   |   Atendido por: ").append(asesor);
        if (modelo != null && !modelo.isBlank()) sb.append("   |   Vehículo: ").append(modelo);
        if (placa != null && !placa.isBlank()) sb.append("   |   Placa: ").append(placa);
        return sb.toString();
    }

    public ReporteSolicitudesDTO solicitudes(LocalDate desde, LocalDate hasta, String estado,
                                             String sede, String categoria, String busqueda) {
        return solicitudes(desde, hasta, estado, sede, categoria, busqueda, null, null, null, null);
    }

    public ReporteSolicitudesDTO solicitudes(LocalDate desde, LocalDate hasta, String estado,
                                             String sede, String categoria, String busqueda,
                                             String metodo, String asesor, String modelo, String placaFiltroIn) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = desde != null ? desde : hoy;
        LocalDate fin = hasta != null ? hasta : inicio;
        if (fin.isBefore(inicio)) {
            LocalDate tmp = inicio;
            inicio = fin;
            fin = tmp;
        }
        final LocalDate rangoInicio = inicio;
        final LocalDate rangoFin = fin;
        String grupoFiltro = normalizar(estado);
        String sedeFiltro = normalizar(sede);
        String categoriaFiltro = normalizar(categoria);
        String texto = busqueda == null ? "" : busqueda.trim().toLowerCase();
        String metodoFiltro = metodo == null ? "" : metodo.trim().toLowerCase();
        String asesorFiltro = asesor == null ? "" : asesor.trim().toLowerCase();
        String modeloFiltro = modelo == null ? "" : modelo.trim();
        String placaPedida = placaFiltroIn == null ? "" : placaFiltroIn.trim();

        Map<Integer, List<Pago>> pagosPorAlquiler = new LinkedHashMap<>();
        for (Pago p : pagoRepository.findAll()) {
            if (p.getAlquiler() == null || p.getAlquiler().getIdAlquiler() == null) continue;
            if (p.getTipoPago() != null && !"ALQUILER".equalsIgnoreCase(p.getTipoPago()) && !"REEMBOLSO".equalsIgnoreCase(p.getTipoPago())) continue;
            pagosPorAlquiler.computeIfAbsent(p.getAlquiler().getIdAlquiler(), k -> new ArrayList<>()).add(p);
        }

        List<FilaSolicitud> filas = new ArrayList<>();
        for (Alquiler a : alquilerRepository.findAll()) {
            LocalDateTime registro = a.getFechaRegistro();
            if (registro == null) continue;
            LocalDate diaRegistro = registro.toLocalDate();
            if (diaRegistro.isBefore(rangoInicio) || diaRegistro.isAfter(rangoFin)) continue;
            if (!coincideAlquiler(a, sedeFiltro, categoriaFiltro)) continue;

            List<Pago> pagos = pagosPorAlquiler.getOrDefault(a.getIdAlquiler(), List.of());
            BigDecimal total = totalContrato(a).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pagado = suma(pagos.stream().map(Pago::getMontoTotal).toList());
            BigDecimal saldo = total.subtract(pagado).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            boolean pagadoTotal = pagado.signum() > 0 && saldo.signum() == 0;

            String grupo = grupoEstado(estado(a), pagadoTotal);
            if (!grupoFiltro.isEmpty()) {
                boolean coincideGrupo = "CON_PAGO".equals(grupoFiltro)
                        ? java.util.Set.of("PAGADO", "EN_RUTA", "FINALIZADO").contains(grupo) : grupoFiltro.equals(grupo);
                if (!coincideGrupo) continue;
            }
            // Una solicitud pendiente o anulada todavía no genera deuda: no se muestra saldo por cobrar
            if ("ANULADO".equals(grupo) || "PENDIENTE".equals(grupo)) {
                saldo = BigDecimal.ZERO.setScale(2);
            }

            String cliente = a.getCliente() != null
                    ? ((a.getCliente().getNombres() != null ? a.getCliente().getNombres() : "") + " "
                    + (a.getCliente().getApellidos() != null ? a.getCliente().getApellidos() : "")).trim()
                    : "-";
            String documento = a.getCliente() != null && a.getCliente().getNumeroDocumento() != null
                    ? a.getCliente().getNumeroDocumento() : "-";
            VehiculoModelo m = a.getVehiculoModelo();
            String marca = m != null && m.getMarca() != null ? m.getMarca().getNombreMarca() : "";
            String vehiculo = m != null ? (marca + " " + m.getNombreModelo()).trim() : "-";
            String placa = a.getVehiculoUnidad() != null ? a.getVehiculoUnidad().getPlaca() : "";
            String codigo = String.format("ALQ-%06d", a.getIdAlquiler());

            if (!modeloFiltro.isEmpty() && !modeloFiltro.equalsIgnoreCase(vehiculo)) continue;
            if (!placaPedida.isEmpty() && !placaPedida.equalsIgnoreCase(placa)) continue;

            if (!texto.isEmpty()) {
                String pajar = (cliente + " " + documento + " " + vehiculo + " " + placa + " " + codigo).toLowerCase();
                if (!pajar.contains(texto)) continue;
            }

            // Filtro por método de pago: algún pago del contrato se hizo con ese método
            if (!metodoFiltro.isEmpty() && pagos.stream().noneMatch(p -> p.getMetodoPago() != null
                    && p.getMetodoPago().getNombreMetodo() != null
                    && p.getMetodoPago().getNombreMetodo().toLowerCase().contains(metodoFiltro))) continue;

            // Filtro por asesor: entregó las llaves, cobró o canceló
            String cobrador = pagos.stream().map(Pago::getRegistradoPor).filter(x -> x != null && !x.isBlank()).findFirst().orElse(null);
            String atendido = a.getEntregadoPor() != null && !a.getEntregadoPor().isBlank() ? a.getEntregadoPor() : cobrador;
            if (!asesorFiltro.isEmpty()) {
                boolean coincide = (a.getEntregadoPor() != null && a.getEntregadoPor().equalsIgnoreCase(asesor.trim()))
                        || (a.getCanceladoPor() != null && a.getCanceladoPor().equalsIgnoreCase(asesor.trim()))
                        || pagos.stream().anyMatch(p -> p.getRegistradoPor() != null && p.getRegistradoPor().equalsIgnoreCase(asesor.trim()));
                if (!coincide) continue;
            }

            String metodos = metodosLegibles(pagos);

            filas.add(new FilaSolicitud(
                    a.getIdAlquiler(), codigo, registro, a.getFechaUltimoEstado(),
                    cliente, documento, vehiculo, placa, categoriaDe(a), sedeDe(a),
                    a.getFechaInicio(), a.getHoraRecojo(), a.getFechaFinEstimada(), a.getHoraDevolucion(),
                    a.getDias() != null ? a.getDias() : 1,
                    grupo, etiquetaGrupo(grupo), estadoPago(grupo, pagado, saldo),
                    metodos.isEmpty() ? "-" : metodos,
                    total, pagado, saldo, a.getFechaEntrega(), a.getEntregadoPor(),
                    a.getFechaCheckout(), a.getKmEntrega(), a.getCombustibleEntrega(),
                    diferenciaMinutos(a.getFechaInicio(), a.getHoraRecojo(), a.getFechaEntrega()),
                    diferenciaMinutos(a.getFechaFinEstimada(), a.getHoraDevolucion(), a.getFechaCheckout()),
                    atendido != null ? atendido : "-"));
        }
        filas.sort(Comparator.comparing(FilaSolicitud::fechaRegistro).reversed());

        long pend = contar(filas, "PENDIENTE");
        long porPagar = contar(filas, "POR_PAGAR");
        long pagadas = contar(filas, "PAGADO");
        long enRuta = contar(filas, "EN_RUTA");
        long finalizadas = contar(filas, "FINALIZADO");
        long anuladas = contar(filas, "ANULADO");

        // Los contratos anulados no suman al monto contratado ni al saldo por cobrar
        List<FilaSolicitud> vigentes = filas.stream().filter(f -> !"ANULADO".equals(f.grupoEstado())).toList();
        Resumen resumen = new Resumen(filas.size(), pend, porPagar, pagadas, enRuta, finalizadas, anuladas,
                suma(vigentes.stream().map(FilaSolicitud::montoTotal).toList()),
                suma(filas.stream().map(FilaSolicitud::montoPagado).toList()),
                suma(vigentes.stream().filter(f -> !"PENDIENTE".equals(f.grupoEstado())).map(FilaSolicitud::saldo).toList()));

        Map<String, List<FilaSolicitud>> porEstado = new LinkedHashMap<>();
        Map<String, List<FilaSolicitud>> porSede = new LinkedHashMap<>();
        Map<String, List<FilaSolicitud>> porVehiculo = new LinkedHashMap<>();
        for (FilaSolicitud f : filas) {
            porEstado.computeIfAbsent(f.estado(), k -> new ArrayList<>()).add(f);
            porSede.computeIfAbsent(f.sede(), k -> new ArrayList<>()).add(f);
            porVehiculo.computeIfAbsent(f.vehiculo(), k -> new ArrayList<>()).add(f);
        }

        Map<String, long[]> metodoCont = new LinkedHashMap<>();
        Map<String, BigDecimal> metodoMonto = new LinkedHashMap<>();
        for (FilaSolicitud f : filas) {
            for (Pago p : pagosPorAlquiler.getOrDefault(f.idAlquiler(), List.of())) {
                String nombre = p.getMetodoPago() != null ? p.getMetodoPago().getNombreMetodo() : "SIN MÉTODO";
                metodoCont.computeIfAbsent(nombre, k -> new long[1])[0]++;
                metodoMonto.merge(nombre, p.getMontoTotal() != null ? p.getMontoTotal() : BigDecimal.ZERO, BigDecimal::add);
            }
        }
        List<FilaMonto> filasMetodo = metodoCont.entrySet().stream()
                .map(e -> new FilaMonto(e.getKey(), e.getValue()[0], metodoMonto.get(e.getKey()).setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(FilaMonto::total).reversed()).toList();

        List<FilaVehiculoSolicitado> vehiculos = porVehiculo.entrySet().stream()
                .map(e -> new FilaVehiculoSolicitado(e.getKey(), e.getValue().get(0).categoria(), e.getValue().size(),
                        suma(e.getValue().stream().map(FilaSolicitud::montoTotal).toList())))
                .sorted(Comparator.comparingLong(FilaVehiculoSolicitado::solicitudes).reversed()
                        .thenComparing(Comparator.comparing(FilaVehiculoSolicitado::monto).reversed()))
                .toList();

        return new ReporteSolicitudesDTO(
                rangoInicio, rangoFin,
                grupoFiltro.isEmpty() ? "TODOS" : grupoFiltro,
                sedeFiltro.isEmpty() ? "TODAS" : sedeFiltro,
                categoriaFiltro.isEmpty() ? "TODAS" : categoriaFiltro,
                texto,
                filtrosExtra(metodoFiltro.isEmpty() ? null : metodo.trim(), asesorFiltro.isEmpty() ? null : asesor.trim(),
                        modeloFiltro, placaPedida),
                LocalDateTime.now(), usuarioActual(),
                resumen, filas,
                resumirPorClave(porEstado),
                filasMetodo,
                resumirPorClave(porSede),
                vehiculos,
                detallePagos(filas, pagosPorAlquiler));
    }

    /** "Yape", "Yape (2 pagos)" o "Yape + Efectivo" en lugar de volcar el texto crudo de cada pago. */
    private String metodosLegibles(List<Pago> pagos) {
        if (pagos == null || pagos.isEmpty()) return "-";
        Map<String, Long> porMetodo = new LinkedHashMap<>();
        for (Pago p : pagos) {
            String nombre = p.getMetodoPago() != null ? p.getMetodoPago().getNombreMetodo() : "";
            if (nombre == null || nombre.isBlank()) continue;
            porMetodo.merge(nombre, 1L, Long::sum);
        }
        if (porMetodo.isEmpty()) return "-";
        if (porMetodo.size() == 1) {
            var e = porMetodo.entrySet().iterator().next();
            return e.getValue() > 1 ? e.getKey() + " (" + e.getValue() + " pagos)" : e.getKey();
        }
        return String.join(" + ", porMetodo.keySet());
    }

    /** Minutos entre la hora pactada (fecha + HH:mm) y la hora real; positivo = tarde. Nulo si falta algún dato. */
    private Long diferenciaMinutos(LocalDate fechaPactada, String horaPactada, LocalDateTime real) {
        if (real == null || fechaPactada == null || horaPactada == null || horaPactada.isBlank()) return null;
        try {
            LocalDateTime pactada = fechaPactada.atTime(java.time.LocalTime.parse(horaPactada.trim()));
            return ChronoUnit.MINUTES.between(pactada, real);
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    private List<ReporteSolicitudesDTO.FilaPago> detallePagos(List<FilaSolicitud> filas, Map<Integer, List<Pago>> pagosPorAlquiler) {
        List<ReporteSolicitudesDTO.FilaPago> detalle = new ArrayList<>();
        for (FilaSolicitud f : filas) {
            for (Pago p : pagosPorAlquiler.getOrDefault(f.idAlquiler(), List.of())) {
                detalle.add(new ReporteSolicitudesDTO.FilaPago(
                        f.codigo(),
                        p.getIdPago() != null ? String.format("REC-%06d", p.getIdPago()) : "-",
                        p.getFechaHoraPago(),
                        f.cliente(),
                        p.getMetodoPago() != null ? p.getMetodoPago().getNombreMetodo() : "-",
                        p.getCanal() != null ? p.getCanal() : "-",
                        p.getNumeroOperacion() != null ? p.getNumeroOperacion() : "-",
                        p.getMontoTotal() != null ? p.getMontoTotal() : BigDecimal.ZERO));
            }
        }
        detalle.sort(Comparator.comparing(ReporteSolicitudesDTO.FilaPago::fechaHora,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return detalle;
    }

    private List<FilaMonto> resumirPorClave(Map<String, List<FilaSolicitud>> mapa) {
        return mapa.entrySet().stream()
                .map(e -> new FilaMonto(e.getKey(), e.getValue().size(),
                        suma(e.getValue().stream().map(FilaSolicitud::montoTotal).toList())))
                .sorted(Comparator.comparingLong(FilaMonto::cantidad).reversed())
                .toList();
    }

    private long contar(List<FilaSolicitud> filas, String grupo) {
        return filas.stream().filter(f -> grupo.equals(f.grupoEstado())).count();
    }

    private String grupoEstado(String estado, boolean pagadoTotal) {
        return switch (estado) {
            case "PENDIENTE" -> "PENDIENTE";
            case "ACTIVO", "EN_CURSO", "EN CURSO" -> "EN_RUTA";
            case "FINALIZADO", "CERRADO_CON_DEUDA" -> "FINALIZADO";
            case "CANCELADO", "RECHAZADO", "RECHAZADO_DOCUMENTACION", "CANCELADO_SIN_STOCK" -> "ANULADO";
            default -> pagadoTotal ? "PAGADO" : "POR_PAGAR";
        };
    }

    private String etiquetaGrupo(String grupo) {
        return switch (grupo) {
            case "PENDIENTE" -> "Pendiente";
            case "POR_PAGAR" -> "Reservado - Por pagar";
            case "PAGADO" -> "Reservado - Pagado";
            case "EN_RUTA" -> "En ruta";
            case "FINALIZADO" -> "Finalizado";
            default -> "Anulado";
        };
    }

    private String estadoPago(String grupo, BigDecimal pagado, BigDecimal saldo) {
        if ("ANULADO".equals(grupo)) return "Anulado";
        if ("PENDIENTE".equals(grupo)) return "Sin cobro";
        if (pagado.signum() > 0 && saldo.signum() == 0) return "Pagado";
        if (pagado.signum() > 0) return "Pago parcial";
        return "Por pagar";
    }

    // ───────────────────── Cierre de caja ─────────────────────

    /** Lo cobrado en un día por asesor y método de pago, con el detalle de cada cobro. */
    public com.example.alquiler_de_vehiculos.dto.CierreCajaDTO cierreCaja(LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        List<Pago> pagos = new ArrayList<>(pagoRepository.findByFechaPagoBetween(dia, dia));
        pagos.sort(Comparator.comparing(Pago::getFechaHoraPago, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, BigDecimal[]> acum = new LinkedHashMap<>();
        Map<String, long[]> cant = new LinkedHashMap<>();
        List<com.example.alquiler_de_vehiculos.dto.CierreCajaDTO.Detalle> detalle = new ArrayList<>();
        BigDecimal[] total = nuevoAcumulador();
        long totalCant = 0;

        for (Pago p : pagos) {
            String asesor = p.getRegistradoPor() != null && !p.getRegistradoPor().isBlank() ? p.getRegistradoPor()
                    : ("DIGITAL".equalsIgnoreCase(p.getCanal()) ? "Pago en línea" : "Sin asesor registrado");
            String metodo = p.getMetodoPago() != null && p.getMetodoPago().getNombreMetodo() != null
                    ? p.getMetodoPago().getNombreMetodo().toUpperCase() : "";
            BigDecimal monto = p.getMontoTotal() != null ? p.getMontoTotal() : BigDecimal.ZERO;
            int col = metodo.contains("EFECTIVO") ? 0 : metodo.contains("TARJETA") ? 1
                    : (metodo.contains("YAPE") || metodo.contains("PLIN")) ? 2 : 3;

            BigDecimal[] fila = acum.computeIfAbsent(asesor, k -> nuevoAcumulador());
            fila[col] = fila[col].add(monto);
            fila[4] = fila[4].add(monto);
            total[col] = total[col].add(monto);
            total[4] = total[4].add(monto);
            cant.computeIfAbsent(asesor, k -> new long[1])[0]++;
            totalCant++;

            Alquiler a = p.getAlquiler();
            String cliente = a != null && a.getCliente() != null
                    ? ((a.getCliente().getNombres() != null ? a.getCliente().getNombres() : "") + " "
                    + (a.getCliente().getApellidos() != null ? a.getCliente().getApellidos() : "")).trim() : "-";
            String boleta = boletaService.buscar(p.getIdPago()).map(b -> b.numero())
                    .orElse(p.getIdPago() != null ? String.format("REC-%06d", p.getIdPago()) : "-");
            detalle.add(new com.example.alquiler_de_vehiculos.dto.CierreCajaDTO.Detalle(
                    p.getFechaHoraPago(), boleta,
                    a != null && a.getIdAlquiler() != null ? String.format("ALQ-%06d", a.getIdAlquiler()) : "-",
                    cliente, (metodo.isEmpty() ? "-" : metodo) + ("REEMBOLSO".equalsIgnoreCase(p.getTipoPago()) ? " (DEVOLUCIÓN)" : ""),
                    p.getNumeroOperacion() != null ? p.getNumeroOperacion() : "-",
                    p.getNumeroAutorizacion() != null ? p.getNumeroAutorizacion() : "-",
                    asesor, monto));
        }

        List<com.example.alquiler_de_vehiculos.dto.CierreCajaDTO.FilaAsesor> filas = new ArrayList<>();
        acum.forEach((asesor, v) -> filas.add(filaAsesor(asesor, cant.get(asesor)[0], v)));
        return new com.example.alquiler_de_vehiculos.dto.CierreCajaDTO(dia, LocalDateTime.now(), usuarioActual(),
                filas, filaAsesor("TOTAL", totalCant, total), detalle);
    }

    private BigDecimal[] nuevoAcumulador() {
        BigDecimal[] v = new BigDecimal[5];
        java.util.Arrays.fill(v, BigDecimal.ZERO);
        return v;
    }

    private com.example.alquiler_de_vehiculos.dto.CierreCajaDTO.FilaAsesor filaAsesor(String asesor, long cantidad, BigDecimal[] v) {
        return new com.example.alquiler_de_vehiculos.dto.CierreCajaDTO.FilaAsesor(asesor, cantidad,
                v[0].setScale(2, RoundingMode.HALF_UP), v[1].setScale(2, RoundingMode.HALF_UP),
                v[2].setScale(2, RoundingMode.HALF_UP), v[3].setScale(2, RoundingMode.HALF_UP),
                v[4].setScale(2, RoundingMode.HALF_UP));
    }

    public String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return "Sistema";
        return usuarioRepository.findByUsernameIgnoreCase(auth.getName())
                .map(u -> u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank() ? u.getNombreCompleto() : u.getUsername())
                .orElse(auth.getName());
    }

    private List<FilaVehiculo> topVehiculos(List<VehiculoUnidad> unidades, List<VehiculoModelo> modelos,
                                            List<Alquiler> alquileres, List<Pago> pagos,
                                            LocalDate inicio, LocalDate fin, long diasRango) {
        List<FilaVehiculo> filas = new ArrayList<>();

        if (!unidades.isEmpty()) {
            for (VehiculoUnidad u : unidades) {
                VehiculoModelo m = u.getVehiculoModelo();
                String marca = m != null && m.getMarca() != null ? m.getMarca().getNombreMarca() : "";
                String modelo = m != null ? m.getNombreModelo() : "";
                String nombreCompleto = (marca + " " + modelo).trim();
                String categoria = m != null ? TarifarioPaulCars.categoria(m) : "SEDAN";

                // Solo lo que se alquiló con esta placa: nada se reparte entre placas del mismo modelo
                List<Alquiler> suyos = alquileres.stream()
                        .filter(a -> a.getVehiculoUnidad() != null && u.getIdUnidad().equals(a.getVehiculoUnidad().getIdUnidad()))
                        .toList();

                BigDecimal ingresos = suma(pagos.stream()
                        .filter(p -> p.getAlquiler() != null && p.getAlquiler().getVehiculoUnidad() != null
                                && u.getIdUnidad().equals(p.getAlquiler().getVehiculoUnidad().getIdUnidad()))
                        .map(Pago::getMontoTotal).toList());

                long dias = suyos.stream().mapToLong(a -> diasSolapados(a, inicio, fin)).sum();
                BigDecimal utilizacion = diasRango > 0
                        ? BigDecimal.valueOf(Math.min(dias, diasRango)).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(diasRango), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                filas.add(new FilaVehiculo(u.getIdUnidad(), nombreCompleto, u.getPlaca(),
                        categoria, suyos.size(), dias, ingresos, utilizacion));
            }

            // Reservas que ya no tienen placa (canceladas) o aún no la reciben: su dinero se muestra aparte, por modelo
            if (unidades.size() > 1) {
                Set<Integer> idsModelos = new java.util.LinkedHashSet<>();
                for (VehiculoUnidad u : unidades) {
                    if (u.getVehiculoModelo() != null) idsModelos.add(u.getVehiculoModelo().getIdModelo());
                }
                for (Integer idModelo : idsModelos) {
                    List<Alquiler> sinPlaca = alquileres.stream()
                            .filter(a -> a.getVehiculoUnidad() == null && a.getVehiculoModelo() != null
                                    && idModelo.equals(a.getVehiculoModelo().getIdModelo()))
                            .toList();
                    BigDecimal ingresos = suma(pagos.stream()
                            .filter(p -> p.getAlquiler() != null && p.getAlquiler().getVehiculoUnidad() == null
                                    && p.getAlquiler().getVehiculoModelo() != null
                                    && idModelo.equals(p.getAlquiler().getVehiculoModelo().getIdModelo()))
                            .map(Pago::getMontoTotal).toList());
                    if (sinPlaca.isEmpty() && ingresos.signum() == 0) continue;
                    VehiculoModelo m = unidades.stream().map(VehiculoUnidad::getVehiculoModelo)
                            .filter(x -> x != null && idModelo.equals(x.getIdModelo())).findFirst().orElse(null);
                    String marca = m != null && m.getMarca() != null ? m.getMarca().getNombreMarca() : "";
                    String nombre = (marca + " " + (m != null ? m.getNombreModelo() : "")).trim();
                    filas.add(new FilaVehiculo(idModelo, nombre, "Sin placa (canceladas)",
                            m != null ? TarifarioPaulCars.categoria(m) : "SEDAN", sinPlaca.size(), 0L, ingresos, BigDecimal.ZERO));
                }
            }
        } else {
            for (VehiculoModelo m : modelos) {
                String marca = m.getMarca() != null ? m.getMarca().getNombreMarca() : "";
                String nombreCompleto = (marca + " " + m.getNombreModelo()).trim();
                String categoria = TarifarioPaulCars.categoria(m);

                List<Alquiler> suyos = alquileres.stream()
                        .filter(a -> a.getVehiculoModelo() != null && m.getIdModelo().equals(a.getVehiculoModelo().getIdModelo()))
                        .toList();

                BigDecimal ingresos = suma(pagos.stream()
                        .filter(p -> p.getAlquiler() != null && p.getAlquiler().getVehiculoModelo() != null
                                && m.getIdModelo().equals(p.getAlquiler().getVehiculoModelo().getIdModelo()))
                        .map(Pago::getMontoTotal).toList());

                long dias = suyos.stream().mapToLong(a -> diasSolapados(a, inicio, fin)).sum();
                BigDecimal utilizacion = diasRango > 0
                        ? BigDecimal.valueOf(Math.min(dias, diasRango)).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(diasRango), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                filas.add(new FilaVehiculo(m.getIdModelo(), nombreCompleto, "FLOTA",
                        categoria, suyos.size(), dias, ingresos, utilizacion));
            }
        }

        return filas.stream()
                .sorted(Comparator.comparing(FilaVehiculo::ingresos).reversed()
                        .thenComparing(Comparator.comparingLong(FilaVehiculo::diasAlquilados).reversed()))
                .limit(10)
                .toList();
    }

    private long diasSolapados(Alquiler a, LocalDate inicio, LocalDate fin) {
        if (a.getFechaInicio() == null || a.getFechaFinEstimada() == null) return 0;
        LocalDate finAlquiler = a.getFechaFinReal() != null ? a.getFechaFinReal() : a.getFechaFinEstimada();
        LocalDate desde = a.getFechaInicio().isAfter(inicio) ? a.getFechaInicio() : inicio;
        LocalDate hasta = finAlquiler.isBefore(fin) ? finAlquiler : fin;
        if (hasta.isBefore(desde)) return 0;
        long dias = ChronoUnit.DAYS.between(desde, hasta);
        return Math.max(1, dias);
    }

    private BigDecimal totalContrato(Alquiler a) {
        if (a.getPrecioTotal() != null && a.getPrecioTotal() > 0) {
            return BigDecimal.valueOf(a.getPrecioTotal());
        }
        int dias = a.getDias() != null && a.getDias() > 0 ? a.getDias() : 1;
        BigDecimal precioDia = (a.getVehiculoModelo() != null && a.getVehiculoModelo().getPrecioDia() != null)
                ? a.getVehiculoModelo().getPrecioDia()
                : BigDecimal.ZERO;
        return precioDia.multiply(BigDecimal.valueOf(dias));
    }

    private boolean coincideUnidad(VehiculoUnidad u, String sede, String categoria) {
        String sedeUnidad = u.getSede() != null ? u.getSede().toUpperCase() : "MIRAFLORES";
        boolean okSede = sede.isEmpty() || sedeUnidad.equals(sede);
        VehiculoModelo m = u.getVehiculoModelo();
        boolean okCategoria = categoria.isEmpty() || (m != null && TarifarioPaulCars.categoria(m).equals(categoria));
        return okSede && okCategoria;
    }

    private boolean coincideModelo(VehiculoModelo m, String sede, String categoria) {
        boolean okSede = sede.isEmpty()
                || (m.getSede() != null && m.getSede().toUpperCase().contains(sede))
                || (m.getUnidades() != null && m.getUnidades().stream().anyMatch(u -> u.getSede() != null && u.getSede().equalsIgnoreCase(sede)));
        boolean okCategoria = categoria.isEmpty() || TarifarioPaulCars.categoria(m).equals(categoria);
        return okSede && okCategoria;
    }

    private boolean coincideAlquiler(Alquiler a, String sede, String categoria) {
        boolean okSede = sede.isEmpty() || sedeDe(a).equals(sede);
        boolean okCategoria = categoria.isEmpty() || categoriaDe(a).equals(categoria);
        return okSede && okCategoria;
    }

    private String categoriaDe(Alquiler a) {
        if (a != null && a.getVehiculoModelo() != null) {
            return TarifarioPaulCars.categoria(a.getVehiculoModelo());
        }
        return "SEDAN";
    }

    private String sedeDe(Alquiler a) {
        if (a == null) return "MIRAFLORES";
        if (a.getSedeRecojo() != null && !a.getSedeRecojo().isBlank()) {
            return a.getSedeRecojo().trim().toUpperCase();
        }
        if (a.getVehiculoUnidad() != null && a.getVehiculoUnidad().getSede() != null) {
            return a.getVehiculoUnidad().getSede().trim().toUpperCase();
        }
        return "MIRAFLORES";
    }

    private String estado(Alquiler a) {
        return a.getEstado() != null ? a.getEstado().getNombreEstado().toUpperCase() : "";
    }

    private long contarEstadoUnidad(List<VehiculoUnidad> flota, String estado) {
        return flota.stream()
                .filter(u -> u.getEstado() != null && estado.equalsIgnoreCase(u.getEstado().getNombreEstado()))
                .count();
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank() || "TODAS".equalsIgnoreCase(valor) || "TODOS".equalsIgnoreCase(valor)) {
            return "";
        }
        return valor.trim().toUpperCase();
    }

    private BigDecimal suma(List<BigDecimal> valores) {
        return valores.stream()
                .map(TarifarioPaulCars::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, List<Pago>> agrupar(List<Pago> pagos, java.util.function.Function<Pago, String> clave) {
        Map<String, List<Pago>> mapa = new LinkedHashMap<>();
        for (Pago p : pagos) {
            mapa.computeIfAbsent(clave.apply(p), k -> new ArrayList<>()).add(p);
        }
        return mapa;
    }

    private List<FilaMonto> filas(Map<String, List<Pago>> mapa) {
        return mapa.entrySet().stream()
                .map(e -> new FilaMonto(e.getKey(), e.getValue().size(),
                        suma(e.getValue().stream().map(Pago::getMontoTotal).toList())))
                .sorted(Comparator.comparing(FilaMonto::total).reversed())
                .toList();
    }
}
