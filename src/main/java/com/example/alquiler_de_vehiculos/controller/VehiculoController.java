package com.example.alquiler_de_vehiculos.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.service.VehiculoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService vehiculoService;
    private final com.example.alquiler_de_vehiculos.service.AlquilerService alquilerService;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules();

    /** El personal (ADMIN y EMPLEADO) ve todo; visitantes y clientes solo reciben lo necesario para el catálogo. */
    private boolean esPersonal() {
        org.springframework.security.core.Authentication a = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream()
                .anyMatch(g -> "ROLE_ADMIN".equals(g.getAuthority()) || "ROLE_EMPLEADO".equals(g.getAuthority()));
    }

    @SuppressWarnings("unchecked")
    private Object visible(VehiculoModelo m) {
        if (esPersonal()) return m;
        Map<String, Object> mapa = mapper.convertValue(m, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        Object u = mapa.get("unidades");
        if (u instanceof List<?> lista) {
            List<Map<String, Object>> limpias = new java.util.ArrayList<>();
            for (Object o : lista) {
                Map<String, Object> un = (Map<String, Object>) o;
                Map<String, Object> corto = new java.util.LinkedHashMap<>();
                corto.put("sede", un.get("sede"));
                corto.put("color", un.get("color"));
                Object est = un.get("estado");
                if (est instanceof Map<?, ?> em) corto.put("estado", Map.of("nombreEstado", String.valueOf(em.get("nombreEstado"))));
                limpias.add(corto);
            }
            mapa.put("unidades", limpias);
        }
        return mapa;
    }

    public VehiculoController(VehiculoService vehiculoService,
                              com.example.alquiler_de_vehiculos.service.AlquilerService alquilerService) {
        this.vehiculoService = vehiculoService;
        this.alquilerService = alquilerService;
    }

    /** Unidades libres de cada modelo por sede para el rango de fechas (sin fechas: hoy y mañana). */
    @GetMapping("/disponibilidad-sedes")
    public Map<Integer, Map<String, Integer>> disponibilidadSedes(
            @RequestParam(required = false) String inicio, @RequestParam(required = false) String fin) {
        java.time.LocalDate i = inicio != null && !inicio.isBlank() ? java.time.LocalDate.parse(inicio) : java.time.LocalDate.now();
        java.time.LocalDate f = fin != null && !fin.isBlank() ? java.time.LocalDate.parse(fin) : i.plusDays(1);
        Map<Integer, Map<String, Integer>> r = new java.util.LinkedHashMap<>();
        for (VehiculoModelo m : vehiculoService.getAllVehiculos()) {
            Map<String, Integer> porSede = new java.util.LinkedHashMap<>();
            for (String sede : List.of("MIRAFLORES", "AEROPUERTO")) {
                porSede.put(sede, alquilerService.contarStockDisponibleFechas(m.getIdModelo(), i, f, sede));
            }
            r.put(m.getIdModelo(), porSede);
        }
        return r;
    }


    @GetMapping
    public List<Object> obtenerTodos(@RequestParam(required = false) String sede) {
        List<VehiculoModelo> lista = (sede != null && !sede.trim().isEmpty())
                ? vehiculoService.getAllVehiculos(sede) : vehiculoService.getAllVehiculos();
        return lista.stream().map(this::visible).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerPorId(@PathVariable Integer id) {
        return vehiculoService.getVehiculoById(id)
                .map(m -> ResponseEntity.ok(visible(m)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehiculoModelo> crearVehiculo(@Valid @RequestBody VehiculoModelo vehiculo) {
        return new ResponseEntity<>(vehiculoService.saveVehiculo(vehiculo), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehiculoModelo> actualizarVehiculo(@PathVariable Integer id, @RequestBody VehiculoModelo vehiculoDetalles) {
        return ResponseEntity.ok(vehiculoService.updateVehiculo(id, vehiculoDetalles));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarVehiculo(@PathVariable Integer id) {
        if (vehiculoService.getVehiculoById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        vehiculoService.deleteVehiculo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> subirImagen(@RequestPart("archivo") MultipartFile archivo) {
        return Map.of("url", vehiculoService.guardarImagen(archivo));
    }


    @GetMapping("/{idModelo}/unidades")
    public List<VehiculoUnidad> obtenerUnidadesPorModelo(@PathVariable Integer idModelo) {
        return vehiculoService.getUnidadesPorModelo(idModelo);
    }

    @PostMapping("/{idModelo}/unidades")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehiculoUnidad> agregarUnidad(@PathVariable Integer idModelo,
                                                        @Valid @RequestBody VehiculoUnidad unidad) {
        return new ResponseEntity<>(vehiculoService.saveUnidad(idModelo, unidad), HttpStatus.CREATED);
    }

    @PutMapping("/unidades/{idUnidad}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehiculoUnidad> actualizarUnidad(@PathVariable Integer idUnidad,
                                                          @Valid @RequestBody VehiculoUnidad unidad) {
        return ResponseEntity.ok(vehiculoService.updateUnidad(idUnidad, unidad));
    }

    @PutMapping("/unidades/{idUnidad}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VehiculoUnidad> cambiarEstadoUnidad(@PathVariable Integer idUnidad,
                                                             @RequestParam String estado) {
        return ResponseEntity.ok(vehiculoService.cambiarEstadoOperativoUnidad(idUnidad, estado));
    }

    @DeleteMapping("/unidades/{idUnidad}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarUnidad(@PathVariable Integer idUnidad) {
        vehiculoService.deleteUnidad(idUnidad);
        return ResponseEntity.noContent().build();
    }
}
