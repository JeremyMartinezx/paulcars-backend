package com.example.alquiler_de_vehiculos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@jakarta.persistence.EntityListeners(com.example.alquiler_de_vehiculos.service.CambiosListener.class)
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

    // Auditoría mínima: cuándo se registró el proceso y cuándo cambió por última vez de estado
    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_ultimo_estado")
    private LocalDateTime fechaUltimoEstado;

    /** Hasta cuándo puede pagar el cliente (solo reservas aprobadas sin pago). No se guarda: se calcula al listar. */
    @jakarta.persistence.Transient
    private LocalDateTime fechaLimitePago;

    // Acta de entrega del vehículo (entrega de llaves)
    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "km_entrega")
    private Integer kmEntrega;

    /** Kilometraje que marcaba el odómetro cuando el cliente devolvió el vehículo. */
    @Column(name = "km_devolucion")
    private Integer kmDevolucion;

    /** Nivel de combustible al recibir el vehículo: LLENO, 3/4, 1/2 o 1/4. */
    @Column(name = "combustible_devolucion", length = 20)
    private String combustibleDevolucion;

    @Column(name = "combustible_entrega", length = 30)
    private String combustibleEntrega;

    @Column(name = "checklist_entrega", length = 600)
    private String checklistEntrega;

    @Column(name = "observaciones_entrega", length = 1000)
    private String observacionesEntrega;

    @Column(name = "entregado_por", length = 150)
    private String entregadoPor;


    @Column(name = "garantia_retenida", precision = 10, scale = 2)
    private BigDecimal garantiaRetenida;

    @Column(name = "garantia_liberada", precision = 10, scale = 2)
    private BigDecimal garantiaLiberada;

    @Column(name = "fecha_checkout")
    private LocalDateTime fechaCheckout;

    @Lob
    @Column(name = "acta_observaciones")
    private String actaObservaciones;


    // Cancelación de una reserva ya aprobada (distinta del rechazo de una solicitud)
    @Column(name = "motivo_cancelacion", length = 60)
    private String motivoCancelacion;

    @Column(name = "observacion_cancelacion", length = 500)
    private String observacionCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "cancelado_por", length = 150)
    private String canceladoPor;

    /** Quién canceló: CLIENTE, ADMINISTRADOR, ASESOR o SISTEMA (cancelación automática). */
    @Column(name = "cancelado_por_rol", length = 20)
    private String canceladoPorRol;

    // Cargos de la recepción (se descuentan de la garantía)
    @Column(name = "cargo_combustible", precision = 10, scale = 2)
    private java.math.BigDecimal cargoCombustible;

    @Column(name = "cargo_retraso", precision = 10, scale = 2)
    private java.math.BigDecimal cargoRetraso;

    @Column(name = "minutos_retraso")
    private Integer minutosRetraso;

    // Daños del vehículo: inspección de entrega y recepción, tasación del administrador y reparto con el seguro
    /** null = sin daños; PENDIENTE_EVALUACION (garantía retenida hasta la tasación); EVALUADO. */
    @Column(name = "estado_danos", length = 25)
    private String estadoDanos;

    /** Costo real de reparación de los daños nuevos (tasación). */
    @Column(name = "costo_danos", precision = 10, scale = 2)
    private BigDecimal costoDanos;

    @Column(name = "cobertura_seguro_pct")
    private Integer coberturaSeguroPct;

    @Column(name = "monto_cubierto_seguro", precision = 10, scale = 2)
    private BigDecimal montoCubiertoSeguro;

    /** Parte del costo de los daños que paga el cliente. */
    @Column(name = "cargo_danos", precision = 10, scale = 2)
    private BigDecimal cargoDanos;

    @Column(name = "observacion_tasacion", length = 500)
    private String observacionTasacion;

    @Column(name = "tasado_por", length = 150)
    private String tasadoPor;

    @Column(name = "fecha_tasacion")
    private LocalDateTime fechaTasacion;

    @Column(name = "motivo_rechazo", length = 100)
    private String motivoRechazo;

    @Column(name = "observacion_rechazo", length = 500)
    private String observacionRechazo;

    @OneToMany(mappedBy = "alquiler", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Pago> pagos;

    // Historial de cambios de estado (de qué estado salió, a cuál entró y cuándo)
    @OneToMany(mappedBy = "alquiler", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<HistorialEstadoAlquiler> historialEstados = new ArrayList<>();

    @PrePersist
    void alRegistrar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
        if (fechaUltimoEstado == null) {
            fechaUltimoEstado = fechaRegistro;
        }
    }

    // Cada vez que el contrato cambia de estado se actualiza la fecha del último cambio
    public void setEstado(EstadoAlquiler nuevo) {
        String anterior = this.estado != null ? this.estado.getNombreEstado() : null;
        String siguiente = nuevo != null ? nuevo.getNombreEstado() : null;
        boolean cambio = !Objects.equals(anterior, siguiente);
        this.estado = nuevo;
        if (cambio && this.estado != null) {
            LocalDateTime ahora = LocalDateTime.now();
            this.fechaUltimoEstado = ahora;
            if (this.historialEstados == null) {
                this.historialEstados = new ArrayList<>();
            }
            this.historialEstados.add(new HistorialEstadoAlquiler(this, anterior, siguiente, ahora));
        }
    }

    // Registros antiguos (sin hora) caen a la fecha de reserva
    public LocalDateTime getFechaRegistro() {
        if (fechaRegistro != null) return fechaRegistro;
        return fechaReserva != null ? fechaReserva.atStartOfDay() : null;
    }

    public LocalDateTime getFechaUltimoEstado() {
        return fechaUltimoEstado != null ? fechaUltimoEstado : getFechaRegistro();
    }
}
