package com.example.alquiler_de_vehiculos.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;

/**
 * Utilidad de seguridad y control de acceso para mitigar vulnerabilidades BOLA / IDOR
 * (Broken Object Level Authorization - OWASP API1:2023).
 *
 * Garantiza que un usuario con rol CLIENTE solo pueda consultar o interactuar
 * con registros que le pertenezcan directamente, impidiendo la manipulación de IDs en la URL o payload.
 */
@Component("securityUtils")
public class SecurityUtils {

    private final UsuarioRepository usuarioRepository;

    public SecurityUtils(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Verifica si el usuario autenticado posee rol administrativo u operativo.
     */
    public boolean esAdminOEmpleado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String rol = ga.getAuthority();
            if ("ROLE_ADMIN".equals(rol) || "ROLE_EMPLEADO".equals(rol)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valida que el alquiler solicitado pertenezca al cliente autenticado.
     * Si el usuario es ADMIN o EMPLEADO, la verificación se aprueba de inmediato.
     */
    public void validarAccesoAlquiler(Alquiler alquiler) {
        if (alquiler == null) {
            return;
        }
        if (esAdminOEmpleado()) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new AccessDeniedException("Debe estar autenticado para acceder a los datos de este alquiler.");
        }

        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if (usuario == null || usuario.getCliente() == null) {
            throw new AccessDeniedException("Su cuenta no cuenta con un perfil de cliente asociado.");
        }

        if (alquiler.getCliente() == null ||
                !usuario.getCliente().getIdCliente().equals(alquiler.getCliente().getIdCliente())) {
            throw new AccessDeniedException("Acceso denegado: no tiene autorización para consultar o modificar este alquiler.");
        }
    }

    /**
     * Valida que el ID de cliente solicitado coincida con el del usuario autenticado.
     */
    public void validarAccesoCliente(Integer idCliente) {
        if (idCliente == null) {
            return;
        }
        if (esAdminOEmpleado()) {
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new AccessDeniedException("Debe estar autenticado para consultar información del cliente.");
        }

        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if (usuario == null || usuario.getCliente() == null ||
                !usuario.getCliente().getIdCliente().equals(idCliente)) {
            throw new AccessDeniedException("Acceso denegado: no tiene autorización para acceder a los datos de otro cliente.");
        }
    }
}

