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
    private final Path carpetaImagenes;

    public VehiculoService(VehiculoModeloRepository vehiculoModeloRepository,
                           VehiculoUnidadRepository vehiculoUnidadRepository,
                           MarcaRepository marcaRepository,
                           TipoVehiculoRepository tipoRepository,
                           CombustibleRepository combustibleRepository,
                           EstadoVehiculoRepository estadoRepository,
                           AlquilerRepository alquilerRepository,
                           @Value("${app.uploads.dir:uploads}") String carpetaUploads) {
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
        if (unidad.getKilometraje() == null) {
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
        if (datos.getKilometraje() != null) unidad.setKilometraje(datos.getKilometraje());
        if (datos.getNumeroVin() != null) unidad.setNumeroVin(datos.getNumeroVin());
        if (datos.getNumeroMotor() != null) unidad.setNumeroMotor(datos.getNumeroMotor());
        if (datos.getSede() != null) unidad.setSede(datos.getSede().toUpperCase());
        if (datos.getEstado() != null && datos.getEstado().getNombreEstado() != null) {
            EstadoVehiculo estado = estadoRepository.findByNombreEstadoIgnoreCase(datos.getEstado().getNombreEstado())
                    .orElseGet(() -> estadoRepository.save(new EstadoVehiculo(null, datos.getEstado().getNombreEstado())));
            unidad.setEstado(estado);
        }

        return vehiculoUnidadRepository.save(unidad);
    }

    @Transactional
    public VehiculoUnidad cambiarEstadoOperativoUnidad(Integer idUnidad, String estadoNuevo) {
        if (!ESTADOS_OPERATIVOS.contains(estadoNuevo.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado no válido para inspección operativa");
        }
        VehiculoUnidad unidad = vehiculoUnidadRepository.findById(idUnidad)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad física no encontrada"));

        boolean enContrato = alquilerRepository.findByVehiculoUnidad_IdUnidad(idUnidad).stream()
                .anyMatch(a -> a.getFechaFinReal() == null && a.getEstado() != null
                        && Set.of("RESERVADO", "ACTIVO", "CONFIRMADO").contains(a.getEstado().getNombreEstado().toUpperCase()));
        if (enContrato) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La unidad física tiene un alquiler activo en curso. Primero finaliza o cancela el contrato.");
        }

        final String estadoFinal = "ALQUILADO".equalsIgnoreCase(estadoNuevo) ? "EN_RUTA" : estadoNuevo.toUpperCase();
        EstadoVehiculo estado = estadoRepository.findByNombreEstadoIgnoreCase(estadoFinal)
                .orElseGet(() -> estadoRepository.save(new EstadoVehiculo(null, estadoFinal)));
        unidad.setEstado(estado);
        return vehiculoUnidadRepository.save(unidad);
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
