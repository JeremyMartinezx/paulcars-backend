package com.example.alquiler_de_vehiculos.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {
    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);
    Optional<Cliente> findByEmail(String email);
    Optional<Cliente> findByEmailIgnoreCase(String email);
}
