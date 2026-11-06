package com.example.alquiler_de_vehiculos.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.ActualizarUsuarioRequest;
import com.example.alquiler_de_vehiculos.dto.CrearUsuarioRequest;
import com.example.alquiler_de_vehiculos.model.Rol;
import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.repository.RolRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == null || !"CLIENTE".equalsIgnoreCase(u.getRol().getNombreRol()))
                .toList();
    }

    public Usuario crearUsuario(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
        }

        Rol rol = rolRepository.findByNombreRolIgnoreCase(request.rolNombre())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El rol '" + request.rolNombre() + "' no existe"));

        if (rol.getNombreRol().equalsIgnoreCase("CLIENTE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los usuarios CLIENTE se crean mediante el registro público, no desde este panel");
        }

        validarDominioCorporativo(request.username(), request.emailRecuperacion(), rol.getNombreRol());

        Usuario usuario = new Usuario();
        usuario.setUsername(request.username().trim());

        String rawPass = (request.password() != null && !request.password().isBlank())
                ? request.password().trim()
                : (request.dni() != null && !request.dni().isBlank() ? request.dni().trim() : "123456");
        usuario.setPassword(passwordEncoder.encode(rawPass));
        usuario.setEmailRecuperacion(request.emailRecuperacion() != null ? request.emailRecuperacion().trim() : request.username().trim());

        if (request.dni() != null && !request.dni().isBlank()) {
            usuario.setDni(request.dni().trim());
        }
        if (request.nombres() != null && !request.nombres().isBlank()) {
            usuario.setNombres(request.nombres().trim());
        }
        if (request.apellidos() != null && !request.apellidos().isBlank()) {
            usuario.setApellidos(request.apellidos().trim());
        }

        String nombreComp = request.nombreCompleto();
        if (nombreComp == null || nombreComp.isBlank()) {
            String nom = request.nombres() != null ? request.nombres().trim() : "";
            String ape = request.apellidos() != null ? request.apellidos().trim() : "";
            nombreComp = (nom + " " + ape).trim();
        }
        usuario.setNombreCompleto(!nombreComp.isBlank() ? nombreComp : usuario.getUsername());
        usuario.setRol(rol);
        usuario.setEstado(true);
        usuario.setFechaCreacion(LocalDateTime.now());

        return usuarioRepository.save(usuario);
    }

    public Usuario actualizarUsuario(Integer idUsuario, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!usuario.getUsername().equalsIgnoreCase(request.username()) &&
                usuarioRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
        }

        Rol rol = rolRepository.findByNombreRolIgnoreCase(request.rolNombre())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El rol '" + request.rolNombre() + "' no existe"));

        if (rol.getNombreRol().equalsIgnoreCase("CLIENTE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede asignar el rol CLIENTE a cuentas corporativas");
        }

        validarDominioCorporativo(request.username(), request.emailRecuperacion(), rol.getNombreRol());

        usuario.setUsername(request.username().trim());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.password().trim()));
        } else if (request.dni() != null && !request.dni().isBlank() && (usuario.getPassword() == null || usuario.getPassword().isBlank())) {
            usuario.setPassword(passwordEncoder.encode(request.dni().trim()));
        }

        usuario.setEmailRecuperacion(request.emailRecuperacion() != null ? request.emailRecuperacion().trim() : request.username().trim());

        if (request.dni() != null && !request.dni().isBlank()) {
            usuario.setDni(request.dni().trim());
        }
        if (request.nombres() != null && !request.nombres().isBlank()) {
            usuario.setNombres(request.nombres().trim());
        }
        if (request.apellidos() != null && !request.apellidos().isBlank()) {
            usuario.setApellidos(request.apellidos().trim());
        }

        String nombreComp = request.nombreCompleto();
        if (nombreComp == null || nombreComp.isBlank()) {
            String nom = request.nombres() != null ? request.nombres().trim() : (usuario.getNombres() != null ? usuario.getNombres() : "");
            String ape = request.apellidos() != null ? request.apellidos().trim() : (usuario.getApellidos() != null ? usuario.getApellidos() : "");
            nombreComp = (nom + " " + ape).trim();
        }
        if (!nombreComp.isBlank()) {
            usuario.setNombreCompleto(nombreComp);
        }
        usuario.setRol(rol);

        return usuarioRepository.save(usuario);
    }

    private void validarDominioCorporativo(String username, String emailRecuperacion, String rolNombre) {
        if ("ADMIN".equalsIgnoreCase(rolNombre) || "EMPLEADO".equalsIgnoreCase(rolNombre)) {
            boolean usernameValido = username != null && username.toLowerCase().trim().endsWith("@paulcars.com");
            boolean emailValido = emailRecuperacion != null && emailRecuperacion.toLowerCase().trim().endsWith("@paulcars.com");
            if (!usernameValido && !emailValido) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Los usuarios corporativos (ADMIN y EMPLEADO) deben pertenecer obligatoriamente al dominio @paulcars.com");
            }
        }
    }

    public Usuario cambiarEstado(Integer idUsuario, boolean estado) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setEstado(estado);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminarUsuario(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if ("admin@paulcars.com".equalsIgnoreCase(usuario.getUsername()) || idUsuario.equals(1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No es posible eliminar la cuenta principal de Administrador.");
        }

        if (usuario.getCliente() != null) {
            usuario.setCliente(null);
        }

        usuarioRepository.delete(usuario);
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll().stream()
                .filter(r -> !"CLIENTE".equalsIgnoreCase(r.getNombreRol()))
                .toList();
    }
}
