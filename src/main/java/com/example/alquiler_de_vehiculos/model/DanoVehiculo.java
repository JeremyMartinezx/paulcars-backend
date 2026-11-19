package com.example.alquiler_de_vehiculos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
 * Un daño registrado en el vehículo durante una inspección. Se compara el estado de entrega (daños que ya tenía) con el de
 * recepción (daños nuevos): solo los daños nuevos pueden generar un cargo al cliente.
 */
@Entity
@Table(name = "danos_vehiculo")
@Getter
@Setter
@NoArgsConstructor
public class DanoVehiculo {

    public static final String ENTREGA = "ENTREGA";
    public static final String RECEPCION = "RECEPCION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dano")
    private Integer idDano;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alquiler", nullable = false)
    private Alquiler alquiler;

    /** ENTREGA (daño que el vehículo ya tenía) o RECEPCION (daño nuevo al devolverlo). */
    @Column(nullable = false, length = 12)
    private String momento;

    @Column(nullable = false, length = 30)
    private String zona;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 12)
    private String gravedad;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "foto_url", length = 300)
    private String fotoUrl;

    @Column(name = "registrado_por", length = 150)
    private String registradoPor;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    /** Costo real de reparación según la tasación del administrador (solo daños de recepción). */
    @Column(name = "costo_reparacion", precision = 10, scale = 2)
    private BigDecimal costoReparacion;
}
