package com.example.alquiler_de_vehiculos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Política de cancelación y reembolso de Paul Car's. Sigue el esquema habitual de las empresas de alquiler:
 * reembolso total con 48 horas o más de anticipación, parcial entre 24 y 48 horas, y sin reembolso con menos
 * de 24 horas o si el cliente no se presenta. Si la cancelación es por culpa de la empresa, siempre es total.
 */
@Component
@ConfigurationProperties(prefix = "politica")
public class PoliticaCancelacionProperties {

    /** Horas de anticipación (antes del recojo) desde las cuales se reembolsa el 100%. */
    private int horasReembolsoTotal = 48;
    /** Horas de anticipación desde las cuales se reembolsa el porcentaje parcial. */
    private int horasReembolsoParcial = 24;
    /** Porcentaje que se reembolsa entre las horas parcial y total. */
    private int porcentajeParcial = 50;

    /** Horas que tiene el cliente para pagar una reserva aprobada. */
    private int plazoPagoHoras = 24;
    /** La reserva sin pago vence, como muy tarde, estas horas antes del recojo. */
    private int horasAntesRecojoLimitePago = 2;
    /** Aunque el recojo esté muy cerca, el cliente siempre tiene al menos este tiempo para pagar tras la aprobación. */
    private int plazoMinimoPagoHoras = 1;
    /** Plazos de devolución que se le informan al cliente, según el método con que pagó. */
    private String plazoDevolucionTarjeta = "De 5 a 10 días hábiles, según su banco";
    private String plazoDevolucionBilletera = "Hasta 2 días hábiles, a la misma billetera";
    private String plazoDevolucionTransferencia = "Hasta 2 días hábiles, a la misma cuenta";
    private String plazoDevolucionEfectivo = "En efectivo, en nuestra sede";
    private String plazoDevolucionOtro = "Hasta 5 días hábiles";

    /** Días hábiles máximos en que llega la devolución a la cuenta del cliente, según el método (el efectivo es inmediato). */
    private int diasDevolucionTarjeta = 10;
    private int diasDevolucionBilletera = 2;
    private int diasDevolucionTransferencia = 2;
    private int diasDevolucionOtro = 5;

    /** Minutos antes de la hora pactada desde los cuales ya se puede entregar el vehículo. */
    private int entregaAnticipadaMinutos = 30;
    /** Minutos que se espera al cliente después de la hora pactada; pasado ese tiempo la reserva pagada se cancela como "No se presentó". */
    private int esperaClienteMinutos = 60;
    /** Una unidad disponible con una reserva que se recoge dentro de estas horas no puede enviarse a limpieza (ya está comprometida). */
    private int horasReservaBloqueaLimpieza = 24;
    /** Si una unidad sale de servicio (mantenimiento / no disponible), se reasignan las reservas que se recogen dentro de estos días. */
    private int diasReasignarPorBaja = 7;
    /**
     * Margen preventivo: al recibir un vehículo, si le faltan estos km o menos para su mantenimiento, pasa a mantenimiento
     * en vez de salir otra vez (un alquiler puede recorrer cientos de km y se pasaría del límite).
     */
    private int kmMargenMantenimiento = 500;
    /** Al entregar, el odómetro no puede superar al último kilometraje de la unidad en más de estos km (evita errores de digitación). */
    private int kmMaximoSaltoEntrega = 500;
    /** Al recibir, no se aceptan más de estos km recorridos por día de alquiler (evita errores de digitación). */
    private int kmMaximoPorDia = 1500;
    /** Si está activo, un proceso cancela solo las reservas pagadas cuyo cliente no se presentó dentro de la espera. */
    private boolean cancelarNoPresentadasAuto = true;

    /** Si está activo, un proceso cancela solo las reservas sin pago que vencieron el plazo. */
    private boolean cancelarVencidasAuto = false;

    public int getDiasReasignarPorBaja() { return diasReasignarPorBaja; }
    public void setDiasReasignarPorBaja(int v) { this.diasReasignarPorBaja = v; }

