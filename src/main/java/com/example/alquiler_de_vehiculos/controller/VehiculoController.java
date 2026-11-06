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

    public VehiculoController(VehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }


    @GetMapping
    public List<VehiculoModelo> obtenerTodos(@RequestParam(required = false) String sede) {
        if (sede != null && !sede.trim().isEmpty()) {
            return vehiculoService.getAllVehiculos(sede);
        }
        return vehiculoService.getAllVehiculos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculoModelo> obtenerPorId(@PathVariable Integer id) {
        return vehiculoService.getVehiculoById(id)
                .map(ResponseEntity::ok)
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
