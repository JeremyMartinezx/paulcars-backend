package com.example.alquiler_de_vehiculos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Boleta de venta electrónica emitida por un pago (una boleta por pago). */
@Entity
@Table(name = "boletas_electronicas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_boleta_pago", columnNames = "id_pago"),
        @UniqueConstraint(name = "uk_boleta_serie_numero", columnNames = { "serie", "correlativo" })
})
@Getter
@Setter
@NoArgsConstructor
public class BoletaElectronica {

    public static final String MODO_DEMO = "DEMO";
    public static final String MODO_REAL = "NUBEFACT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_boleta")
    private Integer idBoleta;

    @ManyToOne
    @JoinColumn(name = "id_pago", nullable = false)
    @JsonIgnore
    private Pago pago;

    @Column(name = "serie", nullable = false, length = 4)
    private String serie;

    @Column(name = "correlativo", nullable = false)
    private Integer correlativo;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "op_gravada", precision = 12, scale = 2)
    private BigDecimal opGravada;

    @Column(name = "igv", precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", precision = 12, scale = 2)
    private BigDecimal total;

    /** Tipo de documento del cliente según SUNAT: 1 DNI, 4 CE, 7 pasaporte, 0 sin documento. */
    @Column(name = "cliente_tipo_doc", length = 2)
    private String clienteTipoDoc;

    @Column(name = "cliente_num_doc", length = 20)
    private String clienteNumDoc;

    @Column(name = "cliente_nombre", length = 200)
    private String clienteNombre;

    /** DEMO (sin validez tributaria) o NUBEFACT (emitida ante SUNAT). */
    @Column(name = "modo", length = 12, nullable = false)
    private String modo;

    /** EMITIDA_DEMO, ACEPTADA o PENDIENTE_SUNAT. */
    @Column(name = "estado", length = 20, nullable = false)
    private String estado;

    @Column(name = "codigo_hash", length = 100)
    private String codigoHash;

    @Column(name = "cadena_qr", length = 500)
    private String cadenaQr;

    @Column(name = "enlace_pdf", length = 500)
    private String enlacePdf;

    @Column(name = "enlace_xml", length = 500)
    private String enlaceXml;

    @Column(name = "mensaje_sunat", length = 500)
    private String mensajeSunat;

    /** BOLETA o NOTA_CREDITO (los registros anteriores sin valor son boletas). */
    @Column(name = "tipo_comprobante", length = 15)
    private String tipoComprobante;

    /** En una nota de crédito, número de la boleta que modifica. */
    @Column(name = "documento_referencia", length = 20)
    private String documentoReferencia;

    @Column(name = "motivo_nota", length = 80)
    private String motivoNota;

    public boolean esNotaCredito() {
        return "NOTA_CREDITO".equals(tipoComprobante);
    }

    public String numero() {
        return String.format("%s-%08d", serie, correlativo != null ? correlativo : 0);
    }

    public boolean esDemo() {
        return MODO_DEMO.equals(modo);
    }
}
