package com.example.alquiler_de_vehiculos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Datos legales y de contacto de la empresa que aparecen en recibos, comprobantes y reportes.
 * Se configuran en application.properties (prefijo "empresa") o con variables de entorno.
 */
@Component
@ConfigurationProperties(prefix = "empresa")
public class EmpresaProperties {

    private String razonSocial = "PAUL CAR'S - ALQUILER DE VEHÍCULOS";
    private String nombreComercial = "Paul Car's";
    private String ruc = "";
    private String direccion = "Av. 28 de Julio 489, Miraflores, Lima";
    private String telefono = "+51 959 137 712";
    private String email = "info@paulcars.com";
    private String web = "www.paulcars.com";

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }

    public String getRuc() { return ruc; }
    public void setRuc(String ruc) { this.ruc = ruc; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWeb() { return web; }
    public void setWeb(String web) { this.web = web; }

    /** Texto listo para mostrar: "RUC 20123456789" o un aviso mientras no se configure. */
    public String rucTexto() {
        return ruc == null || ruc.isBlank() ? "RUC: pendiente de configurar" : "RUC " + ruc.trim();
    }

    /** Línea de contacto para pies de página y membretes. */
    public String lineaContacto() {
        return direccion + "  |  Tel. " + telefono + "  |  " + email;
    }
}
