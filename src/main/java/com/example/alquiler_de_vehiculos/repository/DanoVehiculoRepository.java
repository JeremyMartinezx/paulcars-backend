package com.example.alquiler_de_vehiculos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.DanoVehiculo;

public interface DanoVehiculoRepository extends JpaRepository<DanoVehiculo, Integer> {

    List<DanoVehiculo> findByAlquiler_IdAlquilerOrderByIdDano(Integer idAlquiler);

    List<DanoVehiculo> findByAlquiler_IdAlquilerAndMomentoOrderByIdDano(Integer idAlquiler, String momento);
}
