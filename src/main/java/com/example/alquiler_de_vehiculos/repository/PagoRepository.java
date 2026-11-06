package com.example.alquiler_de_vehiculos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.Pago;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    @Override
    @EntityGraph(attributePaths = {"alquiler", "alquiler.cliente", "alquiler.vehiculoModelo", "alquiler.vehiculoUnidad", "metodoPago"})
    List<Pago> findAll();

    @Override
    @EntityGraph(attributePaths = {"alquiler", "alquiler.cliente", "alquiler.vehiculoModelo", "alquiler.vehiculoUnidad", "metodoPago"})
    Optional<Pago> findById(Integer id);

    @EntityGraph(attributePaths = {"alquiler", "metodoPago"})
    List<Pago> findByAlquiler_IdAlquiler(Integer idAlquiler);

    @EntityGraph(attributePaths = {"alquiler", "alquiler.cliente", "alquiler.vehiculoModelo", "alquiler.vehiculoUnidad", "metodoPago"})
    List<Pago> findByFechaPagoBetween(LocalDate desde, LocalDate hasta);
}
