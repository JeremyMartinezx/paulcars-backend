package com.example.alquiler_de_vehiculos.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.ActualizarPerfilClienteRequest;
import com.example.alquiler_de_vehiculos.dto.CambiarPasswordRequest;
import com.example.alquiler_de_vehiculos.dto.CrearClienteRequest;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.Rol;
import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.ClienteRepository;
import com.example.alquiler_de_vehiculos.repository.RolRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;

import jakarta.transaction.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AlquilerRepository alquilerRepository;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    public ClienteService(ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder,
                          AlquilerRepository alquilerRepository,
                          EmailService emailService) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.alquilerRepository = alquilerRepository;
        this.emailService = emailService;
    }

    public List<Cliente> listarClientes() {
        java.util.Set<String> emailsStaff = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != null && !"CLIENTE".equalsIgnoreCase(u.getRol().getNombreRol()))
                .map(u -> {
                    String em = u.getEmailRecuperacion() != null ? u.getEmailRecuperacion().trim().toLowerCase() : "";
                    if (em.isEmpty() && u.getUsername() != null) em = u.getUsername().trim().toLowerCase();
                    return em;
                })
                .filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        return clienteRepository.findAll().stream()
                .filter(c -> {
                    String email = c.getEmail() != null ? c.getEmail().trim().toLowerCase() : "";
                    if (email.endsWith("@paulcars.com") || emailsStaff.contains(email)) {
                        return false;
                    }
                    String nom = (c.getNombres() != null ? c.getNombres().toLowerCase() : "") + " " +
                                 (c.getApellidos() != null ? c.getApellidos().toLowerCase() : "");
                    return !nom.contains("administrador");
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public Cliente obtenerCliente(Integer idCliente) {
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    @Transactional
    public Cliente crearClienteConUsuario(CrearClienteRequest request) {
        String email = (request.email() != null && !request.email().isBlank()) ? request.email().trim() : "";

        Usuario usuarioSesion = null;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Usuario u = usuarioRepository.findByUsernameIgnoreCase(auth.getName()).orElse(null);
            if (u != null && u.getRol() != null && "CLIENTE".equalsIgnoreCase(u.getRol().getNombreRol())) {
                usuarioSesion = u;
            }
        }

        Cliente aux = null;
        if (usuarioSesion != null && usuarioSesion.getCliente() != null) {
            aux = usuarioSesion.getCliente();
        }

        if (aux == null) {
            Optional<Cliente> existentePorDoc = (request.numeroDocumento() != null && !request.numeroDocumento().isBlank())
                    ? clienteRepository.findByNumeroDocumento(request.numeroDocumento().trim())
                    : Optional.empty();
            Optional<Cliente> existentePorEmail = (!email.isBlank())
                    ? clienteRepository.findByEmailIgnoreCase(email)
                    : Optional.empty();
            aux = existentePorDoc.or(() -> existentePorEmail).orElse(null);
        }

        if (usuarioSesion == null && aux != null && !esPersonalInterno()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una cuenta con ese DNI o correo. Inicia sesión para reservar.");
        }

        Cliente clienteGuardado;
        if (aux != null) {
            if (request.nombres() != null && !request.nombres().isBlank()) aux.setNombres(request.nombres().trim());
            if (request.apellidos() != null && !request.apellidos().isBlank()) aux.setApellidos(request.apellidos().trim());
            if (request.tipoDocumento() != null && !request.tipoDocumento().isBlank()) aux.setTipoDocumento(request.tipoDocumento().trim());
            if (request.numeroDocumento() != null && !request.numeroDocumento().isBlank()) aux.setNumeroDocumento(request.numeroDocumento().trim());
            if (request.telefono() != null && !request.telefono().isBlank()) aux.setTelefono(request.telefono().trim());
            if (request.email() != null && !request.email().isBlank()) aux.setEmail(request.email().trim());
            if (request.licenciaConducir() != null && !request.licenciaConducir().isBlank()) {
                aux.setLicenciaConducir(request.licenciaConducir().trim().toUpperCase());
            }
            if (request.direccion() != null && !request.direccion().isBlank()) {
                aux.setDireccion(request.direccion().trim());
            }
            clienteGuardado = clienteRepository.save(aux);
        } else {
            Cliente cliente = new Cliente();
            cliente.setNombres(request.nombres() != null ? request.nombres().trim() : "");
            cliente.setApellidos(request.apellidos() != null ? request.apellidos().trim() : "");
            cliente.setTipoDocumento(request.tipoDocumento() != null ? request.tipoDocumento().trim() : "DNI");
            cliente.setNumeroDocumento(request.numeroDocumento() != null ? request.numeroDocumento().trim() : "");
            cliente.setTelefono(request.telefono() != null ? request.telefono().trim() : "");
            cliente.setEmail(request.email() != null ? request.email().trim() : "");
            cliente.setLicenciaConducir(
                (request.licenciaConducir() == null || request.licenciaConducir().isBlank())
                    ? "Q" + request.numeroDocumento()
                    : request.licenciaConducir().trim().toUpperCase()
            );
            if (request.direccion() != null && !request.direccion().isBlank()) {
                cliente.setDireccion(request.direccion().trim());
            }
            clienteGuardado = clienteRepository.save(cliente);
        }

        if (usuarioSesion != null) {
            if (usuarioSesion.getCliente() == null || !usuarioSesion.getCliente().getIdCliente().equals(clienteGuardado.getIdCliente())) {
                Optional<Usuario> previoDueno = usuarioRepository.findByCliente_IdCliente(clienteGuardado.getIdCliente());
                if (previoDueno.isPresent() && !previoDueno.get().getIdUsuario().equals(usuarioSesion.getIdUsuario())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ese DNI o correo pertenece a otra cuenta registrada.");
                }
                usuarioSesion.setCliente(clienteGuardado);
                usuarioRepository.save(usuarioSesion);
            }
            return clienteGuardado;
        }

        String emailLimpio = (email != null) ? email.trim() : "";
        String docLimpio = (request.numeroDocumento() != null) ? request.numeroDocumento().trim() : "";
        String passFinal = (request.password() != null && !request.password().isBlank())
                ? request.password().trim()
                : docLimpio;

        Usuario usuarioTarget = null;
        if (!emailLimpio.isBlank()) {
            usuarioTarget = usuarioRepository.findByUsernameIgnoreCase(emailLimpio)
                    .or(() -> usuarioRepository.findByEmailRecuperacionIgnoreCase(emailLimpio))
                    .orElse(null);
        }

        if (usuarioTarget == null && !docLimpio.isBlank()) {
            usuarioTarget = usuarioRepository.findByCliente_NumeroDocumento(docLimpio).orElse(null);
        }

        if (usuarioTarget == null) {
            usuarioTarget = usuarioRepository.findByCliente_IdCliente(clienteGuardado.getIdCliente()).orElse(null);
        }

        if (usuarioTarget != null) {
            if (esPersonalInterno()) {
                return clienteGuardado;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una cuenta con ese correo. Inicia sesión para reservar.");
        }

        if (!emailLimpio.isBlank()) {
            Rol rolCliente = rolRepository.findByNombreRolIgnoreCase("CLIENTE")
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Rol CLIENTE no encontrado en BD"));

            Usuario usuario = new Usuario();
            usuario.setUsername(emailLimpio);
            usuario.setPassword(passwordEncoder.encode(passFinal));
            usuario.setEmailRecuperacion(emailLimpio);
            usuario.setRol(rolCliente);
            usuario.setCliente(clienteGuardado);
            usuario.setEstado(true);
            usuario.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(usuario);
        }

        return clienteGuardado;
    }

    @Transactional
    public Cliente obtenerPerfilCliente(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String email = (usuario.getEmailRecuperacion() != null && !usuario.getEmailRecuperacion().isBlank())
                ? usuario.getEmailRecuperacion().trim()
                : usuario.getUsername().trim();

        if (usuario.getRol() != null && !"CLIENTE".equalsIgnoreCase(usuario.getRol().getNombreRol())) {
            Cliente staff = new Cliente();
            staff.setIdCliente(0);
            boolean esAdmin = "ADMIN".equalsIgnoreCase(usuario.getRol().getNombreRol());
            staff.setNombres(usuario.getNombres() != null && !usuario.getNombres().isBlank()
                    ? usuario.getNombres().trim()
                    : (esAdmin ? "Víctor Manuel" : "Carlos Miguel"));
            staff.setApellidos(usuario.getApellidos() != null && !usuario.getApellidos().isBlank()
                    ? usuario.getApellidos().trim()
                    : (esAdmin ? "Paredes Silva" : "Mendoza Muñoz"));
            staff.setTipoDocumento("DNI");
            staff.setNumeroDocumento(usuario.getDni() != null && !usuario.getDni().isBlank()
                    ? usuario.getDni().trim()
                    : (esAdmin ? "70000001" : "72172845"));
            staff.setEmail(email);
            staff.setTelefono(esAdmin ? "959137712" : "978111232");
            staff.setEstadoCliente("ACTIVO");
            return staff;
        }

        Cliente cliente = usuario.getCliente();

        if (cliente == null && email.contains("@")) {
            cliente = clienteRepository.findByEmailIgnoreCase(email).orElse(null);
        }

        if (cliente == null) {
            String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
            String[] partes = base.split("[._-]+");
            String nombre = "";
            String apellido = "";
            if (partes.length >= 2) {
                nombre = Character.toUpperCase(partes[0].charAt(0)) + (partes[0].length() > 1 ? partes[0].substring(1).toLowerCase() : "");
                apellido = Character.toUpperCase(partes[1].charAt(0)) + (partes[1].length() > 1 ? partes[1].substring(1).toLowerCase() : "");
            } else if (partes.length == 1 && !partes[0].isBlank()) {
                nombre = Character.toUpperCase(partes[0].charAt(0)) + (partes[0].length() > 1 ? partes[0].substring(1).toLowerCase() : "");
                apellido = "";
            } else {
                nombre = "Cliente";
                apellido = "";
            }

            cliente = new Cliente();
            cliente.setNombres(nombre);
            cliente.setApellidos(apellido);
            cliente.setEmail(email.contains("@") ? email : email + "@paulcars.com");
            cliente.setTipoDocumento("DNI");
            cliente.setNumeroDocumento(null);
            cliente.setTelefono("");
            cliente = clienteRepository.save(cliente);
            usuario.setCliente(cliente);
            usuarioRepository.save(usuario);
        }

        Usuario usuarioVerif = usuarioRepository.findByUsername(username)
                .or(() -> usuarioRepository.findByEmailRecuperacion(username))
                .or(() -> usuarioRepository.findByCliente_EmailIgnoreCase(username))
                .orElse(null);
        if (usuarioVerif != null && cliente != null && cliente.getNumeroDocumento() != null && usuarioVerif.getPassword() != null) {
            boolean esProvisional = passwordEncoder.matches(cliente.getNumeroDocumento(), usuarioVerif.getPassword());
            cliente.setPasswordPersonalizado(!esProvisional);
        }

        return cliente;
    }

    private record CodigoVerificacion(String codigo, LocalDateTime expiracion) {}
    private final Map<String, CodigoVerificacion> codigosPassword = new ConcurrentHashMap<>();

    public boolean solicitarCodigoPassword(String username) {
        Cliente cliente = obtenerPerfilCliente(username);
        String codigo = String.format("%06d", RANDOM.nextInt(900000) + 100000);
        codigosPassword.put(username.toLowerCase().trim(), new CodigoVerificacion(codigo, LocalDateTime.now().plusMinutes(15)));
        if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) {
            codigosPassword.put(cliente.getEmail().toLowerCase().trim(), new CodigoVerificacion(codigo, LocalDateTime.now().plusMinutes(15)));
        }
        return emailService.enviarCodigoVerificacion(cliente.getEmail(), codigo);
    }

    @Transactional
    public void cambiarPasswordCliente(String username, CambiarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .or(() -> usuarioRepository.findByEmailRecuperacion(username))
                .or(() -> usuarioRepository.findByCliente_EmailIgnoreCase(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (request.passwordNuevo() == null || request.passwordNuevo().trim().length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe tener al menos 4 caracteres");
        }

        boolean autorizado = false;
        if (request.codigoVerificacion() != null && !request.codigoVerificacion().isBlank()) {
            String cod = request.codigoVerificacion().trim();
            CodigoVerificacion info = codigosPassword.get(username.toLowerCase().trim());
            if (info == null && usuario.getEmailRecuperacion() != null) {
                info = codigosPassword.get(usuario.getEmailRecuperacion().toLowerCase().trim());
            }
            if (info != null && info.expiracion().isAfter(LocalDateTime.now()) && info.codigo().equals(cod)) {
                autorizado = true;
                codigosPassword.remove(username.toLowerCase().trim());
                if (usuario.getEmailRecuperacion() != null) {
                    codigosPassword.remove(usuario.getEmailRecuperacion().toLowerCase().trim());
                }
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de verificación incorrecto o expirado");
            }
        } else if (request.passwordActual() != null && !request.passwordActual().isBlank()) {
            if (passwordEncoder.matches(request.passwordActual().trim(), usuario.getPassword())) {
                autorizado = true;
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes ingresar tu contraseña actual o el código de verificación enviado a tu correo");
        }

        if (autorizado) {
            usuario.setPassword(passwordEncoder.encode(request.passwordNuevo().trim()));
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public Cliente actualizarPerfilCliente(String username, ActualizarPerfilClienteRequest request) {
        Usuario usuarioStaff = usuarioRepository.findByUsernameIgnoreCase(username).orElse(null);
        if (usuarioStaff != null && usuarioStaff.getRol() != null && !"CLIENTE".equalsIgnoreCase(usuarioStaff.getRol().getNombreRol())) {
            if (request.nombres() != null && !request.nombres().isBlank()) {
                usuarioStaff.setNombres(request.nombres().trim());
            }
            if (request.apellidos() != null && !request.apellidos().isBlank()) {
                usuarioStaff.setApellidos(request.apellidos().trim());
            }
            if (request.numeroDocumento() != null && !request.numeroDocumento().isBlank()) {
                usuarioStaff.setDni(request.numeroDocumento().trim());
            }
            if (request.password() != null && !request.password().isBlank()) {
                usuarioStaff.setPassword(passwordEncoder.encode(request.password().trim()));
            }
            usuarioRepository.save(usuarioStaff);
            return obtenerPerfilCliente(username);
        }

        Cliente cliente = obtenerPerfilCliente(username);

        if (request.numeroDocumento() != null && !request.numeroDocumento().isBlank()) {
            validarDocumentoUnico(request.numeroDocumento().trim(), cliente.getIdCliente());
            cliente.setNumeroDocumento(request.numeroDocumento().trim());
        }

        if (request.nombres() != null && !request.nombres().isBlank()) {
            cliente.setNombres(request.nombres().trim());
        }
        if (request.apellidos() != null && !request.apellidos().isBlank()) {
            cliente.setApellidos(request.apellidos().trim());
        }
        if (request.tipoDocumento() != null && !request.tipoDocumento().isBlank()) {
            cliente.setTipoDocumento(request.tipoDocumento().trim());
        }
        if (request.telefono() != null && !request.telefono().isBlank()) {
            cliente.setTelefono(request.telefono().trim());
        }
        if (request.licenciaConducir() != null && !request.licenciaConducir().isBlank()) {
            cliente.setLicenciaConducir(request.licenciaConducir().trim().toUpperCase());
        }
        if (request.direccion() != null) {
            cliente.setDireccion(request.direccion().trim());
        }

        if (request.password() != null && !request.password().isBlank()) {
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .or(() -> usuarioRepository.findByEmailRecuperacion(username))
                    .or(() -> usuarioRepository.findByCliente_EmailIgnoreCase(username))
                    .orElse(null);
            if (usuario != null) {
                usuario.setPassword(passwordEncoder.encode(request.password().trim()));
                usuarioRepository.save(usuario);
            }
        }

        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente actualizarCliente(Integer idCliente, Cliente cliente) {
        Cliente aux = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Id no encontrado"));
        validarDocumentoUnico(cliente.getNumeroDocumento(), idCliente);

        aux.setNombres(cliente.getNombres());
        aux.setApellidos(cliente.getApellidos());
        aux.setTipoDocumento(cliente.getTipoDocumento());
        aux.setNumeroDocumento(cliente.getNumeroDocumento());
        aux.setTelefono(cliente.getTelefono());
        aux.setEmail(cliente.getEmail());
        aux.setLicenciaConducir(cliente.getLicenciaConducir());
        if (cliente.getDireccion() != null) {
            aux.setDireccion(cliente.getDireccion());
        }

        return clienteRepository.save(aux);
    }

    @Transactional
    public void eliminarCliente(Integer idCliente) {
        Cliente aux = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Id no encontrado"));
        if (alquilerRepository.existsByCliente_IdCliente(idCliente)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cliente tiene historial de alquileres. Bloquéalo en lugar de eliminarlo.");
        }
        usuarioRepository.findByCliente_IdCliente(idCliente).ifPresent(usuarioRepository::delete);
        clienteRepository.delete(aux);
    }

    @Transactional
    public Cliente cambiarEstadoCliente(Integer idCliente, String estado) {
        String nuevo = estado == null ? "" : estado.trim().toUpperCase();
        if ("OBSERVADO_LISTA_NEGRA".equals(nuevo)) {
            nuevo = "LISTA_NEGRA";
        }
        if (!"ACTIVO".equals(nuevo) && !"LISTA_NEGRA".equals(nuevo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de cliente no válido: usa ACTIVO o LISTA_NEGRA");
        }
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        cliente.setEstadoCliente(nuevo);
        Cliente guardado = clienteRepository.save(cliente);
        boolean activo = "ACTIVO".equalsIgnoreCase(nuevo);
        usuarioRepository.findByCliente_IdCliente(idCliente).ifPresent(u -> {
            u.setEstado(activo);
            usuarioRepository.save(u);
        });
        return guardado;
    }

    private boolean esPersonalInterno() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_EMPLEADO".equals(a.getAuthority()));
    }

    private void validarDocumentoUnico(String numeroDocumento, Integer idCliente) {
        clienteRepository.findByNumeroDocumento(numeroDocumento)
                .filter(c -> !c.getIdCliente().equals(idCliente))
                .ifPresent(c -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El numero de documento ya existe");
                });
    }
}
