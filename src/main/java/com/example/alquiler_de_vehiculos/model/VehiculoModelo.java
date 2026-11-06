package com.example.alquiler_de_vehiculos.model;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehiculos_modelos")
@Getter
@Setter
@NoArgsConstructor
public class VehiculoModelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_modelo")
    private Integer idModelo;

    @ManyToOne
    @JoinColumn(name = "id_marca", nullable = false)
    @NotNull
    @Valid
    private Marca marca;

    @NotBlank
    @Column(name = "nombre_modelo", nullable = false, length = 150)
    private String nombreModelo;

    @Column(name = "anio")
    private Integer anio;

    @ManyToOne
    @JoinColumn(name = "id_tipo", nullable = false)
    @NotNull
    @Valid
    private TipoVehiculo tipo;

    @ManyToOne
    @JoinColumn(name = "id_combustible", nullable = false)
    @NotNull
    @Valid
    private Combustible combustible;

    @NotNull
    @PositiveOrZero
    @Column(name = "precio_dia", precision = 10, scale = 2, nullable = false)
    private BigDecimal precioDia;

    @NotNull
    @PositiveOrZero
    @Column(name = "precio_hora", precision = 10, scale = 2, nullable = false)
    private BigDecimal precioHora;

    @Column(name = "garantia", precision = 10, scale = 2)
    private BigDecimal garantia;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "sede", length = 100)
    private String sede;

    @OneToMany(mappedBy = "vehiculoModelo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private Set<ImagenVehiculo> imagenes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "vehiculoModelo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("vehiculoModelo")
    private Set<VehiculoUnidad> unidades = new LinkedHashSet<>();

    @JsonProperty("idVehiculo")
    public Integer getIdVehiculo() {
        return this.idModelo;
    }

    @JsonProperty("modelo")
    public Modelo getModeloCompat() {
        return new Modelo(this.idModelo, this.nombreModelo, this.marca);
    }

    @JsonProperty("unidadesTotales")
    public int getUnidadesTotales() {
        return this.unidades != null ? this.unidades.size() : 0;
    }

    @JsonProperty("unidadesDisponibles")
    public int getUnidadesDisponibles() {
        if (!Boolean.TRUE.equals(this.activo)) {
            return 0;
        }
        if (this.unidades == null) return 0;
        return (int) this.unidades.stream()
                .filter(u -> u.getEstado() != null && "DISPONIBLE".equalsIgnoreCase(u.getEstado().getNombreEstado()))
                .count();
    }

    @JsonProperty("color")
    public String getColor() {
        String mod = this.nombreModelo != null ? this.nombreModelo.trim().toLowerCase() : "";
        if (mod.contains("swift")) return "Amarillo Deportivo";
        if (mod.contains("mazda 3") || mod.equals("3")) return "Gris Polymetal";
        if (mod.contains("kicks")) return "Azul Eléctrico";
        if (mod.contains("asx")) return "Gris Titanio";
        if (mod.contains("ranger")) return "Azul Relámpago";
        if (mod.contains("corolla")) return "Blanco Glaciar";
        if (mod.contains("versa")) return "Blanco Polar";
        if (mod.contains("tucson")) return "Negro Fantasma";
        if (mod.contains("hilux")) return "Plata";
        if (mod.contains("l200")) return "Blanco Polar";
        if (mod.contains("rio")) return "Rojo Fuego";

        if (this.unidades != null && !this.unidades.isEmpty()) {
            return this.unidades.stream()
                    .map(VehiculoUnidad::getColor)
                    .filter(c -> c != null && !c.isBlank())
                    .findFirst()
                    .orElse("Gris Plata");
        }
        return "Gris Plata";
    }

    @JsonProperty("estado")
    public EstadoVehiculo getEstadoCompat() {
        EstadoVehiculo e = new EstadoVehiculo();
        boolean disp = Boolean.TRUE.equals(this.activo);
        e.setIdEstado(disp ? 6 : 8);
        e.setNombreEstado(disp ? "DISPONIBLE" : "NO DISPONIBLE");
        return e;
    }

    @JsonProperty("estado")
    public void setEstadoCompat(Object estadoObj) {
        if (estadoObj instanceof java.util.Map<?, ?> map) {
            Object nom = map.get("nombreEstado");
            if (nom != null) {
                this.activo = "DISPONIBLE".equalsIgnoreCase(nom.toString().trim());
            } else {
                Object id = map.get("idEstado");
                if (id != null) {
                    try {
                        int idVal = Integer.parseInt(id.toString());
                        this.activo = (idVal == 6);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } else if (estadoObj instanceof EstadoVehiculo ev) {
            if (ev.getNombreEstado() != null) {
                this.activo = "DISPONIBLE".equalsIgnoreCase(ev.getNombreEstado().trim());
            } else if (ev.getIdEstado() != null) {
                this.activo = (ev.getIdEstado() == 6);
            }
        } else if (estadoObj instanceof String str) {
            this.activo = "DISPONIBLE".equalsIgnoreCase(str.trim());
        }
    }
}

