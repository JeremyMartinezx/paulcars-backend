package com.example.alquiler_de_vehiculos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
@jakarta.persistence.EntityListeners(com.example.alquiler_de_vehiculos.service.CambiosListener.class)
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

    /** Kilometraje en que la unidad pasó por su último mantenimiento (nulo: se toma el último múltiplo del intervalo). */
    @Column(name = "km_ultimo_mantenimiento")
    private Integer kmUltimoMantenimiento;

    @Column(name = "sede", length = 100)
    private String sede;

    /** Alquiler en curso de esta unidad (no se guarda: se completa al listar). */
    @jakarta.persistence.Transient
    private Integer idAlquilerActual;

    /** Próxima reserva aprobada que todavía no se entrega: cuándo se recoge (no se guarda: se completa al listar). */
    @jakarta.persistence.Transient
    private java.time.LocalDateTime proximaReserva;

    @jakarta.persistence.Transient
    private Integer idProximaReserva;

    /** Verdadero si esa reserva se recoge pronto: la unidad está comprometida y no puede enviarse a limpieza. */
    @jakarta.persistence.Transient
    private Boolean reservaProximaBloquea;

    /** Si esa reserva próxima se puede pasar a otra placa libre del mismo modelo. */
    @jakarta.persistence.Transient
    private Boolean reemplazoDisponible;

    /** A quién está alquilada ahora, por ejemplo "Sofía Ramírez" (no se guarda: se completa al listar). */
    @jakarta.persistence.Transient
    private String alquiladaA;

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

    // ───── Mantenimiento por kilometraje ─────

    public static final int KM_MANTENIMIENTO_POR_DEFECTO = 10000;

    /** Cada cuántos km esta unidad debe pasar por mantenimiento (lo define su modelo). */
    @JsonProperty("kmIntervaloMantenimiento")
    public int intervaloMantenimiento() {
        Integer iv = vehiculoModelo != null ? vehiculoModelo.getKmMantenimiento() : null;
        return iv != null && iv > 0 ? iv : KM_MANTENIMIENTO_POR_DEFECTO;
    }

    @JsonIgnore
    private int baseMantenimiento() {
        if (kmUltimoMantenimiento != null) return kmUltimoMantenimiento;
        int km = kilometraje != null ? kilometraje : 0;
        int iv = intervaloMantenimiento();
        return (km / iv) * iv;
    }

    /** Kilometraje al que le toca su próximo mantenimiento. */
    @JsonProperty("kmProximoMantenimiento")
    public int kmProximoMantenimiento() {
        return baseMantenimiento() + intervaloMantenimiento();
    }

    /** Cuántos km le quedan antes de su mantenimiento (0 o menos: ya le toca). */
    @JsonProperty("kmRestantesMantenimiento")
    public int kmRestantesMantenimiento() {
        return kmProximoMantenimiento() - (kilometraje != null ? kilometraje : 0);
    }

    /**
     * Actualiza el odómetro. Antes de moverlo, si la unidad no tenía registrado su último mantenimiento, se fija en el
     * último múltiplo del intervalo que ya había pasado: así, si con este recorrido cruza el siguiente, el sistema lo detecta.
     */
    public void actualizarKilometraje(int nuevoKm) {
        if (kmUltimoMantenimiento == null) {
            kmUltimoMantenimiento = baseMantenimiento();
        }
        this.kilometraje = nuevoKm;
    }

    /** Se hizo el mantenimiento: la cuenta empieza de nuevo desde el kilometraje actual. */
    public void registrarMantenimientoRealizado() {
        this.kmUltimoMantenimiento = kilometraje != null ? kilometraje : 0;
    }
}
