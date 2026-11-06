package com.example.alquiler_de_vehiculos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estadosalquiler")
@Getter
@Setter
@NoArgsConstructor
public class EstadoAlquiler {

    public EstadoAlquiler(Integer idEstadoAlquiler, String nombreEstado) {
        this.idEstadoAlquiler = idEstadoAlquiler;
        this.nombreEstado = nombreEstado;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idEstadoAlquiler;

    @Column(name = "nombre_estado")
    private String nombreEstado;
}
