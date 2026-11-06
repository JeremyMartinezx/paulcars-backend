package com.example.alquiler_de_vehiculos.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.alquiler_de_vehiculos.model.Alquiler;

public interface AlquilerRepository extends JpaRepository<Alquiler, Integer> {

    @Override
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "cliente",
            "vehiculoModelo",
            "vehiculoModelo.marca",
            "vehiculoModelo.tipo",
            "vehiculoModelo.combustible",
            "vehiculoModelo.imagenes",
            "vehiculoUnidad",
            "vehiculoUnidad.estado",
            "estado"
    })
    List<Alquiler> findAll();

    @Override
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "cliente",
            "vehiculoModelo",
            "vehiculoModelo.marca",
            "vehiculoModelo.tipo",
            "vehiculoModelo.combustible",
            "vehiculoModelo.imagenes",
            "vehiculoUnidad",
            "vehiculoUnidad.estado",
            "estado",
            "pagos"
    })
    Optional<Alquiler> findById(Integer id);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "cliente",
            "vehiculoModelo",
            "vehiculoModelo.marca",
            "vehiculoModelo.tipo",
            "vehiculoModelo.combustible",
            "vehiculoModelo.imagenes",
            "vehiculoUnidad",
            "vehiculoUnidad.estado",
            "estado"
    })
    List<Alquiler> findByCliente_IdCliente(Integer idCliente);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "cliente",
            "vehiculoModelo",
            "vehiculoModelo.imagenes",
            "vehiculoUnidad",
            "estado"
    })
    List<Alquiler> findByVehiculoModelo_IdModelo(Integer idModelo);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "cliente",
            "vehiculoModelo",
            "vehiculoModelo.imagenes",
            "vehiculoUnidad",
            "estado"
    })
    List<Alquiler> findByVehiculoUnidad_IdUnidad(Integer idUnidad);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {
            "cliente",
            "vehiculoModelo",
            "vehiculoModelo.marca",
            "vehiculoModelo.tipo",
            "vehiculoModelo.imagenes",
            "vehiculoUnidad",
            "estado"
    })
    List<Alquiler> findByEstado_NombreEstadoIgnoreCase(String nombreEstado);

    boolean existsByCliente_IdCliente(Integer idCliente);

    @Query("""
        select a.vehiculoUnidad.idUnidad from Alquiler a
        where a.vehiculoUnidad is not null
          and a.fechaFinReal is null
          and upper(a.estado.nombreEstado) in ('RESERVADO', 'CONFIRMADO', 'PAGADO', 'ACTIVO')
          and a.fechaInicio <= :fechaFin
          and a.fechaFinEstimada >= :fechaInicio
        """)
    List<Integer> findIdsUnidadesOcupadasEnFechas(@Param("fechaInicio") LocalDate fechaInicio,
                                                 @Param("fechaFin") LocalDate fechaFin);

    @Query("""
        select a from Alquiler a
        where a.vehiculoModelo.idModelo = :idModelo
          and upper(a.estado.nombreEstado) = 'PENDIENTE'
          and a.idAlquiler <> :idAlquilerExcluir
          and a.fechaInicio <= :fechaFin
          and a.fechaFinEstimada >= :fechaInicio
        """)
    List<Alquiler> findSolicitudesPendientesSolapadas(@Param("idModelo") Integer idModelo,
                                                     @Param("fechaInicio") LocalDate fechaInicio,
                                                     @Param("fechaFin") LocalDate fechaFin,
                                                     @Param("idAlquilerExcluir") Integer idAlquilerExcluir);
}
