package com.example.alquiler_de_vehiculos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.EstadoVehiculo;

public interface EstadoVehiculoRepository extends JpaRepository<EstadoVehiculo, Integer> {
    Optional<EstadoVehiculo> findByNombreEstado(String nombreEstado);
    Optional<EstadoVehiculo> findByNombreEstadoIgnoreCase(String nombreEstado);
}
