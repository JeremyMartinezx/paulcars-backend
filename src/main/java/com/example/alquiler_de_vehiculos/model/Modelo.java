package com.example.alquiler_de_vehiculos.model;

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

@Entity
@Table(name = "modelos")
@Getter
@Setter
@NoArgsConstructor
public class Modelo {

    public Modelo(Integer idModelo, String modelo, Marca marca) {
        this.idModelo = idModelo;
        this.modelo = modelo;
        this.marca = marca;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idModelo;

     @Column(name = "nombre_modelo", nullable = false, length = 150)
     private String modelo;

     @ManyToOne
     @JoinColumn(name = "id_marca", nullable = false)
     private Marca marca;
}
