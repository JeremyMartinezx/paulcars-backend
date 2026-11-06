package com.example.alquiler_de_vehiculos.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
@Table(name = "pagos")
@Getter
@Setter
@NoArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;

    @ManyToOne
    @JoinColumn(name = "id_alquiler", nullable = false)
    private Alquiler alquiler;

    @ManyToOne
    @JoinColumn(name = "id_metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "monto_total", precision = 10, scale = 2, nullable = false)
    private BigDecimal montoTotal;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "estado_pago", nullable = false, length = 20)
    private String estadoPago;

    @Column(name = "tipo_pago", length = 20)
    private String tipoPago;

    @Column(name = "monto_recibido", precision = 10, scale = 2)
    private BigDecimal montoRecibido;

    @Column(name = "vuelto", precision = 10, scale = 2)
    private BigDecimal vuelto;

    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;

    @Column(name = "numero_operacion", length = 40)
    private String numeroOperacion;

    @Column(name = "banco_origen", length = 150)
    private String bancoOrigen;

    @Column(name = "canal", length = 50)
    private String canal;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "validado_por", length = 100)
    private String validadoPor;
}
