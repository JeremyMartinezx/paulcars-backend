package com.example.alquiler_de_vehiculos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.alquiler_de_vehiculos.model.BoletaElectronica;

public interface BoletaElectronicaRepository extends JpaRepository<BoletaElectronica, Integer> {

    Optional<BoletaElectronica> findByPago_IdPago(Integer idPago);

    @Query("select coalesce(max(b.correlativo), 0) from BoletaElectronica b where b.serie = :serie")
    int ultimoCorrelativo(String serie);
}
