package com.example.alquiler_de_vehiculos.model;

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

@Entity
@Table(name = "imagenes_vehiculo")
@Getter
@Setter
@NoArgsConstructor
public class ImagenVehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Integer idImagen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_modelo", nullable = false)
    @JsonIgnore
    private VehiculoModelo vehiculoModelo;

    @Column(name = "url_imagen", nullable = false, length = 500)
    private String urlImagen;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    public ImagenVehiculo(VehiculoModelo vehiculoModelo, String urlImagen, Integer orden) {
        this.vehiculoModelo = vehiculoModelo;
        this.urlImagen = urlImagen;
        this.orden = orden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImagenVehiculo that = (ImagenVehiculo) o;
        if (idImagen != null && that.idImagen != null) {
            return idImagen.equals(that.idImagen);
        }
        return urlImagen != null && urlImagen.equals(that.urlImagen);
    }

    @Override
    public int hashCode() {
        return idImagen != null ? idImagen.hashCode() : (urlImagen != null ? urlImagen.hashCode() : 0);
    }
}