    public int getHorasReservaBloqueaLimpieza() { return horasReservaBloqueaLimpieza; }
    public void setHorasReservaBloqueaLimpieza(int v) { this.horasReservaBloqueaLimpieza = v; }

    /** Cargo (S/) por cada cuarto de tanque que falta respecto al nivel con que se entregó el vehículo. */
    private java.math.BigDecimal combustiblePorCuarto = new java.math.BigDecimal("25");
    /** Minutos de tolerancia después de la hora pactada de devolución antes de cobrar retraso. */
    private int retrasoToleranciaMinutos = 60;

    public java.math.BigDecimal getCombustiblePorCuarto() { return combustiblePorCuarto; }
    public void setCombustiblePorCuarto(java.math.BigDecimal v) { this.combustiblePorCuarto = v; }
    public int getRetrasoToleranciaMinutos() { return retrasoToleranciaMinutos; }
    public void setRetrasoToleranciaMinutos(int v) { this.retrasoToleranciaMinutos = v; }

    public int getKmMargenMantenimiento() { return kmMargenMantenimiento; }
    public void setKmMargenMantenimiento(int v) { this.kmMargenMantenimiento = v; }

    public int getKmMaximoSaltoEntrega() { return kmMaximoSaltoEntrega; }
    public void setKmMaximoSaltoEntrega(int v) { this.kmMaximoSaltoEntrega = v; }

    public int getKmMaximoPorDia() { return kmMaximoPorDia; }
    public void setKmMaximoPorDia(int v) { this.kmMaximoPorDia = v; }

    public int getEntregaAnticipadaMinutos() { return entregaAnticipadaMinutos; }
    public void setEntregaAnticipadaMinutos(int v) { this.entregaAnticipadaMinutos = v; }

    public int getEsperaClienteMinutos() { return esperaClienteMinutos; }
    public void setEsperaClienteMinutos(int v) { this.esperaClienteMinutos = v; }

    public boolean isCancelarNoPresentadasAuto() { return cancelarNoPresentadasAuto; }
    public void setCancelarNoPresentadasAuto(boolean v) { this.cancelarNoPresentadasAuto = v; }

    public int getHorasReembolsoTotal() { return horasReembolsoTotal; }
    public void setHorasReembolsoTotal(int v) { this.horasReembolsoTotal = v; }

    public int getHorasReembolsoParcial() { return horasReembolsoParcial; }
    public void setHorasReembolsoParcial(int v) { this.horasReembolsoParcial = v; }

    public int getPorcentajeParcial() { return porcentajeParcial; }
    public void setPorcentajeParcial(int v) { this.porcentajeParcial = v; }

    public int getPlazoPagoHoras() { return plazoPagoHoras; }
    public void setPlazoPagoHoras(int v) { this.plazoPagoHoras = v; }

    public int getHorasAntesRecojoLimitePago() { return horasAntesRecojoLimitePago; }
    public void setHorasAntesRecojoLimitePago(int v) { this.horasAntesRecojoLimitePago = v; }

    public int getPlazoMinimoPagoHoras() { return plazoMinimoPagoHoras; }
    public void setPlazoMinimoPagoHoras(int v) { this.plazoMinimoPagoHoras = v; }

    /**
     * Fecha límite para pagar una reserva aprobada: a las N horas de aprobada o X horas antes del recojo, lo que ocurra
     * primero; pero nunca menos del plazo mínimo desde la aprobación (si el recojo es inmediato paga al llegar).
     */
    public java.time.LocalDateTime vencimientoPago(java.time.LocalDateTime aprobada, java.time.LocalDateTime recojo) {
        java.time.LocalDateTime porPlazo = aprobada.plusHours(plazoPagoHoras);
        java.time.LocalDateTime porRecojo = recojo.minusHours(horasAntesRecojoLimitePago);
        java.time.LocalDateTime limite = porPlazo.isBefore(porRecojo) ? porPlazo : porRecojo;
        java.time.LocalDateTime minimo = aprobada.plusHours(plazoMinimoPagoHoras);
        return limite.isBefore(minimo) ? minimo : limite;
    }

