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
@Table(name = "combustibles")
@Getter
@Setter
@NoArgsConstructor
public class Combustible {

    public Combustible(Integer idCombustible, String nombreCombustible) {
        this.idCombustible = idCombustible;
        this.nombreCombustible = nombreCombustible;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCombustible;

    @Column(name = "nombre_combustible", nullable = false, length = 30)
    private String nombreCombustible;
}
