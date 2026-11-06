package com.example.alquiler_de_vehiculos.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alquiler_de_vehiculos.dto.ActualizarPerfilClienteRequest;
import com.example.alquiler_de_vehiculos.dto.CambiarPasswordRequest;
import com.example.alquiler_de_vehiculos.dto.CrearClienteRequest;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.service.ClienteService;
import com.example.alquiler_de_vehiculos.security.SecurityUtils;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final SecurityUtils securityUtils;

    public ClienteController(ClienteService clienteService, SecurityUtils securityUtils) {
        this.clienteService = clienteService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ResponseEntity<List<Cliente>> listarClientes() {
        return ResponseEntity.ok(clienteService.listarClientes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> obtenerCliente(@PathVariable Integer id) {
        securityUtils.validarAccesoCliente(id);
        return ResponseEntity.ok(clienteService.obtenerCliente(id));
    }

    @PostMapping
    public ResponseEntity<Cliente> crearCliente(@RequestBody CrearClienteRequest request) {
        return new ResponseEntity<>(clienteService.crearClienteConUsuario(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable Integer id, @RequestBody Cliente cliente) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, cliente));
    }

    @GetMapping("/mi-perfil")
    public ResponseEntity<Cliente> obtenerMiPerfil(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(clienteService.obtenerPerfilCliente(authentication.getName()));
    }

    @PutMapping("/mi-perfil")
    public ResponseEntity<Cliente> actualizarMiPerfil(Authentication authentication,
                                                     @RequestBody ActualizarPerfilClienteRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(clienteService.actualizarPerfilCliente(authentication.getName(), request));
    }

    @PostMapping("/solicitar-codigo-password")
    public ResponseEntity<Map<String, Object>> solicitarCodigoPassword(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean enviado = clienteService.solicitarCodigoPassword(authentication.getName());
        return ResponseEntity.ok(Map.of(
            "exito", enviado,
            "mensaje", enviado
                ? "Código de verificación enviado a tu correo registrado"
                : "No se pudo enviar el correo. Usa tu contraseña actual para cambiarla."
        ));
    }

    @PostMapping("/cambiar-password")
    public ResponseEntity<Map<String, Object>> cambiarPassword(Authentication authentication,
                                                               @RequestBody CambiarPasswordRequest request) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        clienteService.cambiarPasswordCliente(authentication.getName(), request);
        return ResponseEntity.ok(Map.of(
            "exito", true,
            "mensaje", "¡Contraseña actualizada correctamente!"
        ));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Cliente> cambiarEstado(@PathVariable Integer id, @RequestParam String estado) {
        return ResponseEntity.ok(clienteService.cambiarEstadoCliente(id, estado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Integer id) {
        clienteService.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
