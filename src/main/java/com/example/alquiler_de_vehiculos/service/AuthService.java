package com.example.alquiler_de_vehiculos.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.AuthResponse;
import com.example.alquiler_de_vehiculos.dto.LoginRequest;
import com.example.alquiler_de_vehiculos.dto.MeResponse;
import com.example.alquiler_de_vehiculos.dto.RegistroRequest;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.Rol;
import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.repository.ClienteRepository;
import com.example.alquiler_de_vehiculos.repository.RolRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;
import com.example.alquiler_de_vehiculos.security.CustomUserDetailService;
import com.example.alquiler_de_vehiculos.security.JwtService;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailService userDetailsService;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthService(UsuarioRepository usuarioRepository,
                       RolRepository rolRepository,
                       ClienteRepository clienteRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       CustomUserDetailService userDetailsService,
                       JwtService jwtService,
                       EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String username = request.username() != null ? request.username().trim() : "";
        String email = request.emailRecuperacion() != null ? request.emailRecuperacion().trim() : username;

        if (email.toLowerCase().endsWith("@paulcars.com") || username.toLowerCase().endsWith("@paulcars.com")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El dominio corporativo @paulcars.com está reservado exclusivamente para personal interno.");
        }

        if (usuarioRepository.existsByUsername(username) || (!email.isBlank() && usuarioRepository.existsByEmailRecuperacion(email))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electrónico ya se encuentra registrado");
        }

        Rol rol = rolRepository.findByNombreRolIgnoreCase("CLIENTE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Rol no configurado: CLIENTE"));

        Cliente cliente = null;
        if (request.nombres() != null && !request.nombres().isBlank()) {
            cliente = clienteRepository.findByEmailIgnoreCase(email).orElse(null);
            if (cliente == null) {
                Cliente nuevo = new Cliente();
                nuevo.setNombres(request.nombres().trim());
                nuevo.setApellidos(request.apellidos() != null ? request.apellidos().trim() : "");
                nuevo.setEmail(email);
                nuevo.setTelefono(request.telefono() != null ? request.telefono().trim() : "");
                nuevo.setTipoDocumento("DNI");
                nuevo.setNumeroDocumento(null);
                nuevo.setLicenciaConducir(null);
                cliente = clienteRepository.save(nuevo);
            }
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setEmailRecuperacion(email);
        usuario.setRol(rol);
        usuario.setCliente(cliente);
        usuario.setEstado(true);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuarioRepository.save(usuario);

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getUsername());
        return respuesta(userDetails, rol);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String cleanUser = request.username() != null ? request.username().trim() : "";
        String cleanPassword = request.password() != null ? request.password().trim() : "";
        if (cleanUser.matches("^[0-9]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No está permitido iniciar sesión con DNI. Ingrese con su correo electrónico.");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(cleanUser);
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!Boolean.TRUE.equals(usuario.getEstado()) || !userDetails.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta no disponible");
        }
        if (usuario.getCliente() != null) {
            String estCli = usuario.getCliente().getEstadoCliente();
            if (estCli != null && ("INACTIVO".equalsIgnoreCase(estCli) || "LISTA_NEGRA".equalsIgnoreCase(estCli))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta no disponible");
            }
        }

        boolean dniMatch = usuario.getDni() != null && !usuario.getDni().isBlank() && usuario.getDni().equalsIgnoreCase(cleanPassword);
        boolean passMatch = passwordEncoder.matches(cleanPassword, usuario.getPassword());

        if (!dniMatch && !passMatch) {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(userDetails.getUsername(), cleanPassword));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
            }
        }
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);
        return respuesta(userDetails, usuario.getRol());
    }

    public AuthResponse refrescarToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El refresh token no puede estar vacío");
        }
        String username = jwtService.obtenerUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.esRefreshTokenValido(refreshToken, userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido o expirado");
        }
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (!Boolean.TRUE.equals(usuario.getEstado()) || !userDetails.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta no disponible");
        }
        return respuesta(userDetails, usuario.getRol());
    }

    private AuthResponse respuesta(UserDetails userDetails, Rol rol) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(userDetails.getUsername()).orElse(null);
        String accessToken = jwtService.generarToken(userDetails);
        String refreshToken = jwtService.generarRefreshToken(userDetails);
        return new AuthResponse(accessToken, "Bearer",
                userDetails.getUsername(), rol.getNombreRol().toUpperCase(),
                nombreVisible(usuario), correoVisible(usuario), refreshToken);
    }

    public MeResponse perfilSesion(String username) {
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        String rol = usuario.getRol() != null ? usuario.getRol().getNombreRol().toUpperCase() : "";
        return new MeResponse(usuario.getUsername(), rol, nombreVisible(usuario), correoVisible(usuario));
    }

    private String nombreVisible(Usuario usuario) {
        if (usuario == null) return "";
        if (usuario.getNombreCompleto() != null && !usuario.getNombreCompleto().isBlank()) {
            return usuario.getNombreCompleto().trim();
        }
        String rol = usuario.getRol() != null ? usuario.getRol().getNombreRol().toUpperCase() : "";
        if ("ADMIN".equals(rol)) {
            return "Paul Cars Administrador";
        }
        if ("EMPLEADO".equals(rol)) {
            String nom = usuario.getNombres() != null ? usuario.getNombres().trim() : "";
            String ape = usuario.getApellidos() != null ? usuario.getApellidos().trim() : "";
            String primerNom = !nom.isBlank() ? nom.split("\\s+")[0] : "Carlos";
            String primerApe = !ape.isBlank() ? ape.split("\\s+")[0] : "Mendoza";
            return primerNom + " " + primerApe + " - Counter Miraflores";
        }
        Cliente c = usuario.getCliente();
        if (c != null && c.getNombres() != null && !c.getNombres().isBlank()) {
            return (c.getNombres() + " " + (c.getApellidos() != null ? c.getApellidos() : "")).trim();
        }
        String base = usuario.getUsername().contains("@")
                ? usuario.getUsername().substring(0, usuario.getUsername().indexOf('@'))
                : usuario.getUsername();
        base = base.replaceAll("[._-]+", " ").trim();
        return base.isEmpty() ? usuario.getUsername() : Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    private String correoVisible(Usuario usuario) {
        if (usuario == null) return "";
        if (usuario.getUsername() != null && usuario.getUsername().contains("@")) return usuario.getUsername();
        if (usuario.getEmailRecuperacion() != null && !usuario.getEmailRecuperacion().isBlank()) return usuario.getEmailRecuperacion();
        if (usuario.getCliente() != null && usuario.getCliente().getEmail() != null) return usuario.getCliente().getEmail();
        return usuario.getUsername();
    }

    private record CodigoRecuperacion(String codigo, LocalDateTime expiracion) {}
    private final Map<String, CodigoRecuperacion> codigosRecuperacion = new ConcurrentHashMap<>();

    public Map<String, Object> solicitarCodigoRecuperacion(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa tu correo electrónico registrado");
        }
        String clean = emailOrUsername.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByUsername(clean)
                .or(() -> usuarioRepository.findByEmailRecuperacion(clean))
                .or(() -> usuarioRepository.findByCliente_EmailIgnoreCase(clean))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe una cuenta registrada con este correo electrónico"));

        String codigo = String.format("%06d", RANDOM.nextInt(900000) + 100000);
        codigosRecuperacion.put(clean, new CodigoRecuperacion(codigo, LocalDateTime.now().plusMinutes(15)));
        if (usuario.getUsername() != null) {
            codigosRecuperacion.put(usuario.getUsername().toLowerCase().trim(), new CodigoRecuperacion(codigo, LocalDateTime.now().plusMinutes(15)));
        }
        if (usuario.getEmailRecuperacion() != null) {
            codigosRecuperacion.put(usuario.getEmailRecuperacion().toLowerCase().trim(), new CodigoRecuperacion(codigo, LocalDateTime.now().plusMinutes(15)));
        }
        if (usuario.getCliente() != null && usuario.getCliente().getEmail() != null) {
            codigosRecuperacion.put(usuario.getCliente().getEmail().toLowerCase().trim(), new CodigoRecuperacion(codigo, LocalDateTime.now().plusMinutes(15)));
        }

        log.info("Código OTP generado para recuperación de contraseña de usuario [{}]: {}", clean, codigo);

        boolean enviado = emailService.enviarCodigoVerificacion(clean, codigo);

        Map<String, Object> res = new HashMap<>();
        res.put("exito", true);
        res.put("correoEnviado", enviado);
        if (enviado) {
            res.put("mensaje", "Código de verificación enviado exitosamente a tu correo electrónico");
        } else {
            res.put("mensaje", "No se pudo enviar el correo en este momento. Intenta más tarde o acércate a counter.");
        }
        return res;
    }

    public Map<String, Object> validarCodigoRecuperacion(String emailOrUsername, String codigo) {
        if (emailOrUsername == null || emailOrUsername.isBlank() || codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo y código de verificación son requeridos");
        }
        String clean = emailOrUsername.trim().toLowerCase();
        String cod = codigo.trim();
        CodigoRecuperacion info = codigosRecuperacion.get(clean);
        if (info == null || info.expiracion().isBefore(LocalDateTime.now()) || !info.codigo().equals(cod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de verificación incorrecto o expirado");
        }
        return Map.of(
                "valido", true,
                "mensaje", "Código verificado correctamente"
        );
    }

    @Transactional
    public Map<String, Object> restablecerPassword(String emailOrUsername, String codigo, String passwordNuevo) {
        if (emailOrUsername == null || emailOrUsername.isBlank() || codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo y código de verificación son requeridos");
        }
        if (passwordNuevo == null || passwordNuevo.trim().length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe tener al menos 4 caracteres");
        }
        String clean = emailOrUsername.trim().toLowerCase();
        String cod = codigo.trim();
        CodigoRecuperacion info = codigosRecuperacion.get(clean);
        if (info == null || info.expiracion().isBefore(LocalDateTime.now()) || !info.codigo().equals(cod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de verificación incorrecto o expirado");
        }

        Usuario usuario = usuarioRepository.findByUsername(clean)
                .or(() -> usuarioRepository.findByEmailRecuperacion(clean))
                .or(() -> usuarioRepository.findByCliente_EmailIgnoreCase(clean))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setPassword(passwordEncoder.encode(passwordNuevo.trim()));
        usuarioRepository.save(usuario);

        codigosRecuperacion.remove(clean);
        if (usuario.getUsername() != null) codigosRecuperacion.remove(usuario.getUsername().toLowerCase().trim());
        if (usuario.getEmailRecuperacion() != null) codigosRecuperacion.remove(usuario.getEmailRecuperacion().toLowerCase().trim());

        return Map.of(
                "exito", true,
                "mensaje", "Tu contraseña ha sido restablecida exitosamente"
        );
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.revocarToken(token);
            log.info("Token JWT revocado exitosamente en cierre de sesión");
        }
    }
}
