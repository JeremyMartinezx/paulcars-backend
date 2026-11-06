package com.example.alquiler_de_vehiculos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.EstadoAlquiler;

public interface EstadoAlquilerRepository extends JpaRepository<EstadoAlquiler, Integer> {
    Optional<EstadoAlquiler> findByNombreEstadoIgnoreCase(String nombreEstado);
}
