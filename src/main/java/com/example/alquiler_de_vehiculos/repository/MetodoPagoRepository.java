package com.example.alquiler_de_vehiculos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.MetodoPago;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Integer> {

    Optional<MetodoPago> findByNombreMetodo(String nombreMetodo);
}
