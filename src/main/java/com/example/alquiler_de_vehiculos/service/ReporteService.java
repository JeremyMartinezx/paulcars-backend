package com.example.alquiler_de_vehiculos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO.FilaMonto;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO.FilaVehiculo;
import com.example.alquiler_de_vehiculos.dto.ReporteOperativoDTO;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
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

    public ReporteService(AlquilerRepository alquilerRepository,
                          PagoRepository pagoRepository,
                          VehiculoModeloRepository vehiculoModeloRepository,
                          VehiculoUnidadRepository vehiculoUnidadRepository) {
        this.alquilerRepository = alquilerRepository;
        this.pagoRepository = pagoRepository;
        this.vehiculoModeloRepository = vehiculoModeloRepository;
        this.vehiculoUnidadRepository = vehiculoUnidadRepository;
    }

    public ReporteEjecutivoDTO ejecutivo(LocalDate desde, LocalDate hasta, String sede, String categoria) {
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
        long diasRango = Math.max(1, ChronoUnit.DAYS.between(rangoInicio, rangoFin) + 1);

        List<VehiculoUnidad> flotaUnidades = vehiculoUnidadRepository.findAll().stream()
                .filter(u -> coincideUnidad(u, sedeFiltro, categoriaFiltro))
                .toList();
        List<VehiculoModelo> flotaModelos = vehiculoModeloRepository.findAll().stream()
                .filter(m -> coincideModelo(m, sedeFiltro, categoriaFiltro))
                .toList();

        List<Alquiler> todos = alquilerRepository.findAll().stream()
                .filter(a -> coincideAlquiler(a, sedeFiltro, categoriaFiltro))
                .toList();
        List<Alquiler> validosEnRango = todos.stream()
                .filter(a -> ESTADOS_VALIDOS.contains(estado(a)))
                .filter(a -> diasSolapados(a, rangoInicio, rangoFin) > 0)
                .toList();

        List<Pago> pagos = pagoRepository.findByFechaPagoBetween(rangoInicio, rangoFin).stream()
                .filter(p -> p.getAlquiler() != null && coincideAlquiler(p.getAlquiler(), sedeFiltro, categoriaFiltro))
                .toList();
        BigDecimal ingresos = suma(pagos.stream().map(Pago::getMontoTotal).toList());

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
                top,
                filas(porSede), filas(porCategoria), filas(porMetodo));
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

                List<Alquiler> suyos = alquileres.stream()
                        .filter(a -> (a.getVehiculoUnidad() != null && u.getIdUnidad().equals(a.getVehiculoUnidad().getIdUnidad()))
                                || (a.getVehiculoUnidad() == null && a.getVehiculoModelo() != null && m != null && m.getIdModelo().equals(a.getVehiculoModelo().getIdModelo())))
                        .toList();

                BigDecimal ingresos = suma(pagos.stream()
                        .filter(p -> p.getAlquiler() != null
                                && ((p.getAlquiler().getVehiculoUnidad() != null && u.getIdUnidad().equals(p.getAlquiler().getVehiculoUnidad().getIdUnidad()))
                                        || (p.getAlquiler().getVehiculoUnidad() == null && p.getAlquiler().getVehiculoModelo() != null && m != null && m.getIdModelo().equals(p.getAlquiler().getVehiculoModelo().getIdModelo()))))
                        .map(Pago::getMontoTotal).toList());

                long dias = suyos.stream().mapToLong(a -> diasSolapados(a, inicio, fin)).sum();
                BigDecimal utilizacion = diasRango > 0
                        ? BigDecimal.valueOf(Math.min(dias, diasRango)).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(diasRango), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                filas.add(new FilaVehiculo(u.getIdUnidad(), nombreCompleto, u.getPlaca(),
                        categoria, suyos.size(), dias, ingresos, utilizacion));
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
