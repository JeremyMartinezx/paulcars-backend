package com.example.alquiler_de_vehiculos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehiculos_unidades")
@Getter
@Setter
@NoArgsConstructor
public class VehiculoUnidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Integer idUnidad;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_modelo", nullable = false)
    @JsonIgnoreProperties({"unidades", "imagenes"})
    private VehiculoModelo vehiculoModelo;

    @NotBlank
    @Column(name = "placa", nullable = false, unique = true, length = 20)
    private String placa;

    @Column(name = "numero_vin", length = 50)
    private String numeroVin;

    @Column(name = "numero_motor", length = 50)
    private String numeroMotor;

    @Column(name = "color", length = 50)
    private String color;

    @PositiveOrZero
    @Column(name = "kilometraje")
    private Integer kilometraje;

    @Column(name = "sede", length = 100)
    private String sede;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado", nullable = false)
    private EstadoVehiculo estado;

    public VehiculoUnidad(VehiculoModelo vehiculoModelo, String placa, String numeroVin, String numeroMotor,
                          String color, Integer kilometraje, String sede, EstadoVehiculo estado) {
        this.vehiculoModelo = vehiculoModelo;
        this.placa = placa;
        this.numeroVin = numeroVin;
        this.numeroMotor = numeroMotor;
        this.color = color;
        this.kilometraje = kilometraje;
        this.sede = sede;
        this.estado = estado;
    }
}

