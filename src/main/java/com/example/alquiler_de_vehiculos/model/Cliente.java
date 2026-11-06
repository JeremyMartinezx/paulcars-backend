package com.example.alquiler_de_vehiculos.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer idCliente;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombres;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 20)
    private String tipoDocumento;

    @Column(unique = true, length = 20)
    private String numeroDocumento;

    @NotBlank
    @Column(nullable = false, length = 15)
    private String telefono;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String licenciaConducir;

    @Column(length = 250)
    private String direccion;

    @Column(name = "estado_cliente", length = 30)
    private String estadoCliente = "ACTIVO";

    @Column(name = "sancion_manual")
    private Boolean sancionManual = false;

    @Column(name = "fecha_sancion")
    private LocalDateTime fechaSancion;

    @Column(name = "motivo_sancion", length = 500)
    private String motivoSancion;

    @Column(name = "sancion_por", length = 150)
    private String sancionPor;

    @jakarta.persistence.Transient
    private Boolean passwordPersonalizado;

    public String getEstadoCliente() {
        return estadoCliente == null || estadoCliente.isBlank() ? "ACTIVO" : estadoCliente;
    }

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Alquiler> alquileres;

}
