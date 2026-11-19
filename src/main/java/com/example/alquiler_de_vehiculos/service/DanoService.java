package com.example.alquiler_de_vehiculos.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.DanosDTOs.CostoDano;
import com.example.alquiler_de_vehiculos.dto.DanosDTOs.DanoDTO;
import com.example.alquiler_de_vehiculos.dto.DanosDTOs.DanoRequest;
import com.example.alquiler_de_vehiculos.dto.DanosDTOs.DanosAlquilerDTO;
import com.example.alquiler_de_vehiculos.dto.DanosDTOs.PagoDeudaRequest;
import com.example.alquiler_de_vehiculos.dto.DanosDTOs.TasacionRequest;
import com.example.alquiler_de_vehiculos.dto.DanosDTOs.TasacionResultado;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.DanoVehiculo;
import com.example.alquiler_de_vehiculos.model.EstadoAlquiler;
import com.example.alquiler_de_vehiculos.model.MetodoPago;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.DanoVehiculoRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoAlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.MetodoPagoRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;
import com.example.alquiler_de_vehiculos.security.SecurityUtils;
import com.example.alquiler_de_vehiculos.util.TarifarioPaulCars;

import jakarta.transaction.Transactional;

/**
 * Módulo de daños: inspección al entregar y al recibir el vehículo, tasación del administrador, reparto del costo entre el
 * cliente y el seguro, liquidación de la garantía y cobro de la deuda cuando el cargo supera la garantía.
 */
@Service
public class DanoService {

    public static final String PENDIENTE = "PENDIENTE_EVALUACION";
    public static final String EVALUADO = "EVALUADO";

    private static final Set<String> ZONAS = Set.of("FRENTE", "LATERAL_IZQUIERDO", "LATERAL_DERECHO", "POSTERIOR", "TECHO",
            "LUNAS", "LLANTAS", "INTERIOR", "OTRO");
    private static final Set<String> TIPOS = Set.of("RAYON", "ABOLLADURA", "ROTURA", "FALTANTE", "OTRO");
    private static final Set<String> GRAVEDADES = Set.of("LEVE", "MODERADO", "GRAVE");
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");
    private static final String PREFIJO_FOTOS = "/uploads/danos/";

    private final DanoVehiculoRepository danoRepository;
    private final AlquilerRepository alquilerRepository;
    private final PagoRepository pagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final EstadoAlquilerRepository estadoAlquilerRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecurityUtils securityUtils;
    private final Path carpetaFotos;

    public DanoService(DanoVehiculoRepository danoRepository,
                       AlquilerRepository alquilerRepository,
                       PagoRepository pagoRepository,
                       MetodoPagoRepository metodoPagoRepository,
                       EstadoAlquilerRepository estadoAlquilerRepository,
                       UsuarioRepository usuarioRepository,
                       SecurityUtils securityUtils,
                       @Value("${app.uploads.dir:uploads}") String carpetaUploads) {
        this.danoRepository = danoRepository;
        this.alquilerRepository = alquilerRepository;
        this.pagoRepository = pagoRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.estadoAlquilerRepository = estadoAlquilerRepository;
        this.usuarioRepository = usuarioRepository;
        this.securityUtils = securityUtils;
        this.carpetaFotos = Paths.get(carpetaUploads, "danos").toAbsolutePath().normalize();
    }

    // ───────────────────────── Fotos ─────────────────────────

