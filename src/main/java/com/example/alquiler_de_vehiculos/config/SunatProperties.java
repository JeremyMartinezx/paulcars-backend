package com.example.alquiler_de_vehiculos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Facturación electrónica SUNAT mediante un Proveedor de Servicios Electrónicos (PSE) autorizado.
 * Se usa la API JSON de Nubefact: sin URL y token se trabaja en modo DEMO (documento de demostración,
 * sin validez tributaria). Con URL y token se emite la boleta real y se envía a SUNAT.
 */
@Component
@ConfigurationProperties(prefix = "sunat")
public class SunatProperties {

    /** Serie de las boletas de venta electrónicas (debe empezar con B y tener 4 caracteres). */
    private String serieBoleta = "B001";
    /** Serie de las notas de crédito electrónicas que modifican boletas. */
    private String serieNotaCredito = "BC01";
    /** Porcentaje de IGV (18 en Perú). */
    private int igv = 18;
    /** Ruta (URL) de la API de Nubefact, la entrega el proveedor en su panel. */
    private String nubefactUrl = "";
    /** Token de la API de Nubefact. */
    private String nubefactToken = "";

    public String getSerieBoleta() { return serieBoleta; }
    public void setSerieBoleta(String serieBoleta) { this.serieBoleta = serieBoleta; }

    public String getSerieNotaCredito() { return serieNotaCredito; }
    public void setSerieNotaCredito(String serieNotaCredito) { this.serieNotaCredito = serieNotaCredito; }

    public int getIgv() { return igv; }
    public void setIgv(int igv) { this.igv = igv; }

    public String getNubefactUrl() { return nubefactUrl; }
    public void setNubefactUrl(String nubefactUrl) { this.nubefactUrl = nubefactUrl; }

    public String getNubefactToken() { return nubefactToken; }
    public void setNubefactToken(String nubefactToken) { this.nubefactToken = nubefactToken; }

    /** true cuando hay credenciales del PSE: las boletas se emiten realmente ante SUNAT. */
    public boolean emisionReal() {
        return nubefactUrl != null && !nubefactUrl.isBlank() && nubefactToken != null && !nubefactToken.isBlank();
    }
}
