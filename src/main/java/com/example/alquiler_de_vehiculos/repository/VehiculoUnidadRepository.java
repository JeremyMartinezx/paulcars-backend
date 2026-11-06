package com.example.alquiler_de_vehiculos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;

public interface VehiculoUnidadRepository extends JpaRepository<VehiculoUnidad, Integer> {

    List<VehiculoUnidad> findByVehiculoModelo_IdModelo(Integer idModelo);

    List<VehiculoUnidad> findByVehiculoModelo_IdModeloAndSedeIgnoreCase(Integer idModelo, String sede);

    Optional<VehiculoUnidad> findByPlacaIgnoreCase(String placa);

    boolean existsByPlacaIgnoreCase(String placa);

    boolean existsByPlacaIgnoreCaseAndIdUnidadNot(String placa, Integer idUnidad);

    List<VehiculoUnidad> findByEstado_NombreEstadoIgnoreCase(String nombreEstado);
}