    public String guardarFoto(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se recibió ninguna foto");
        }
        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        String extension = nombreOriginal.contains(".")
                ? nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        String tipo = archivo.getContentType() != null ? archivo.getContentType() : "";
        if (!EXTENSIONES.contains(extension) || !tipo.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se aceptan fotos JPG, PNG o WEBP");
        }
        try {
            Files.createDirectories(carpetaFotos);
            String nombre = UUID.randomUUID() + "." + extension;
            Path destino = carpetaFotos.resolve(nombre).normalize();
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return PREFIJO_FOTOS + nombre;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la foto");
        }
    }

    // ───────────────────────── Inspección ─────────────────────────

    /** Guarda los daños de una inspección (entrega o recepción). Cada daño exige zona, tipo, gravedad, descripción y foto. */
    @Transactional
    public List<DanoVehiculo> registrar(Alquiler alquiler, String momento, List<DanoRequest> lista) {
        if (lista == null || lista.isEmpty()) return List.of();
        String quien = nombreUsuarioActual();
        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        List<DanoVehiculo> guardados = new java.util.ArrayList<>();
        for (DanoRequest r : lista) {
            if (r == null) continue;
            String zona = valido(r.zona(), ZONAS, "la zona");
            String tipo = valido(r.tipo(), TIPOS, "el tipo de daño");
            String gravedad = valido(r.gravedad(), GRAVEDADES, "la gravedad");
            String descripcion = r.descripcion() == null ? "" : r.descripcion().trim();
            if (descripcion.length() < 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Describa el daño de la zona " + etiquetaZona(zona) + ".");
            }
            if (descripcion.length() > 500) descripcion = descripcion.substring(0, 500);
            String foto = r.fotoUrl() == null ? "" : r.fotoUrl().trim();
            if (!foto.startsWith(PREFIJO_FOTOS) || foto.contains("..")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada daño debe llevar una foto como evidencia (zona " + etiquetaZona(zona) + ").");
            }
            DanoVehiculo d = new DanoVehiculo();
            d.setAlquiler(alquiler);
            d.setMomento(momento);
            d.setZona(zona);
            d.setTipo(tipo);
            d.setGravedad(gravedad);
            d.setDescripcion(descripcion);
            d.setFotoUrl(foto);
            d.setRegistradoPor(quien);
            d.setFechaRegistro(ahora);
            guardados.add(danoRepository.save(d));
        }
        return guardados;
    }

    public List<DanoDTO> listar(Integer idAlquiler, String momento) {
        List<DanoVehiculo> lista = momento == null
                ? danoRepository.findByAlquiler_IdAlquilerOrderByIdDano(idAlquiler)
                : danoRepository.findByAlquiler_IdAlquilerAndMomentoOrderByIdDano(idAlquiler, momento);
        return lista.stream().map(DanoService::aDto).toList();
    }

    public static DanoDTO aDto(DanoVehiculo d) {
        return new DanoDTO(d.getIdDano(), d.getMomento(), d.getZona(), d.getTipo(), d.getGravedad(), d.getDescripcion(),
                d.getFotoUrl(), d.getRegistradoPor(), d.getFechaRegistro(), d.getCostoReparacion());
    }

    // ───────────────────────── Reparto del costo ─────────────────────────

    /**
     * Reparte el costo real de los daños entre el cliente y el seguro y calcula cómo queda la garantía.
     * <ul>
     *   <li>Con Seguro Todo Riesgo el seguro aplica solo: el cliente paga únicamente el deducible (o el costo del daño, si es
     *       menor) y el seguro cubre el resto.</li>
     *   <li>Si el seguro no aplica por una exclusión (ebriedad, conductor no autorizado, uso indebido, dolo) o el cliente no
     *       lo contrató, el cliente paga todo el costo.</li>
     *   <li>Todo cargo (combustible, retraso y daños) sale primero de la garantía; lo que no alcanza queda como deuda.</li>
     * </ul>
     */
    public TasacionResultado calcular(Alquiler a, BigDecimal costoTotal, boolean seguroNoAplica) {
        boolean seguro = TarifarioPaulCars.tieneSeguro(a.getServiciosExtras());
        boolean aplica = seguro && !seguroNoAplica;
        BigDecimal garantia = TarifarioPaulCars.redondear(TarifarioPaulCars.nz(a.getGarantiaRetenida()));
        BigDecimal costo = TarifarioPaulCars.redondear(TarifarioPaulCars.nz(costoTotal));
        BigDecimal deducible = TarifarioPaulCars.redondear(TarifarioPaulCars.deducibleSeguro(a.getVehiculoModelo()));

        BigDecimal cliente = aplica ? costo.min(deducible) : costo;
        BigDecimal cubierto = costo.subtract(cliente);

        BigDecimal combustible = TarifarioPaulCars.nz(a.getCargoCombustible());
        BigDecimal retraso = TarifarioPaulCars.nz(a.getCargoRetraso());
        BigDecimal total = combustible.add(retraso).add(cliente);
        BigDecimal liberada = garantia.subtract(total).max(BigDecimal.ZERO);
        BigDecimal deuda = total.subtract(garantia).max(BigDecimal.ZERO);
        return new TasacionResultado(costo, seguro, aplica, deducible, TarifarioPaulCars.redondear(cliente), TarifarioPaulCars.redondear(cubierto),
                garantia, TarifarioPaulCars.redondear(combustible), TarifarioPaulCars.redondear(retraso),
                TarifarioPaulCars.redondear(total), TarifarioPaulCars.redondear(liberada), TarifarioPaulCars.redondear(deuda));
    }

    /** Vista previa para el administrador mientras ingresa los costos (no guarda nada). */
    public TasacionResultado vistaPrevia(Integer idAlquiler, TasacionRequest req) {
        Alquiler a = buscar(idAlquiler);
        BigDecimal total = req == null || req.costos() == null ? BigDecimal.ZERO : req.costos().stream()
                .map(c -> c == null || c.costo() == null ? BigDecimal.ZERO : c.costo().max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return calcular(a, total, req != null && Boolean.TRUE.equals(req.seguroNoAplica()));
    }

    // ───────────────────────── Tasación ─────────────────────────

    /** El administrador fija el costo de reparación de cada daño y el porcentaje que cubre el seguro; se liquida la garantía. */
    @Transactional
    public Alquiler tasar(Integer idAlquiler, TasacionRequest req) {
        Alquiler a = buscar(idAlquiler);
        if (!PENDIENTE.equals(a.getEstadoDanos())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este alquiler no tiene daños pendientes de evaluación.");
        }
        List<DanoVehiculo> danos = danoRepository.findByAlquiler_IdAlquilerAndMomentoOrderByIdDano(idAlquiler, DanoVehiculo.RECEPCION);
        Map<Integer, BigDecimal> costos = new HashMap<>();
        if (req != null && req.costos() != null) {
            for (CostoDano c : req.costos()) {
                if (c != null && c.idDano() != null) costos.put(c.idDano(), c.costo());
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        for (DanoVehiculo d : danos) {
            BigDecimal costo = costos.get(d.getIdDano());
            if (costo == null || costo.signum() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ingrese el costo de reparación del daño de la zona " + etiquetaZona(d.getZona()) + " (puede ser S/ 0.00).");
            }
            d.setCostoReparacion(TarifarioPaulCars.redondear(costo));
            total = total.add(costo);
        }
        boolean seguro = TarifarioPaulCars.tieneSeguro(a.getServiciosExtras());
        boolean noAplica = seguro && req != null && Boolean.TRUE.equals(req.seguroNoAplica());
        String motivo = req == null || req.observacion() == null ? "" : req.observacion().trim();
        if (noAplica && motivo.length() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Si el seguro no aplica, indique el motivo de la exclusión en la observación.");
        }
        TasacionResultado r = calcular(a, total, noAplica);
        danoRepository.saveAll(danos);

        a.setCostoDanos(r.costoTotal());
        // 100 = el seguro aplicó; 0 = exclusión; sin valor = el cliente no contrató el seguro
        a.setCoberturaSeguroPct(!r.tieneSeguro() ? null : r.seguroAplica() ? 100 : 0);
        a.setMontoCubiertoSeguro(r.cubiertoSeguro());
        a.setCargoDanos(r.cargoCliente());
        String obs = req == null || req.observacion() == null ? null : req.observacion().trim();
        a.setObservacionTasacion(obs == null || obs.isEmpty() ? null : (obs.length() > 500 ? obs.substring(0, 500) : obs));
        a.setTasadoPor(nombreUsuarioActual());
        a.setFechaTasacion(LocalDateTime.now().withNano(0));
        a.setEstadoDanos(EVALUADO);
        a.setGarantiaLiberada(r.garantiaLiberada());
        a.setEstado(estado(r.deuda().signum() > 0 ? "CERRADO_CON_DEUDA" : "FINALIZADO"));
        return alquilerRepository.save(a);
    }

    // ───────────────────────── Deuda ─────────────────────────

    /** Lo ya cobrado de la deuda (pagos de tipo LIQUIDACION). */
    public BigDecimal deudaPagada(Integer idAlquiler) {
        return pagoRepository.findByAlquiler_IdAlquiler(idAlquiler).stream()
                .filter(p -> "LIQUIDACION".equalsIgnoreCase(p.getTipoPago()) && p.getMontoTotal() != null && p.getMontoTotal().signum() > 0)
                .map(Pago::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total de cargos del cierre: combustible, retraso y daños a cargo del cliente (estos solo cuando ya fueron evaluados). */
    public static BigDecimal totalCargos(Alquiler a) {
        BigDecimal total = TarifarioPaulCars.nz(a.getCargoCombustible()).add(TarifarioPaulCars.nz(a.getCargoRetraso()));
        if (EVALUADO.equals(a.getEstadoDanos())) total = total.add(TarifarioPaulCars.nz(a.getCargoDanos()));
        return total;
    }

    /** Parte de los cargos que superó la garantía. */
    public static BigDecimal deudaBruta(Alquiler a) {
        if (a.getGarantiaRetenida() == null) return BigDecimal.ZERO;
        return TarifarioPaulCars.redondear(totalCargos(a).subtract(a.getGarantiaRetenida()).max(BigDecimal.ZERO));
    }

    public BigDecimal deudaPendiente(Alquiler a) {
        if (PENDIENTE.equals(a.getEstadoDanos())) return BigDecimal.ZERO; // aún no se sabe cuánto es
        return deudaBruta(a).subtract(deudaPagada(a.getIdAlquiler())).max(BigDecimal.ZERO);
    }

    @Transactional
    public DanosAlquilerDTO cobrarDeuda(Integer idAlquiler, PagoDeudaRequest req) {
        Alquiler a = buscar(idAlquiler);
        BigDecimal pendiente = deudaPendiente(a);
        if (pendiente.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este alquiler no tiene deuda pendiente.");
        }
        if (req == null || req.idMetodoPago() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elija el método de pago.");
        }
        MetodoPago metodo = metodoPagoRepository.findById(req.idMetodoPago())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pago no válido."));
        BigDecimal monto = req.monto() == null ? pendiente : TarifarioPaulCars.redondear(req.monto());
        if (monto.signum() <= 0 || monto.compareTo(pendiente) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto debe ser mayor que cero y no superar la deuda pendiente (S/ " + pendiente.toPlainString() + ").");
        }
        boolean efectivo = metodo.getNombreMetodo() != null && metodo.getNombreMetodo().toUpperCase(Locale.ROOT).contains("EFECTIVO");

        Pago p = new Pago();
        p.setAlquiler(a);
        p.setMetodoPago(metodo);
        p.setMontoTotal(monto);
        p.setFechaPago(LocalDate.now());
        p.setFechaHoraPago(LocalDateTime.now().withNano(0));
        p.setEstadoPago("Pagado");
        p.setTipoPago("LIQUIDACION");
        p.setCanal("COUNTER");
        p.setRegistradoPor(nombreUsuarioActual());
        p.setValidadoPor("COBRO_DEUDA");
        p.setFechaValidacion(LocalDateTime.now().withNano(0));
        if (efectivo) {
            BigDecimal recibido = req.montoRecibido() == null ? monto : req.montoRecibido();
            if (recibido.compareTo(monto) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto recibido no cubre el pago.");
            }
            p.setMontoRecibido(recibido);
            p.setVuelto(recibido.subtract(monto));
        } else if (req.numeroOperacion() != null && !req.numeroOperacion().isBlank()) {
            p.setNumeroOperacion(req.numeroOperacion().trim());
        }
        pagoRepository.save(p);

        if (pendiente.subtract(monto).signum() <= 0) {
            a.setEstado(estado("FINALIZADO"));
            alquilerRepository.save(a);
        }
        return resumen(idAlquiler);
    }

    // ───────────────────────── Consulta ─────────────────────────

    public DanosAlquilerDTO resumen(Integer idAlquiler) {
        Alquiler a = buscar(idAlquiler);
        securityUtils.validarAccesoAlquiler(a);
        boolean seguro = TarifarioPaulCars.tieneSeguro(a.getServiciosExtras());
        TasacionResultado tasacion = EVALUADO.equals(a.getEstadoDanos())
                ? calcular(a, a.getCostoDanos(), a.getCoberturaSeguroPct() != null && a.getCoberturaSeguroPct() == 0) : null;
        BigDecimal pagada = deudaPagada(idAlquiler);
        return new DanosAlquilerDTO(
                a.getEstadoDanos(), seguro, a.getGarantiaRetenida(),
                listar(idAlquiler, DanoVehiculo.ENTREGA), listar(idAlquiler, DanoVehiculo.RECEPCION),
                tasacion, a.getObservacionTasacion(), a.getTasadoPor(), a.getFechaTasacion(),
                pagada, deudaPendiente(a));
    }

    // ───────────────────────── Utilidades ─────────────────────────

    private Alquiler buscar(Integer id) {
        return alquilerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado"));
    }

    private EstadoAlquiler estado(String nombre) {
        return estadoAlquilerRepository.findByNombreEstadoIgnoreCase(nombre)
                .orElseGet(() -> estadoAlquilerRepository.save(new EstadoAlquiler(null, nombre)));
    }

    private static String valido(String valor, Set<String> permitidos, String que) {
        String v = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!permitidos.contains(v)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique " + que + " del daño.");
        }
        return v;
    }

    public static String etiquetaZona(String zona) {
        return switch (zona == null ? "" : zona) {
            case "FRENTE" -> "frontal";
            case "LATERAL_IZQUIERDO" -> "lateral izquierda";
            case "LATERAL_DERECHO" -> "lateral derecha";
            case "POSTERIOR" -> "posterior";
            case "TECHO" -> "techo";
            case "LUNAS" -> "lunas";
            case "LLANTAS" -> "llantas";
            case "INTERIOR" -> "interior";
            default -> "otra";
        };
    }

    private String nombreUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return "Counter";
        return usuarioRepository.findByUsernameIgnoreCase(auth.getName())
                .map(u -> u.getNombreCompleto() != null && !u.getNombreCompleto().isBlank() ? u.getNombreCompleto() : u.getUsername())
                .orElse(auth.getName());
    }
}
