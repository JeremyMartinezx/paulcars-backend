package com.example.alquiler_de_vehiculos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.alquiler_de_vehiculos.model.HistorialEstadoAlquiler;

public interface HistorialEstadoAlquilerRepository extends JpaRepository<HistorialEstadoAlquiler, Integer> {

    @Query("select h from HistorialEstadoAlquiler h where h.alquiler.idAlquiler = :idAlquiler order by h.fechaHora asc, h.idHistorial asc")
    List<HistorialEstadoAlquiler> findByAlquilerOrdenado(Integer idAlquiler);

    @Query("select h from HistorialEstadoAlquiler h order by h.fechaHora asc, h.idHistorial asc")
    List<HistorialEstadoAlquiler> findTodosOrdenados();
}
