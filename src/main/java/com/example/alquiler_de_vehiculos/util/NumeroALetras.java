package com.example.alquiler_de_vehiculos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Convierte un monto a su expresión en letras, como exige la boleta: "SON: TRESCIENTOS SESENTA CON 00/100 SOLES". */
public final class NumeroALetras {

    private static final String[] UNIDADES = { "", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIÚN", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE",
            "VEINTIOCHO", "VEINTINUEVE" };
    private static final String[] DECENAS = { "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA" };
    private static final String[] CENTENAS = { "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS" };

    private NumeroALetras() {
    }

    public static String soles(BigDecimal monto) {
        BigDecimal m = (monto != null ? monto : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        long enteros = m.longValue();
        int centimos = m.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
        String letras = enteros == 0 ? "CERO" : convertir(enteros);
        return "SON: " + letras + String.format(" CON %02d/100 SOLES", centimos);
    }

    private static String convertir(long n) {
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            long resto = n % 1_000_000;
            String pref = millones == 1 ? "UN MILLÓN" : convertir(millones) + " MILLONES";
            return resto == 0 ? pref : pref + " " + convertir(resto);
        }
        if (n >= 1000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String pref = miles == 1 ? "MIL" : convertir(miles) + " MIL";
            return resto == 0 ? pref : pref + " " + convertir(resto);
        }
        if (n == 100) return "CIEN";
        if (n >= 100) {
            long resto = n % 100;
            String c = CENTENAS[(int) (n / 100)];
            return resto == 0 ? c : c + " " + convertir(resto);
        }
        if (n < 30) return UNIDADES[(int) n];
        long unidad = n % 10;
        String d = DECENAS[(int) (n / 10)];
        return unidad == 0 ? d : d + " Y " + UNIDADES[(int) unidad];
    }
}
