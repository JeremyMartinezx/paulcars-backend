package com.example.alquiler_de_vehiculos.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alquiler_de_vehiculos.dto.AuthResponse;
import com.example.alquiler_de_vehiculos.dto.LoginRequest;
import com.example.alquiler_de_vehiculos.dto.MeResponse;
import com.example.alquiler_de_vehiculos.dto.RefreshTokenRequest;
import com.example.alquiler_de_vehiculos.dto.RegistroRequest;
import com.example.alquiler_de_vehiculos.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints de login, registro, renovación JWT y recuperación de credenciales")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Registro público de cliente", description = "Registra una cuenta de cliente con validación documental")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Inicio de sesión", description = "Autentica credenciales y genera tokens de acceso y refresco")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovación de token", description = "Genera un nuevo token de acceso a partir de un refresh token válido")
    public AuthResponse refrescarToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refrescarToken(request.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil de la sesión activa", description = "Devuelve los datos del usuario autenticado en la sesión")
    public MeResponse usuarioActual(Authentication authentication) {
        return authService.perfilSesion(authentication.getName());
    }

    @PostMapping("/recuperar-password/solicitar-codigo")
    @Operation(summary = "Solicitud de código OTP", description = "Envía código de seguridad al correo registrado")
    public ResponseEntity<Map<String, Object>> solicitarCodigoRecuperacion(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.solicitarCodigoRecuperacion(body.get("email")));
    }

    @PostMapping("/recuperar-password/validar-codigo")
    @Operation(summary = "Validación de OTP", description = "Verifica si el código ingresado coincide con el emitido")
    public ResponseEntity<Map<String, Object>> validarCodigoRecuperacion(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.validarCodigoRecuperacion(body.get("email"), body.get("codigo")));
    }

    @PostMapping("/recuperar-password/cambiar-password")
    @Operation(summary = "Restablecimiento de contraseña", description = "Aplica nueva contraseña con validación de código de recuperación")
    public ResponseEntity<Map<String, Object>> restablecerPassword(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.restablecerPassword(body.get("email"), body.get("codigo"), body.get("passwordNuevo")));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierre de sesión seguro", description = "Invalida y revoca el token JWT activo agregándolo a la lista de revocación")
    public ResponseEntity<Map<String, Object>> logout(jakarta.servlet.http.HttpServletRequest request) {
        authService.logout(request.getHeader("Authorization"));
        return ResponseEntity.ok(Map.of(
            "exito", true,
            "mensaje", "Sesión finalizada y credenciales invalidadas con éxito"
        ));
    }
}
