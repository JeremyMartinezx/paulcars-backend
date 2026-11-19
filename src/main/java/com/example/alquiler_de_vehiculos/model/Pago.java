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
@jakarta.persistence.EntityListeners(com.example.alquiler_de_vehiculos.service.CambiosListener.class)
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

    // Momento exacto del cobro (fecha y hora); fecha_pago se mantiene por compatibilidad
    @Column(name = "fecha_hora_pago")
    private LocalDateTime fechaHoraPago;

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

    /** En una devolución (tipo REEMBOLSO), id del pago original que se devuelve. */
    @Column(name = "id_pago_referencia")
    private Integer idPagoReferencia;

    /** Asesor (o "Pago en línea") que registró el cobro. */
    @Column(name = "registrado_por", length = 150)
    private String registradoPor;

    /** Marca de la tarjeta cobrada en el POS: VISA, MASTERCARD, AMEX, DINERS u OTRA. */
    @Column(name = "marca_tarjeta", length = 20)
    private String marcaTarjeta;

    /** Número de autorización del voucher del POS. */
    @Column(name = "numero_autorizacion", length = 20)
    private String numeroAutorizacion;

    /** En una devolución (tipo REEMBOLSO): EN_PROCESO si el dinero aún no llegó al cliente, DEVUELTA si ya. */
    @Column(name = "estado_devolucion", length = 20)
    private String estadoDevolucion;

    @Column(name = "fecha_devuelta")
    private LocalDateTime fechaDevuelta;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "validado_por", length = 100)
    private String validadoPor;

    @jakarta.persistence.PrePersist
    void alRegistrarPago() {
        if (fechaHoraPago == null) {
            fechaHoraPago = LocalDateTime.now();
        }
        if (fechaPago == null) {
            fechaPago = fechaHoraPago.toLocalDate();
        }
    }

    // Pagos antiguos (sin hora) caen al inicio del día de pago
    public LocalDateTime getFechaHoraPago() {
        if (fechaHoraPago != null) return fechaHoraPago;
        return fechaPago != null ? fechaPago.atStartOfDay() : null;
    }
}
