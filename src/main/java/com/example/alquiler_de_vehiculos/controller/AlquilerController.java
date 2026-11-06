package com.example.alquiler_de_vehiculos.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.AlquilerCounterRequest;
import com.example.alquiler_de_vehiculos.dto.CheckoutRequest;
import com.example.alquiler_de_vehiculos.dto.DisponibilidadHorariosDTO;
import com.example.alquiler_de_vehiculos.dto.LiquidacionResponse;
import com.example.alquiler_de_vehiculos.dto.RechazoReservaRequest;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.service.AlquilerService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/alquileres")
@Validated
public class AlquilerController {

    private final AlquilerService alquilerService;

    public AlquilerController(AlquilerService alquilerService) {
        this.alquilerService = alquilerService;
    }

    @GetMapping("/horarios")
    public DisponibilidadHorariosDTO consultarHorarios(
            @RequestParam(required = false, defaultValue = "MIRAFLORES") String sede,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return alquilerService.consultarDisponibilidad(sede, fecha != null ? fecha : LocalDate.now());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<Alquiler> listarAlquileres() {
        return alquilerService.listarAlquileres();
    }

    @GetMapping("/{idAlquiler}")
    public Alquiler obtenerAlquiler(@PathVariable Integer idAlquiler) {
        return alquilerService.obtenerAlquiler(idAlquiler);
    }

    @PostMapping
    public Alquiler crearAlquiler(@RequestParam @NotNull Integer idCliente,
                                  @RequestParam @NotNull Integer idVehiculo,
                                  @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                  @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
                                  @RequestParam(required = false) String sedeRecojo,
                                  @RequestParam(required = false) String sedeDevolucion,
                                  @RequestParam(required = false) String horaRecojo,
                                  @RequestParam(required = false) String horaDevolucion,
                                  @RequestParam(required = false) String serviciosExtras) {
        return alquilerService.crearAlquiler(idCliente, idVehiculo, inicio, fin,
                sedeRecojo, sedeDevolucion, horaRecojo, horaDevolucion, serviciosExtras);
    }

    @PostMapping("/counter")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<Alquiler> crearAlquilerCounter(@Valid @RequestBody AlquilerCounterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alquilerService.crearAlquilerCounter(request));
    }

    @GetMapping("/mis-alquileres")
    public List<Alquiler> obtenerMisAlquileres(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return alquilerService.obtenerAlquileresPorUsername(authentication.getName());
    }

    @GetMapping("/cliente/{idCliente}")
    public List<Alquiler> obtenerAlquileresPorCliente(@PathVariable("idCliente") Integer idCliente) {
        return alquilerService.obtenerAlquileresPorCliente(idCliente);
    }

    @GetMapping("/vehiculo/{idVehiculo}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public List<Alquiler> obtenerAlquileresPorVehiculo(@PathVariable("idVehiculo") Integer idVehiculo) {
        return alquilerService.obtenerAlquileresPorVehiculo(idVehiculo);
    }

    @RequestMapping(value = "/{idAlquiler}/aprobar", method = { RequestMethod.POST, RequestMethod.PUT })
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public Alquiler aprobarReserva(@PathVariable Integer idAlquiler) {
        return alquilerService.aprobarReserva(idAlquiler);
    }

    @PutMapping("/{idAlquiler}/entregar-llaves")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public Alquiler entregarLlaves(@PathVariable Integer idAlquiler) {
        return alquilerService.entregarLlaves(idAlquiler);
    }

    @PostMapping("/{idAlquiler}/rechazar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public Alquiler rechazarReserva(@PathVariable Integer idAlquiler,
                                    @Valid @RequestBody RechazoReservaRequest request) {
        return alquilerService.rechazarReserva(idAlquiler, request);
    }

    @PostMapping("/{idAlquiler}/checkout-inspeccion")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public LiquidacionResponse checkoutInspeccion(@PathVariable Integer idAlquiler,
                                                  @RequestBody CheckoutRequest request) {
        return alquilerService.checkoutInspeccion(idAlquiler, request);
    }

    @GetMapping("/{idAlquiler}/acta")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public LiquidacionResponse obtenerActa(@PathVariable Integer idAlquiler) {
        return alquilerService.obtenerActa(idAlquiler);
    }

    @PutMapping("/{idAlquiler}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO','CLIENTE')")
    public Alquiler cancelarAlquiler(@PathVariable Integer idAlquiler) {
        return alquilerService.cancelarAlquiler(idAlquiler);
    }
}
