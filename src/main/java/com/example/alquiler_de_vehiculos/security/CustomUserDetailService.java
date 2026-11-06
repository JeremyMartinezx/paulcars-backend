package com.example.alquiler_de_vehiculos.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;

@Service
public class CustomUserDetailService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Usuario no proporcionado");
        }
        String cleanUser = username.trim();
        if (cleanUser.matches("^[0-9]+$")) {
            throw new UsernameNotFoundException("El acceso con DNI no está permitido. Debe ingresar con su correo electrónico.");
        }
        Usuario usuario = usuarioRepository.findByUsernameIgnoreCase(cleanUser)
                .or(() -> usuarioRepository.findByEmailRecuperacionIgnoreCase(cleanUser))
                .or(() -> usuarioRepository.findByCliente_EmailIgnoreCase(cleanUser))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + cleanUser));

        boolean activo = Boolean.TRUE.equals(usuario.getEstado());
        if (usuario.getCliente() != null) {
            String estCli = usuario.getCliente().getEstadoCliente();
            if (estCli != null && ("INACTIVO".equalsIgnoreCase(estCli) || "LISTA_NEGRA".equalsIgnoreCase(estCli))) {
                activo = false;
            }
        }

        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(usuario.getRol().getNombreRol().toUpperCase())
                .disabled(!activo)
                .build();
    }
}
