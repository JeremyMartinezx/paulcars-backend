package com.example.alquiler_de_vehiculos.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.model.Combustible;
import com.example.alquiler_de_vehiculos.model.EstadoVehiculo;
import com.example.alquiler_de_vehiculos.model.ImagenVehiculo;
import com.example.alquiler_de_vehiculos.model.Marca;
import com.example.alquiler_de_vehiculos.model.TipoVehiculo;
import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.CombustibleRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoVehiculoRepository;
import com.example.alquiler_de_vehiculos.repository.MarcaRepository;
import com.example.alquiler_de_vehiculos.repository.TipoVehiculoRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoModeloRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoUnidadRepository;
import com.example.alquiler_de_vehiculos.util.TarifarioPaulCars;

import jakarta.transaction.Transactional;

@Service
public class VehiculoService {

    private static final Set<String> ESTADOS_OPERATIVOS = Set.of("DISPONIBLE", "MANTENIMIENTO", "LIMPIEZA", "NO DISPONIBLE", "ALQUILADO", "EN_RUTA");
    private static final Set<String> EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");

    private final VehiculoModeloRepository vehiculoModeloRepository;
    private final VehiculoUnidadRepository vehiculoUnidadRepository;
    private final MarcaRepository marcaRepository;
    private final TipoVehiculoRepository tipoRepository;
    private final CombustibleRepository combustibleRepository;
    private final EstadoVehiculoRepository estadoRepository;
    private final AlquilerRepository alquilerRepository;
    private final CancelacionService cancelacionService;
    private final com.example.alquiler_de_vehiculos.config.PoliticaCancelacionProperties politica;
    private final Path carpetaImagenes;

    public VehiculoService(VehiculoModeloRepository vehiculoModeloRepository,
                           VehiculoUnidadRepository vehiculoUnidadRepository,
                           MarcaRepository marcaRepository,
                           TipoVehiculoRepository tipoRepository,
                           CombustibleRepository combustibleRepository,
                           EstadoVehiculoRepository estadoRepository,
                           AlquilerRepository alquilerRepository,
                           CancelacionService cancelacionService,
                           com.example.alquiler_de_vehiculos.config.PoliticaCancelacionProperties politica,
                           @Value("${app.uploads.dir:uploads}") String carpetaUploads) {
        this.cancelacionService = cancelacionService;
        this.politica = politica;
        this.vehiculoModeloRepository = vehiculoModeloRepository;
        this.vehiculoUnidadRepository = vehiculoUnidadRepository;
        this.marcaRepository = marcaRepository;
        this.tipoRepository = tipoRepository;
        this.combustibleRepository = combustibleRepository;
        this.estadoRepository = estadoRepository;
        this.alquilerRepository = alquilerRepository;
        this.carpetaImagenes = Paths.get(carpetaUploads, "vehiculos").toAbsolutePath().normalize();
    }


    public List<VehiculoModelo> getAllVehiculos() {
        return vehiculoModeloRepository.findAll();
    }

    public List<VehiculoModelo> getAllVehiculos(String sede) {
        if (sede != null && !sede.trim().isEmpty() && !sede.equalsIgnoreCase("TODAS")) {
            return vehiculoModeloRepository.findAll().stream()
                    .filter(m -> m.getUnidades().stream()
                            .anyMatch(u -> u.getSede() != null && u.getSede().equalsIgnoreCase(sede.trim())))
                    .toList();
        }
        return vehiculoModeloRepository.findAll();
    }

    public Optional<VehiculoModelo> getVehiculoById(Integer id) {
        return vehiculoModeloRepository.findById(id);
    }

    @Transactional
    public VehiculoModelo saveVehiculo(VehiculoModelo modelo) {
        if (modelo.getGarantia() == null) {
            modelo.setGarantia(TarifarioPaulCars.garantiaHoldPorCategoria(modelo));
        }
        if (modelo.getActivo() == null) {
            modelo.setActivo(true);
        }
        if (modelo.getKmMantenimiento() == null || modelo.getKmMantenimiento() <= 0) {
            modelo.setKmMantenimiento(VehiculoUnidad.KM_MANTENIMIENTO_POR_DEFECTO);
        }
        asignarCatalogos(modelo);
        List<ImagenVehiculo> fotos = modelo.getImagenes() != null ? new ArrayList<>(modelo.getImagenes()) : new ArrayList<>();
        modelo.setImagenes(new LinkedHashSet<>());
        aplicarImagenes(modelo, fotos);
        return vehiculoModeloRepository.save(modelo);
    }

