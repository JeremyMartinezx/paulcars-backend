package com.example.alquiler_de_vehiculos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alquileres")
@Getter
@Setter
@NoArgsConstructor
public class Alquiler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alquiler")
    private Integer idAlquiler;

    @JsonProperty("codigoReferencia")
    public String getCodigoReferencia() {
        return idAlquiler != null ? String.format("N° ALQ-%06d", idAlquiler) : "N° ALQ-000000";
    }

    @ManyToOne
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_modelo", nullable = false)
    private VehiculoModelo vehiculoModelo;

    @ManyToOne
    @JoinColumn(name = "id_unidad", nullable = true)
    private VehiculoUnidad vehiculoUnidad;

    @JsonProperty("vehiculo")
    public VehiculoModelo getVehiculo() {
        return this.vehiculoModelo;
    }

    public void setVehiculo(VehiculoModelo vehiculoModelo) {
        this.vehiculoModelo = vehiculoModelo;
    }

    @JsonProperty("placa")
    public String getPlaca() {
        return this.vehiculoUnidad != null ? this.vehiculoUnidad.getPlaca() : null;
    }

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin_estimada", nullable = false)
    private LocalDate fechaFinEstimada;

    @Column(name = "fecha_fin_real")
    private LocalDate fechaFinReal;

    @Column(name = "sede_recojo", length = 100)
    private String sedeRecojo;

    @Column(name = "sede_devolucion", length = 100)
    private String sedeDevolucion;

    @Column(name = "hora_recojo", length = 10)
    private String horaRecojo;

    @Column(name = "hora_devolucion", length = 10)
    private String horaDevolucion;

    @Column(name = "dias")
    private Integer dias;

    @Column(name = "precio_base")
    private Double precioBase;

    @Column(name = "monto_extras")
    private Double montoExtras;

    @Column(name = "precio_total")
    private Double precioTotal;

    @Column(name = "servicios_extras", length = 500)
    private String serviciosExtras;

    @ManyToOne
    @JoinColumn(name = "id_estado_alquiler", nullable = false)
    private EstadoAlquiler estado;


    @Column(name = "garantia_retenida", precision = 10, scale = 2)
    private BigDecimal garantiaRetenida;

    @Column(name = "garantia_liberada", precision = 10, scale = 2)
    private BigDecimal garantiaLiberada;

    @Column(name = "fecha_checkout")
    private LocalDateTime fechaCheckout;

    @Lob
    @Column(name = "acta_observaciones")
    private String actaObservaciones;


    @Column(name = "motivo_rechazo", length = 100)
    private String motivoRechazo;

    @Column(name = "observacion_rechazo", length = 500)
    private String observacionRechazo;

    @OneToMany(mappedBy = "alquiler", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Pago> pagos;
}
