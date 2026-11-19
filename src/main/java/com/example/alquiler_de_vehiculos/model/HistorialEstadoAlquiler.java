package com.example.alquiler_de_vehiculos.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Registro de cada cambio de estado de un alquiler: de qué estado salió,
 * a cuál entró y en qué momento exacto.
 */
@Entity
@jakarta.persistence.EntityListeners(com.example.alquiler_de_vehiculos.service.CambiosListener.class)
@Table(name = "historial_estado_alquiler")
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstadoAlquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Integer idHistorial;

    @ManyToOne
    @JoinColumn(name = "id_alquiler", nullable = false)
    @JsonIgnore
    private Alquiler alquiler;

    /** Nulo cuando es el registro inicial del contrato. */
    @Column(name = "estado_anterior", length = 40)
    private String estadoAnterior;

    @Column(name = "estado_nuevo", length = 40, nullable = false)
    private String estadoNuevo;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    public HistorialEstadoAlquiler(Alquiler alquiler, String estadoAnterior, String estadoNuevo, LocalDateTime fechaHora) {
        this.alquiler = alquiler;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.fechaHora = fechaHora;
    }
}