    @Transactional
    public VehiculoModelo updateVehiculo(Integer id, VehiculoModelo detalles) {
        VehiculoModelo modelo = vehiculoModeloRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));

        if (detalles.getNombreModelo() != null && !detalles.getNombreModelo().isBlank()) {
            modelo.setNombreModelo(detalles.getNombreModelo().trim());
        }
        if (detalles.getAnio() != null && detalles.getAnio() > 1900) {
            modelo.setAnio(detalles.getAnio());
        }
        if (detalles.getColorPropio() != null && !detalles.getColorPropio().isBlank()) {
            modelo.setColorPropio(detalles.getColorPropio());
        }
        if (detalles.getPrecioDia() != null) {
            modelo.setPrecioDia(detalles.getPrecioDia());
        }
        if (detalles.getPrecioHora() != null) {
            modelo.setPrecioHora(detalles.getPrecioHora());
        } else if (detalles.getPrecioDia() != null) {
            modelo.setPrecioHora(detalles.getPrecioDia().divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP));
        }
        if (detalles.getGarantia() != null) {
            modelo.setGarantia(detalles.getGarantia());
        } else if (modelo.getGarantia() == null) {
            modelo.setGarantia(TarifarioPaulCars.garantiaHoldPorCategoria(modelo));
        }
        if (detalles.getActivo() != null) {
            modelo.setActivo(detalles.getActivo());
        }
        if (detalles.getKmMantenimiento() != null && detalles.getKmMantenimiento() > 0) {
            modelo.setKmMantenimiento(detalles.getKmMantenimiento());
        }
        if (detalles.getSede() != null && !detalles.getSede().isBlank()) {
            modelo.setSede(detalles.getSede());
        }

        if (detalles.getMarca() != null && detalles.getMarca().getIdMarca() != null) {
            marcaRepository.findById(detalles.getMarca().getIdMarca()).ifPresent(modelo::setMarca);
        }
        if (detalles.getTipo() != null && detalles.getTipo().getIdTipo() != null) {
            tipoRepository.findById(detalles.getTipo().getIdTipo()).ifPresent(modelo::setTipo);
        }
        if (detalles.getCombustible() != null && detalles.getCombustible().getIdCombustible() != null) {
            combustibleRepository.findById(detalles.getCombustible().getIdCombustible()).ifPresent(modelo::setCombustible);
        }

        if (detalles.getImagenes() != null && !detalles.getImagenes().isEmpty()) {
            aplicarImagenes(modelo, detalles.getImagenes());
        }
        return vehiculoModeloRepository.save(modelo);
    }

    @Transactional
    public void deleteVehiculo(Integer id) {
        VehiculoModelo modelo = vehiculoModeloRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));
        boolean tieneAlquileres = !alquilerRepository.findByVehiculoModelo_IdModelo(id).isEmpty();
        if (tieneAlquileres) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el modelo porque tiene historial de solicitudes o contratos de alquiler vinculados. Desactívalo en su lugar.");
        }
        vehiculoModeloRepository.delete(modelo);
    }


    public List<VehiculoUnidad> getUnidadesPorModelo(Integer idModelo) {
        List<VehiculoUnidad> unidades = vehiculoUnidadRepository.findByVehiculoModelo_IdModelo(idModelo);
        VehiculoModelo modelo = vehiculoModeloRepository.findById(idModelo).orElse(null);
        if (modelo != null && modelo.getColor() != null) {
            String colorModelo = modelo.getColor();
            for (VehiculoUnidad u : unidades) {
                if (u.getColor() == null || !u.getColor().equals(colorModelo)) {
                    u.setColor(colorModelo);
                }
            }
        }
        for (VehiculoUnidad u : unidades) {
            alquilerEnCurso(u.getIdUnidad()).ifPresent(a -> {
                // Una unidad con un cliente siempre figura como alquilada: si el estado quedó desactualizado, se corrige
                if (!"EN_RUTA".equalsIgnoreCase(etiquetaEstadoActual(u))) {
                    u.setEstado(estadoRepository.findByNombreEstadoIgnoreCase("EN_RUTA")
                            .orElseGet(() -> estadoRepository.save(new EstadoVehiculo(null, "EN_RUTA"))));
                    vehiculoUnidadRepository.save(u);
                }
                u.setIdAlquilerActual(a.getIdAlquiler());
                u.setAlquiladaA(a.getCliente() != null
                        ? (a.getCliente().getNombres() + " " + a.getCliente().getApellidos()).trim() : null);
            });
        }
        // Agenda de la placa: su próxima reserva aprobada (todavía sin entregar) y si el recojo está cerca
        java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
        for (VehiculoUnidad u : unidades) {
            proximaReserva(u.getIdUnidad()).ifPresent(a -> {
                java.time.LocalDateTime recojo = recojoDe(a);
                u.setProximaReserva(recojo);
                u.setIdProximaReserva(a.getIdAlquiler());
                boolean cercana = !recojo.isAfter(ahora.plusHours(politica.getHorasReservaBloqueaLimpieza()));
                u.setReservaProximaBloquea(cercana);
                if (cercana) {
                    u.setReemplazoDisponible(cancelacionService.hayReemplazo(u, ahora.plusHours(politica.getHorasReservaBloqueaLimpieza())));
                }
            });
        }
        unidades.sort((a, b) -> {
            int pA = getPrioridadEstado(a.getEstado() != null ? a.getEstado().getNombreEstado() : "");
            int pB = getPrioridadEstado(b.getEstado() != null ? b.getEstado().getNombreEstado() : "");
            if (pA != pB) return Integer.compare(pA, pB);
            String plA = a.getPlaca() != null ? a.getPlaca() : "";
            String plB = b.getPlaca() != null ? b.getPlaca() : "";
            return plA.compareTo(plB);
        });
        return unidades;
    }

        private int getPrioridadEstado(String estadoNombre) {
        String e = (estadoNombre != null ? estadoNombre : "").toUpperCase();
        if ("DISPONIBLE".equals(e)) return 1;
        if ("LIMPIEZA".equals(e)) return 2;
        if ("MANTENIMIENTO".equals(e)) return 3;
        if ("ALQUILADO".equals(e) || "EN_RUTA".equals(e)) return 4;
        return 5;
    }

    public List<VehiculoUnidad> getAllUnidades() {
        return vehiculoUnidadRepository.findAll();
    }

    @Transactional
    public VehiculoUnidad saveUnidad(Integer idModelo, VehiculoUnidad unidad) {
        VehiculoModelo modelo = vehiculoModeloRepository.findById(idModelo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo no encontrado"));

        String placa = unidad.getPlaca() != null ? unidad.getPlaca().trim().toUpperCase() : "";
        if (placa.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La placa es obligatoria");
        }
        if (vehiculoUnidadRepository.existsByPlacaIgnoreCase(placa)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La placa " + placa + " ya está registrada en la flota");
        }

        unidad.setIdUnidad(null);
        unidad.setVehiculoModelo(modelo);
        unidad.setPlaca(placa);
        if (unidad.getNumeroVin() == null || unidad.getNumeroVin().isBlank()) {
            unidad.setNumeroVin("VIN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        }
        if (unidad.getNumeroMotor() == null || unidad.getNumeroMotor().isBlank()) {
            unidad.setNumeroMotor("MTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        unidad.setColor(modelo.getColor());
        if (unidad.getKilometraje() == null || unidad.getKilometraje() < 0) {
            unidad.setKilometraje(0);
        }
        if (unidad.getSede() == null || unidad.getSede().isBlank()) {
            unidad.setSede(modelo.getSede() != null ? modelo.getSede() : "MIRAFLORES");
        }

        EstadoVehiculo disp = estadoRepository.findByNombreEstadoIgnoreCase("DISPONIBLE")
                .orElseGet(() -> estadoRepository.save(new EstadoVehiculo(null, "DISPONIBLE")));
        unidad.setEstado(disp);

        return vehiculoUnidadRepository.save(unidad);
    }

    @Transactional
    public VehiculoUnidad updateUnidad(Integer idUnidad, VehiculoUnidad datos) {
        VehiculoUnidad unidad = vehiculoUnidadRepository.findById(idUnidad)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad física no encontrada"));

        if (datos.getPlaca() != null && !datos.getPlaca().isBlank()) {
            String placa = datos.getPlaca().trim().toUpperCase();
            if (vehiculoUnidadRepository.existsByPlacaIgnoreCaseAndIdUnidadNot(placa, idUnidad)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La placa " + placa + " ya está en uso");
            }
            unidad.setPlaca(placa);
        }
        if (datos.getColor() != null) unidad.setColor(datos.getColor());
        if (datos.getKilometraje() != null && datos.getKilometraje() >= 0) unidad.actualizarKilometraje(datos.getKilometraje());
        if (datos.getNumeroVin() != null) unidad.setNumeroVin(datos.getNumeroVin());
        if (datos.getNumeroMotor() != null) unidad.setNumeroMotor(datos.getNumeroMotor());
        if (datos.getSede() != null) unidad.setSede(datos.getSede().toUpperCase());
        if (datos.getEstado() != null && datos.getEstado().getNombreEstado() != null) {
            validarTransicion(unidad, datos.getEstado().getNombreEstado().toUpperCase());
            if (Set.of("MANTENIMIENTO", "NO DISPONIBLE").contains(datos.getEstado().getNombreEstado().toUpperCase())) {
                cancelacionService.reasignarReservasDeUnidad(unidad, java.time.LocalDateTime.now().plusDays(politica.getDiasReasignarPorBaja()));
            }
            EstadoVehiculo estado = estadoRepository.findByNombreEstadoIgnoreCase(datos.getEstado().getNombreEstado())
                    .orElseGet(() -> estadoRepository.save(new EstadoVehiculo(null, datos.getEstado().getNombreEstado())));
            if (salioDeMantenimiento(unidad, estado)) unidad.registrarMantenimientoRealizado();
            unidad.setEstado(estado);
        }

        return vehiculoUnidadRepository.save(unidad);
    }

    // ───── Flujo de estados de una unidad ─────
    // Alquilado lo pone y lo quita el sistema (al entregar y al recibir). El administrador solo mueve la unidad entre estos:
    //   Disponible ⇄ Limpieza        Disponible/Limpieza → Mantenimiento        Mantenimiento → Limpieza (servicio hecho) → Disponible
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            "DISPONIBLE", Set.of("LIMPIEZA", "MANTENIMIENTO", "NO DISPONIBLE"),
            "LIMPIEZA", Set.of("DISPONIBLE", "MANTENIMIENTO", "NO DISPONIBLE"),
            "MANTENIMIENTO", Set.of("LIMPIEZA", "NO DISPONIBLE"),
            "NO DISPONIBLE", Set.of("LIMPIEZA", "MANTENIMIENTO"));

    private static String etiquetaEstado(String e) {
        return switch (e) {
            case "DISPONIBLE" -> "Disponible";
            case "LIMPIEZA" -> "Limpieza";
            case "MANTENIMIENTO" -> "Mantenimiento";
            case "NO DISPONIBLE" -> "No disponible";
            case "EN_RUTA", "ALQUILADO" -> "Alquilado";
            default -> e;
        };
    }

    /** El alquiler en curso (llaves entregadas y todavía no recibidas) de una unidad, si lo tiene. */
    private Optional<com.example.alquiler_de_vehiculos.model.Alquiler> alquilerEnCurso(Integer idUnidad) {
        if (idUnidad == null) return Optional.empty();
        return alquilerRepository.findByVehiculoUnidad_IdUnidad(idUnidad).stream()
                .filter(a -> a.getFechaFinReal() == null && a.getEstado() != null
                        && Set.of("ACTIVO", "EN_CURSO").contains(a.getEstado().getNombreEstado().toUpperCase()))
                .findFirst();
    }

    private static java.time.LocalDateTime recojoDe(com.example.alquiler_de_vehiculos.model.Alquiler a) {
        java.time.LocalTime hora = java.time.LocalTime.of(10, 0);
        try {
            if (a.getHoraRecojo() != null && !a.getHoraRecojo().isBlank()) hora = java.time.LocalTime.parse(a.getHoraRecojo().trim());
        } catch (java.time.format.DateTimeParseException ignorada) {
            // hora con formato inesperado: se usa la hora por defecto
        }
        return a.getFechaInicio().atTime(hora);
    }

    /** La reserva aprobada, todavía sin entregar, que se recoge primero (si tiene). */
    private Optional<com.example.alquiler_de_vehiculos.model.Alquiler> proximaReserva(Integer idUnidad) {
        if (idUnidad == null) return Optional.empty();
        return alquilerRepository.findByVehiculoUnidad_IdUnidad(idUnidad).stream()
                .filter(a -> a.getFechaFinReal() == null && a.getFechaEntrega() == null && a.getFechaInicio() != null
                        && a.getEstado() != null
                        && Set.of("RESERVADO", "PAGADO", "CONFIRMADO").contains(a.getEstado().getNombreEstado().toUpperCase()))
                .min(java.util.Comparator.comparing(VehiculoService::recojoDe));
    }

    private void validarTransicion(VehiculoUnidad unidad, String destino) {
        String actual = unidad.getEstado() != null && unidad.getEstado().getNombreEstado() != null
                ? unidad.getEstado().getNombreEstado().toUpperCase() : "DISPONIBLE";
        if (actual.equals(destino)) return;

        if ("EN_RUTA".equals(destino) || "ALQUILADO".equals(destino)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estado «Alquilado» lo asigna el sistema al entregar las llaves; no se puede elegir manualmente.");
        }
        boolean marcadaAlquilada = "EN_RUTA".equals(actual) || "ALQUILADO".equals(actual);
        if (marcadaAlquilada) {
            // Si de verdad está con un cliente no se toca (se controla antes); si no hay contrato, el estado era un error y se regulariza
            Set<String> regularizar = Set.of("LIMPIEZA", "DISPONIBLE", "MANTENIMIENTO", "NO DISPONIBLE");
            if (!regularizar.contains(destino)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Estado no permitido para esta unidad.");
            }
        } else if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(destino)) {
            String ayuda = "MANTENIMIENTO".equals(actual)
                    ? " Al terminar el mantenimiento la unidad pasa primero a Limpieza y después a Disponible."
                    : "";
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede pasar de " + etiquetaEstado(actual) + " a " + etiquetaEstado(destino) + "." + ayuda);
        }
        if ("DISPONIBLE".equals(destino) && unidad.kmRestantesMantenimiento() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La unidad alcanzó su kilometraje de mantenimiento (" + String.format(java.util.Locale.US, "%,d", unidad.kmProximoMantenimiento())
                            + " km): debe pasar por mantenimiento antes de estar disponible.");
        }
    }

    @Transactional
    public VehiculoUnidad cambiarEstadoOperativoUnidad(Integer idUnidad, String estadoNuevo) {
        if (!ESTADOS_OPERATIVOS.contains(estadoNuevo.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido para inspección operativa");
        }
        VehiculoUnidad unidad = vehiculoUnidadRepository.findById(idUnidad)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad física no encontrada"));

        final String estadoFinal = "ALQUILADO".equalsIgnoreCase(estadoNuevo) ? "EN_RUTA" : estadoNuevo.toUpperCase();

        // Un carro que está en la calle con un cliente no se puede mover hasta que lo devuelva
        var enCurso = alquilerEnCurso(idUnidad);
        if (enCurso.isPresent() && !estadoFinal.equals(etiquetaEstadoActual(unidad))) {
            var cli = enCurso.get().getCliente();
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La unidad está alquilada" + (cli != null ? " a " + (cli.getNombres() + " " + cli.getApellidos()).trim() : "")
                            + String.format(" (ALQ-%06d)", enCurso.get().getIdAlquiler())
                            + ". Primero se debe recibir el vehículo.");
        }
        validarTransicion(unidad, estadoFinal);
        // Si sale de servicio, sus reservas aún no entregadas pasan solas a otra unidad libre del mismo modelo
        if (Set.of("MANTENIMIENTO", "NO DISPONIBLE").contains(estadoFinal)) {
            cancelacionService.reasignarReservasDeUnidad(unidad, java.time.LocalDateTime.now().plusDays(politica.getDiasReasignarPorBaja()));
        } else if ("LIMPIEZA".equals(estadoFinal) && "DISPONIBLE".equals(etiquetaEstadoActual(unidad))) {
            // La limpieza es corta: las reservas lejanas no se tocan. Las que se recogen pronto pasan solas a otra placa libre;
            // si no hay otra, la unidad está comprometida y no sale.
            try {
                cancelacionService.reasignarReservasDeUnidad(unidad,
                        java.time.LocalDateTime.now().plusHours(politica.getHorasReservaBloqueaLimpieza()));
            } catch (ResponseStatusException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La unidad está comprometida con una reserva próxima y no hay otra placa libre del mismo modelo para reemplazarla.");
            }
        }

        EstadoVehiculo estado = estadoRepository.findByNombreEstadoIgnoreCase(estadoFinal)
                .orElseGet(() -> estadoRepository.save(new EstadoVehiculo(null, estadoFinal)));
        if (salioDeMantenimiento(unidad, estado)) unidad.registrarMantenimientoRealizado();
        unidad.setEstado(estado);
        return vehiculoUnidadRepository.save(unidad);
    }

    // Una unidad que vuelve a estar disponible después de mantenimiento: ya se hizo el servicio, la cuenta de km empieza de nuevo
    private static String etiquetaEstadoActual(VehiculoUnidad u) {
        return u.getEstado() != null && u.getEstado().getNombreEstado() != null ? u.getEstado().getNombreEstado().toUpperCase() : "";
    }

    private boolean salioDeMantenimiento(VehiculoUnidad unidad, EstadoVehiculo nuevo) {
        return unidad.getEstado() != null && "MANTENIMIENTO".equalsIgnoreCase(unidad.getEstado().getNombreEstado())
                && nuevo != null && Set.of("LIMPIEZA", "DISPONIBLE").contains(nuevo.getNombreEstado().toUpperCase());
    }

    @Transactional
    public void deleteUnidad(Integer idUnidad) {
        VehiculoUnidad unidad = vehiculoUnidadRepository.findById(idUnidad)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad física no encontrada"));
        boolean tieneAlquileres = !alquilerRepository.findByVehiculoUnidad_IdUnidad(idUnidad).isEmpty();
        if (tieneAlquileres) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar la unidad porque tiene registros de alquiler históricos vinculados. Cámbiala a MANTENIMIENTO o NO DISPONIBLE.");
        }
        vehiculoUnidadRepository.delete(unidad);
    }


    public String guardarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se recibió ninguna imagen");
        }
        String nombreOriginal = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        String extension = nombreOriginal.contains(".")
                ? nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase()
                : "";
        String tipo = archivo.getContentType() != null ? archivo.getContentType() : "";
        if (!EXTENSIONES.contains(extension) || !tipo.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se aceptan imágenes JPG, PNG o WEBP");
        }
        try {
            Files.createDirectories(carpetaImagenes);
            String nombreArchivo = UUID.randomUUID() + "." + extension;
            Path destino = carpetaImagenes.resolve(nombreArchivo).normalize();
            try (InputStream entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/vehiculos/" + nombreArchivo;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen");
        }
    }


    private void asignarCatalogos(VehiculoModelo modelo) {
        asignarCatalogos(modelo, modelo);
    }

    private void asignarCatalogos(VehiculoModelo destino, VehiculoModelo datos) {
        if (datos.getMarca() == null || datos.getTipo() == null || datos.getCombustible() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marca, tipo y combustible son obligatorios");
        }
        Marca marca = marcaRepository.findById(datos.getMarca().getIdMarca())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Marca no encontrada"));
        TipoVehiculo tipo = tipoRepository.findById(datos.getTipo().getIdTipo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de vehículo no encontrado"));
        Combustible combustible = combustibleRepository.findById(datos.getCombustible().getIdCombustible())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Combustible no encontrado"));

        destino.setMarca(marca);
        destino.setTipo(tipo);
        destino.setCombustible(combustible);
    }

    private void aplicarImagenes(VehiculoModelo modelo, Collection<ImagenVehiculo> fotos) {
        Map<Integer, String> porOrden = new TreeMap<>();
        for (ImagenVehiculo foto : fotos) {
            if (foto == null || foto.getUrlImagen() == null || foto.getUrlImagen().isBlank()) continue;
            int orden = foto.getOrden() != null ? foto.getOrden() : porOrden.size() + 1;
            if (orden < 1 || orden > 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El orden de las fotos va de 1 a 4");
            }
            porOrden.put(orden, foto.getUrlImagen().trim());
        }
        if (modelo.getImagenes() == null) {
            modelo.setImagenes(new LinkedHashSet<>());
        }
        modelo.getImagenes().clear();
        porOrden.forEach((orden, url) -> modelo.getImagenes().add(new ImagenVehiculo(modelo, url, orden)));
        if (porOrden.containsKey(1)) {
            modelo.setImagenUrl(porOrden.get(1));
        }
    }
}