    public String getPlazoDevolucionTarjeta() { return plazoDevolucionTarjeta; }
    public void setPlazoDevolucionTarjeta(String v) { this.plazoDevolucionTarjeta = v; }
    public String getPlazoDevolucionBilletera() { return plazoDevolucionBilletera; }
    public void setPlazoDevolucionBilletera(String v) { this.plazoDevolucionBilletera = v; }
    public String getPlazoDevolucionTransferencia() { return plazoDevolucionTransferencia; }
    public void setPlazoDevolucionTransferencia(String v) { this.plazoDevolucionTransferencia = v; }
    public String getPlazoDevolucionEfectivo() { return plazoDevolucionEfectivo; }
    public void setPlazoDevolucionEfectivo(String v) { this.plazoDevolucionEfectivo = v; }
    public String getPlazoDevolucionOtro() { return plazoDevolucionOtro; }
    public void setPlazoDevolucionOtro(String v) { this.plazoDevolucionOtro = v; }

    /** Plazo que se le informa al cliente según el método de pago original. */
    public String plazoDevolucion(String metodo) {
        String m = metodo == null ? "" : metodo.toUpperCase(java.util.Locale.ROOT);
        if (m.contains("EFECTIVO")) return plazoDevolucionEfectivo;
        if (m.contains("TARJETA")) return plazoDevolucionTarjeta;
        if (m.contains("YAPE") || m.contains("PLIN")) return plazoDevolucionBilletera;
        if (m.contains("TRANSFER")) return plazoDevolucionTransferencia;
        return plazoDevolucionOtro;
    }

    public int getDiasDevolucionTarjeta() { return diasDevolucionTarjeta; }
    public void setDiasDevolucionTarjeta(int v) { this.diasDevolucionTarjeta = v; }
    public int getDiasDevolucionBilletera() { return diasDevolucionBilletera; }
    public void setDiasDevolucionBilletera(int v) { this.diasDevolucionBilletera = v; }
    public int getDiasDevolucionTransferencia() { return diasDevolucionTransferencia; }
    public void setDiasDevolucionTransferencia(int v) { this.diasDevolucionTransferencia = v; }
    public int getDiasDevolucionOtro() { return diasDevolucionOtro; }
    public void setDiasDevolucionOtro(int v) { this.diasDevolucionOtro = v; }

    /** Días hábiles que tarda la devolución según el método de pago original. */
    public int diasHabilesDevolucion(String metodo) {
        String m = metodo == null ? "" : metodo.toUpperCase(java.util.Locale.ROOT);
        if (m.contains("EFECTIVO")) return 0;
        if (m.contains("TARJETA")) return diasDevolucionTarjeta;
        if (m.contains("YAPE") || m.contains("PLIN")) return diasDevolucionBilletera;
        if (m.contains("TRANSFER")) return diasDevolucionTransferencia;
        return diasDevolucionOtro;
    }

    /** Fecha en que, como máximo, llegará la devolución: se cuentan solo días hábiles (de lunes a viernes). */
    public java.time.LocalDate fechaEstimadaDevolucion(java.time.LocalDate desde, String metodo) {
        int pendientes = diasHabilesDevolucion(metodo);
        java.time.LocalDate f = desde;
        while (pendientes > 0) {
            f = f.plusDays(1);
            java.time.DayOfWeek d = f.getDayOfWeek();
            if (d != java.time.DayOfWeek.SATURDAY && d != java.time.DayOfWeek.SUNDAY) pendientes--;
        }
        return f;
    }

    public boolean isCancelarVencidasAuto() { return cancelarVencidasAuto; }
    public void setCancelarVencidasAuto(boolean v) { this.cancelarVencidasAuto = v; }
}
