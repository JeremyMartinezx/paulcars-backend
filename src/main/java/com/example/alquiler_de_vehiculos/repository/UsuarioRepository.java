package com.example.alquiler_de_vehiculos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @Override
    @EntityGraph(attributePaths = {"rol", "cliente"})
    List<Usuario> findAll();

    @Override
    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findById(Integer id);

    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByUsername(String username);

    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByEmailRecuperacion(String emailRecuperacion);

    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByEmailRecuperacionIgnoreCase(String emailRecuperacion);

    boolean existsByUsername(String username);

    boolean existsByEmailRecuperacion(String emailRecuperacion);

    boolean existsByRol_NombreRolIgnoreCase(String nombreRol);

    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByCliente_IdCliente(Integer idCliente);

    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByCliente_EmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"rol", "cliente"})
    Optional<Usuario> findByCliente_NumeroDocumento(String numeroDocumento);

    boolean existsByCliente_IdCliente(Integer idCliente);
}
