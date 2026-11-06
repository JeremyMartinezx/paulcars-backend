package com.example.alquiler_de_vehiculos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notificaciones_leidas",
       uniqueConstraints = @UniqueConstraint(columnNames = {"username", "clave"}))
@Getter
@Setter
@NoArgsConstructor
public class NotificacionLeida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion_leida")
    private Integer id;

    @Column(nullable = false, length = 150)
    private String username;

    @Column(nullable = false, length = 60)
    private String clave;

    @Column(name = "fecha_lectura", nullable = false)
    private LocalDateTime fechaLectura;

    public NotificacionLeida(String username, String clave) {
        this.username = username;
        this.clave = clave;
        this.fechaLectura = LocalDateTime.now();
    }
}
