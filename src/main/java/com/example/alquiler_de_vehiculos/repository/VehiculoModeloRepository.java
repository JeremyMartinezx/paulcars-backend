package com.example.alquiler_de_vehiculos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.VehiculoModelo;

public interface VehiculoModeloRepository extends JpaRepository<VehiculoModelo, Integer> {

    boolean existsByMarca_IdMarca(Integer idMarca);

    boolean existsByTipo_IdTipo(Integer idTipo);

    boolean existsByCombustible_IdCombustible(Integer idCombustible);

    boolean existsByMarca_IdMarcaAndNombreModeloIgnoreCase(Integer idMarca, String nombreModelo);

    @Override
    @EntityGraph(attributePaths = {"marca", "tipo", "combustible", "imagenes", "unidades", "unidades.estado"})
    List<VehiculoModelo> findAll();

    @Override
    @EntityGraph(attributePaths = {"marca", "tipo", "combustible", "imagenes", "unidades", "unidades.estado"})
    Optional<VehiculoModelo> findById(Integer id);

    @EntityGraph(attributePaths = {"marca", "tipo", "combustible", "imagenes", "unidades", "unidades.estado"})
    List<VehiculoModelo> findByActivoTrue();

    @EntityGraph(attributePaths = {"marca", "tipo", "combustible", "imagenes", "unidades", "unidades.estado"})
    List<VehiculoModelo> findBySedeIgnoreCase(String sede);
}

