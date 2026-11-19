package com.example.alquiler_de_vehiculos.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.config.EmpresaProperties;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO.FilaMonto;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO.FilaSolicitud;

/**
 * Exporta los reportes de Paul Car's a Excel (.xlsx) con formato corporativo:
 * encabezado de empresa, filtros aplicados, tabla con filtros, totales y hoja de resumen.
 */
@Service
public class ReporteExcelService {

    private final EmpresaProperties empresa;

    public ReporteExcelService(EmpresaProperties empresa) {
        this.empresa = empresa;
    }

    private static final int[] NAVY = { 0x0D, 0x1B, 0x3E };
    private static final int[] GOLD = { 0xC9, 0xA2, 0x27 };
    private static final int[] BLANCO = { 0xFF, 0xFF, 0xFF };
    private static final int[] GRIS = { 0xF1, 0xF5, 0xF9 };
    private static final int[] TEXTO = { 0x1E, 0x29, 0x3B };
    private static final int[] VERDE_F = { 0xDC, 0xFC, 0xE7 };
    private static final int[] VERDE_T = { 0x16, 0x65, 0x34 };
    private static final int[] AMBAR_F = { 0xFE, 0xF3, 0xC7 };
    private static final int[] AMBAR_T = { 0x92, 0x40, 0x0E };
    private static final int[] AZUL_F = { 0xDB, 0xEA, 0xFE };
    private static final int[] AZUL_T = { 0x1D, 0x4E, 0xD8 };
    private static final int[] ROJO_F = { 0xFE, 0xE2, 0xE2 };
    private static final int[] ROJO_T = { 0x99, 0x1B, 0x1B };
    private static final int[] GRIS_F = { 0xE2, 0xE8, 0xF0 };
    private static final int[] GRIS_T = { 0x47, 0x55, 0x69 };

    private static final String FMT_MONEDA = "\"S/\" #,##0.00";
    private static final String FMT_FECHA_HORA = "dd/mm/yyyy hh:mm";
    private static final DateTimeFormatter F_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ───────────────────────── Reporte de solicitudes ─────────────────────────

    public byte[] solicitudes(ReporteSolicitudesDTO r) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(wb);
            hojaDetalle(wb, e, r);
            hojaPagos(wb, e, r);
            hojaResumenSolicitudes(wb, e, r);
            wb.setActiveSheet(0);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el Excel: " + ex.getMessage(), ex);
        }
    }

    private void hojaDetalle(XSSFWorkbook wb, Estilos e, ReporteSolicitudesDTO r) {
        Sheet sh = wb.createSheet("Solicitudes");
        String[] cab = { "N° Contrato", "Registrado (fecha y hora)", "Cliente", "DNI", "Vehículo", "Placa", "Categoría", "Sede",
                "Recojo pactado", "Recojo real", "Diferencia de recojo",
                "Devolución pactada", "Devolución real", "Diferencia de devolución",
                "Días", "Estado", "Estado de pago", "Método de pago",
                "Monto total (S/)", "Pagado (S/)", "Saldo (S/)",
                "Llaves entregadas por", "Km de salida", "Combustible de salida", "Atendido por", "Último cambio de estado" };
        int[] anchos = { 14, 19, 28, 12, 26, 11, 13, 14, 18, 18, 18, 18, 18, 18, 7, 23, 14, 22, 15, 15, 15, 22, 12, 16, 22, 20 };
        int ultima = cab.length - 1;

        encabezadoCorporativo(sh, e, "REPORTE DE SOLICITUDES Y ALQUILERES", ultima);
        textoFila(sh, 2, ultima, "Periodo de registro: " + r.desde().format(F_FECHA) + " al " + r.hasta().format(F_FECHA)
                + "      |      Generado el: " + r.generadoEn().format(F_HORA) + "      |      Generado por: " + r.generadoPor(),
                e.info());
        String filtros = "Filtros aplicados  ->  Estado: " + r.estado() + "   |   Sede: " + r.sede()
                + "   |   Categoría: " + r.categoria()
                + (r.filtrosExtra() != null ? r.filtrosExtra() : "")
                + (r.busqueda() != null && !r.busqueda().isBlank() ? "   |   Búsqueda: \"" + r.busqueda() + "\"" : "");
        textoFila(sh, 3, ultima, filtros, e.info());
        sh.createRow(4).setHeightInPoints(6);

        int filaCab = 5;
        Row rc = sh.createRow(filaCab);
        rc.setHeightInPoints(32);
        for (int i = 0; i < cab.length; i++) {
            Cell c = rc.createCell(i);
            c.setCellValue(cab[i]);
            c.setCellStyle(e.cabecera());
            sh.setColumnWidth(i, anchos[i] * 256);
        }

        int fila = filaCab + 1;
        boolean zebra = false;
        for (FilaSolicitud f : r.filas()) {
            Row row = sh.createRow(fila++);
            row.setHeightInPoints(20);
            int i = 0;
            texto(row, i++, f.codigo(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            fechaHora(row, i++, f.fechaRegistro(), e.celda(zebra, HorizontalAlignment.CENTER, FMT_FECHA_HORA));
            texto(row, i++, f.cliente(), e.celda(zebra, HorizontalAlignment.LEFT, null));
            texto(row, i++, f.documento(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            texto(row, i++, f.vehiculo(), e.celda(zebra, HorizontalAlignment.LEFT, null));
            texto(row, i++, f.placa() == null || f.placa().isBlank() ? "Por asignar" : f.placa(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            texto(row, i++, f.categoria(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            texto(row, i++, f.sede(), e.celda(zebra, HorizontalAlignment.CENTER, null));

            // Recojo: hora pactada del plan vs hora real en que el cliente recogió el vehículo
            fechaHora(row, i++, pactada(f.fechaRecojo(), f.horaRecojo()), e.celda(zebra, HorizontalAlignment.CENTER, FMT_FECHA_HORA));
            realOPendiente(row, i++, f.fechaEntrega(), "Sin recoger", zebra, e);
            texto(row, i++, textoDiferencia(f.difRecojoMin()), e.diferencia(f.difRecojoMin()));

            // Devolución: hora pactada del plan vs hora real en que el cliente devolvió el vehículo
            fechaHora(row, i++, pactada(f.fechaDevolucion(), f.horaDevolucion()), e.celda(zebra, HorizontalAlignment.CENTER, FMT_FECHA_HORA));
            realOPendiente(row, i++, f.fechaDevolucionReal(), "Sin devolver", zebra, e);
            texto(row, i++, textoDiferencia(f.difDevolucionMin()), e.diferencia(f.difDevolucionMin()));

            numero(row, i++, f.dias(), e.celda(zebra, HorizontalAlignment.CENTER, "0"));
            texto(row, i++, f.estado(), e.estadoGrupo(f.grupoEstado()));
            texto(row, i++, f.estadoPago(), e.estadoPago(f.estadoPago()));
            texto(row, i++, f.metodosPago(), e.celda(zebra, HorizontalAlignment.LEFT, null));
            dinero(row, i++, f.montoTotal(), e.celda(zebra, HorizontalAlignment.RIGHT, FMT_MONEDA));
            dinero(row, i++, f.montoPagado(), e.celda(zebra, HorizontalAlignment.RIGHT, FMT_MONEDA));
            dinero(row, i++, f.saldo(), e.celda(zebra, HorizontalAlignment.RIGHT, FMT_MONEDA));

            // Entrega de llaves: quién la hizo y con qué kilometraje y combustible salió la unidad
            boolean entregado = f.fechaEntrega() != null;
            texto(row, i++, entregado ? (f.entregadoPor() != null && !f.entregadoPor().isBlank() ? f.entregadoPor() : "Counter") : "Pendiente",
                    e.celda(zebra, HorizontalAlignment.LEFT, null));
            if (entregado && f.kmEntrega() != null) {
                numero(row, i++, f.kmEntrega(), e.celda(zebra, HorizontalAlignment.RIGHT, "#,##0 \"km\""));
            } else {
                texto(row, i++, "-", e.celda(zebra, HorizontalAlignment.CENTER, null));
            }
            texto(row, i++, entregado && f.combustibleEntrega() != null ? f.combustibleEntrega() : "-",
                    e.celda(zebra, HorizontalAlignment.CENTER, null));
            texto(row, i++, f.atendidoPor() != null ? f.atendidoPor() : "-", e.celda(zebra, HorizontalAlignment.LEFT, null));
            fechaHora(row, i, f.fechaUltimoEstado(), e.celda(zebra, HorizontalAlignment.CENTER, FMT_FECHA_HORA));
            zebra = !zebra;
        }

        if (r.filas().isEmpty()) {
            Row vacio = sh.createRow(fila++);
            vacio.setHeightInPoints(26);
            Cell c = vacio.createCell(0);
            c.setCellValue("No se registraron solicitudes con los filtros seleccionados.");
            c.setCellStyle(e.info());
            sh.addMergedRegion(new CellRangeAddress(vacio.getRowNum(), vacio.getRowNum(), 0, ultima));
        } else {
            Row tot = sh.createRow(fila);
            tot.setHeightInPoints(24);
            for (int i = 0; i <= ultima; i++) {
                tot.createCell(i).setCellStyle(e.total(null));
            }
            tot.getCell(0).setCellValue("TOTAL");
            tot.getCell(1).setCellValue(r.filas().size() + " solicitud(es)");
            sh.addMergedRegion(new CellRangeAddress(fila, fila, 1, 3));
            tot.getCell(18).setCellValue(r.resumen().montoContratado().doubleValue());
            tot.getCell(18).setCellStyle(e.total(FMT_MONEDA));
            tot.getCell(19).setCellValue(r.resumen().montoCobrado().doubleValue());
            tot.getCell(19).setCellStyle(e.total(FMT_MONEDA));
            tot.getCell(20).setCellValue(r.resumen().saldoPorCobrar().doubleValue());
            tot.getCell(20).setCellStyle(e.total(FMT_MONEDA));
            sh.setAutoFilter(new CellRangeAddress(filaCab, fila - 1, 0, ultima));
        }

        sh.createFreezePane(3, filaCab + 1);
        configurarImpresion(sh, filaCab);
    }

    private void hojaResumenSolicitudes(XSSFWorkbook wb, Estilos e, ReporteSolicitudesDTO r) {
        Sheet sh = wb.createSheet("Resumen");
        int[] anchos = { 34, 18, 18, 18 };
        for (int i = 0; i < anchos.length; i++) sh.setColumnWidth(i, anchos[i] * 256);

        encabezadoCorporativo(sh, e, "RESUMEN DEL REPORTE DE SOLICITUDES", 3);
        textoFila(sh, 2, 3, "Periodo: " + r.desde().format(F_FECHA) + " al " + r.hasta().format(F_FECHA)
                + "   |   Generado por: " + r.generadoPor(), e.info());

        int fila = 4;
        fila = tituloSeccion(sh, e, fila, "Indicadores generales", 3);
        var res = r.resumen();
        fila = filaKpi(sh, e, fila, "Total de solicitudes registradas", res.total(), false);
        fila = filaKpi(sh, e, fila, "Pendientes de aprobación", res.pendientes(), false);
        fila = filaKpi(sh, e, fila, "Reservadas - por pagar", res.porPagar(), false);
        fila = filaKpi(sh, e, fila, "Reservadas - pagadas", res.pagadas(), false);
        fila = filaKpi(sh, e, fila, "En ruta (vehículo entregado)", res.enRuta(), false);
        fila = filaKpi(sh, e, fila, "Finalizadas", res.finalizadas(), false);
        fila = filaKpi(sh, e, fila, "Anuladas (canceladas / rechazadas)", res.anuladas(), false);
        fila = filaKpi(sh, e, fila, "Monto contratado (S/)", res.montoContratado(), true);
        fila = filaKpi(sh, e, fila, "Monto cobrado (S/)", res.montoCobrado(), true);
        fila = filaKpi(sh, e, fila, "Saldo por cobrar (S/)", res.saldoPorCobrar(), true);

        fila++;
        fila = tablaMontos(sh, e, fila, "Solicitudes por estado", "Estado", "Solicitudes", "Monto contratado (S/)", r.porEstado());
        fila++;
        fila = tablaMontos(sh, e, fila, "Solicitudes por sede de recojo", "Sede", "Solicitudes", "Monto contratado (S/)", r.porSede());
        fila++;
        fila = tablaMontos(sh, e, fila, "Cobros por método de pago", "Método", "Operaciones", "Monto cobrado (S/)", r.porMetodoPago());

        fila++;
        fila = tituloSeccion(sh, e, fila, "Vehículos más solicitados", 3);
        Row cab = sh.createRow(fila++);
        String[] t = { "Vehículo", "Categoría", "Solicitudes", "Monto contratado (S/)" };
        for (int i = 0; i < t.length; i++) {
            Cell c = cab.createCell(i);
            c.setCellValue(t[i]);
            c.setCellStyle(e.cabecera());
        }
        boolean zebra = false;
        for (var v : r.vehiculosSolicitados()) {
            Row row = sh.createRow(fila++);
            texto(row, 0, v.vehiculo(), e.celda(zebra, HorizontalAlignment.LEFT, null));
            texto(row, 1, v.categoria(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            numero(row, 2, v.solicitudes(), e.celda(zebra, HorizontalAlignment.CENTER, "0"));
            dinero(row, 3, v.monto(), e.celda(zebra, HorizontalAlignment.RIGHT, FMT_MONEDA));
            zebra = !zebra;
        }
        sh.getPrintSetup().setLandscape(false);
        sh.setFitToPage(true);
        sh.getPrintSetup().setFitWidth((short) 1);
        sh.getPrintSetup().setFitHeight((short) 0);
        sh.setDisplayGridlines(false);
    }

    // ───────────────────────── Reporte ejecutivo ─────────────────────────

    public byte[] ejecutivo(ReporteEjecutivoDTO r) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(wb);

            Sheet sh = wb.createSheet("Resumen ejecutivo");
            int[] anchos = { 36, 20, 20, 20 };
            for (int i = 0; i < anchos.length; i++) sh.setColumnWidth(i, anchos[i] * 256);
            encabezadoCorporativo(sh, e, "REPORTE EJECUTIVO DE GESTIÓN", 3);
            textoFila(sh, 2, 3, "Periodo: " + r.desde().format(F_FECHA) + " al " + r.hasta().format(F_FECHA)
                    + "   |   Sede: " + r.sede() + "   |   Categoría: " + r.categoria()
                    + ("TODOS".equals(r.vehiculo()) ? "" : "   |   Vehículo: " + r.vehiculo()), e.info());
            int fila = 4;
            fila = tituloSeccion(sh, e, fila, "Indicadores principales", 3);
            fila = filaKpi(sh, e, fila, "Ingresos totales del periodo (S/)", r.ingresosTotales(), true);
            fila = filaKpiTexto(sh, e, fila, "Ocupación de flota", r.ocupacionFlota().toPlainString() + " %");
            fila = filaKpi(sh, e, fila, "Ticket promedio por alquiler (S/)", r.ticketPromedio(), true);
            fila = filaKpi(sh, e, fila, "Contratos iniciados", r.alquileresEnPeriodo(), false);
            fila = filaKpi(sh, e, fila, "Contratos finalizados", r.alquileresFinalizados(), false);
            fila = filaKpi(sh, e, fila, "Total de días alquilados", r.diasTotalesAlquilados(), false);
            fila++;
            fila = tablaMontos(sh, e, fila, "Ingresos por sede", "Sede", "Operaciones", "Total (S/)", r.ingresosPorSede());
            fila++;
            fila = tablaMontos(sh, e, fila, "Ingresos por categoría", "Categoría", "Operaciones", "Total (S/)", r.ingresosPorCategoria());
            fila++;
            tablaMontos(sh, e, fila, "Arqueo de caja por método de pago", "Método", "Operaciones", "Total (S/)", r.ingresosPorMetodo());
            sh.setDisplayGridlines(false);
            sh.setFitToPage(true);
            sh.getPrintSetup().setFitWidth((short) 1);
            sh.getPrintSetup().setFitHeight((short) 0);

            Sheet top = wb.createSheet("Rentabilidad por vehículo");
            String[] cab = { "#", "Vehículo", "Placa", "Categoría", "Alquileres", "Días alquilados", "Ingresos (S/)", "Utilización (%)" };
            int[] an2 = { 6, 30, 14, 14, 12, 16, 16, 16 };
            encabezadoCorporativo(top, e, "RENTABILIDAD POR VEHÍCULO", cab.length - 1);
            textoFila(top, 2, cab.length - 1, "Periodo: " + r.desde().format(F_FECHA) + " al " + r.hasta().format(F_FECHA), e.info());
            Row rc = top.createRow(4);
            rc.setHeightInPoints(28);
            for (int i = 0; i < cab.length; i++) {
                Cell c = rc.createCell(i);
                c.setCellValue(cab[i]);
                c.setCellStyle(e.cabecera());
                top.setColumnWidth(i, an2[i] * 256);
            }
            int f2 = 5;
            boolean zebra = false;
            int n = 1;
            for (var v : r.topVehiculos()) {
                Row row = top.createRow(f2++);
                numero(row, 0, n++, e.celda(zebra, HorizontalAlignment.CENTER, "0"));
                texto(row, 1, v.vehiculo(), e.celda(zebra, HorizontalAlignment.LEFT, null));
                texto(row, 2, v.placa(), e.celda(zebra, HorizontalAlignment.CENTER, null));
                texto(row, 3, v.categoria(), e.celda(zebra, HorizontalAlignment.CENTER, null));
                numero(row, 4, v.alquileres(), e.celda(zebra, HorizontalAlignment.CENTER, "0"));
                numero(row, 5, v.diasAlquilados(), e.celda(zebra, HorizontalAlignment.CENTER, "0"));
                dinero(row, 6, v.ingresos(), e.celda(zebra, HorizontalAlignment.RIGHT, FMT_MONEDA));
                numero(row, 7, v.utilizacion().doubleValue(), e.celda(zebra, HorizontalAlignment.CENTER, "0.0\"%\""));
                zebra = !zebra;
            }
            top.createFreezePane(0, 5);
            if (f2 > 5) top.setAutoFilter(new CellRangeAddress(4, f2 - 1, 0, cab.length - 1));
            configurarImpresion(top, 4);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el Excel: " + ex.getMessage(), ex);
        }
    }

    // ───────────────────────── Bloques reutilizables ─────────────────────────

    private void encabezadoCorporativo(Sheet sh, Estilos e, String titulo, int ultimaCol) {
        Row r0 = sh.createRow(0);
        r0.setHeightInPoints(30);
        for (int i = 0; i <= ultimaCol; i++) r0.createCell(i).setCellStyle(e.marca());
        r0.getCell(0).setCellValue(empresa.getRazonSocial() + "   |   " + empresa.rucTexto());
        sh.addMergedRegion(new CellRangeAddress(0, 0, 0, ultimaCol));

        Row r1 = sh.createRow(1);
        r1.setHeightInPoints(26);
        for (int i = 0; i <= ultimaCol; i++) r1.createCell(i).setCellStyle(e.titulo());
        r1.getCell(0).setCellValue(titulo);
        sh.addMergedRegion(new CellRangeAddress(1, 1, 0, ultimaCol));
    }

    private void textoFila(Sheet sh, int fila, int ultimaCol, String texto, XSSFCellStyle estilo) {
        Row r = sh.createRow(fila);
        r.setHeightInPoints(20);
        for (int i = 0; i <= ultimaCol; i++) r.createCell(i).setCellStyle(estilo);
        r.getCell(0).setCellValue(texto);
        sh.addMergedRegion(new CellRangeAddress(fila, fila, 0, ultimaCol));
    }

    private int tituloSeccion(Sheet sh, Estilos e, int fila, String texto, int ultimaCol) {
        Row r = sh.createRow(fila);
        r.setHeightInPoints(22);
        for (int i = 0; i <= ultimaCol; i++) r.createCell(i).setCellStyle(e.seccion());
        r.getCell(0).setCellValue(texto.toUpperCase());
        sh.addMergedRegion(new CellRangeAddress(fila, fila, 0, ultimaCol));
        return fila + 1;
    }

    private int filaKpi(Sheet sh, Estilos e, int fila, String etiqueta, Number valor, boolean moneda) {
        Row r = sh.createRow(fila);
        r.setHeightInPoints(20);
        Cell k = r.createCell(0);
        k.setCellValue(etiqueta);
        k.setCellStyle(e.celda(false, HorizontalAlignment.LEFT, null));
        Cell v = r.createCell(1);
        v.setCellValue(valor.doubleValue());
        v.setCellStyle(moneda ? e.kpi(FMT_MONEDA) : e.kpi("#,##0"));
        return fila + 1;
    }

    private int filaKpiTexto(Sheet sh, Estilos e, int fila, String etiqueta, String valor) {
        Row r = sh.createRow(fila);
        r.setHeightInPoints(20);
        Cell k = r.createCell(0);
        k.setCellValue(etiqueta);
        k.setCellStyle(e.celda(false, HorizontalAlignment.LEFT, null));
        Cell v = r.createCell(1);
        v.setCellValue(valor);
        v.setCellStyle(e.kpi(null));
        return fila + 1;
    }

    private int tablaMontos(Sheet sh, Estilos e, int fila, String titulo, String col1, String col2, String col3,
                            List<FilaMonto> filas) {
        fila = tituloSeccion(sh, e, fila, titulo, 3);
        Row cab = sh.createRow(fila++);
        String[] t = { col1, col2, col3 };
        for (int i = 0; i < t.length; i++) {
            Cell c = cab.createCell(i);
            c.setCellValue(t[i]);
            c.setCellStyle(e.cabecera());
        }
        boolean zebra = false;
        for (FilaMonto m : filas) {
            Row row = sh.createRow(fila++);
            texto(row, 0, m.etiqueta(), e.celda(zebra, HorizontalAlignment.LEFT, null));
            numero(row, 1, m.cantidad(), e.celda(zebra, HorizontalAlignment.CENTER, "0"));
            dinero(row, 2, m.total(), e.celda(zebra, HorizontalAlignment.RIGHT, FMT_MONEDA));
            zebra = !zebra;
        }
        if (filas.isEmpty()) {
            Row row = sh.createRow(fila++);
            texto(row, 0, "Sin registros en el periodo", e.celda(false, HorizontalAlignment.LEFT, null));
        }
        return fila;
    }

    private void configurarImpresion(Sheet sh, int filaCabecera) {
        sh.setFitToPage(true);
        PrintSetup ps = sh.getPrintSetup();
        ps.setLandscape(true);
        ps.setPaperSize(PrintSetup.A4_PAPERSIZE);
        ps.setFitWidth((short) 1);
        ps.setFitHeight((short) 0);
        sh.setRepeatingRows(new CellRangeAddress(filaCabecera, filaCabecera, -1, -1));
        sh.getFooter().setLeft("Paul Car's - Alquiler de Vehículos");
        sh.getFooter().setRight("Página &P de &N");
        sh.setDisplayGridlines(false);
    }

    private void texto(Row row, int col, String valor, XSSFCellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(valor == null ? "" : valor);
        c.setCellStyle(estilo);
    }

    private void numero(Row row, int col, double valor, XSSFCellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private void dinero(Row row, int col, BigDecimal valor, XSSFCellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(valor == null ? 0d : valor.doubleValue());
        c.setCellStyle(estilo);
    }

    private void fechaHora(Row row, int col, LocalDateTime valor, XSSFCellStyle estilo) {
        Cell c = row.createCell(col);
        if (valor != null) c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private LocalDateTime pactada(LocalDate fecha, String hora) {
        if (fecha == null) return null;
        if (hora != null && !hora.isBlank()) {
            try {
                return fecha.atTime(java.time.LocalTime.parse(hora.trim()));
            } catch (java.time.format.DateTimeParseException ignorada) {
                // hora con formato inesperado: se muestra solo la fecha
            }
        }
        return fecha.atStartOfDay();
    }

    private void realOPendiente(Row row, int col, LocalDateTime real, String pendiente, boolean zebra, Estilos e) {
        if (real != null) {
            fechaHora(row, col, real, e.celda(zebra, HorizontalAlignment.CENTER, FMT_FECHA_HORA));
        } else {
            texto(row, col, pendiente, e.celda(zebra, HorizontalAlignment.CENTER, null));
        }
    }

    /** "A tiempo", "+20 min tarde" o "15 min antes". Tolerancia de 5 minutos. */
    private String textoDiferencia(Long minutos) {
        if (minutos == null) return "-";
        long abs = Math.abs(minutos);
        if (abs <= 5) return "A tiempo";
        String duracion = abs >= 60 ? (abs / 60) + " h " + (abs % 60 == 0 ? "" : (abs % 60) + " min") : abs + " min";
        return minutos > 0 ? "+" + duracion.trim() + " tarde" : duracion.trim() + " antes";
    }

    private void hojaPagos(XSSFWorkbook wb, Estilos e, ReporteSolicitudesDTO r) {
        Sheet sh = wb.createSheet("Pagos");
        String[] cab = { "N° Recibo", "Fecha y hora del pago", "N° Contrato", "Cliente", "Método de pago", "Canal", "N° de operación", "Monto (S/)" };
        int[] anchos = { 14, 20, 14, 30, 20, 14, 18, 16 };
        int ultima = cab.length - 1;

        encabezadoCorporativo(sh, e, "DETALLE DE PAGOS DEL PERIODO", ultima);
        textoFila(sh, 2, ultima, "Periodo de registro: " + r.desde().format(F_FECHA) + " al " + r.hasta().format(F_FECHA)
                + "      |      Generado por: " + r.generadoPor(), e.info());
        sh.createRow(3).setHeightInPoints(6);

        int filaCab = 4;
        Row rc = sh.createRow(filaCab);
        rc.setHeightInPoints(28);
        for (int i = 0; i < cab.length; i++) {
            Cell c = rc.createCell(i);
            c.setCellValue(cab[i]);
            c.setCellStyle(e.cabecera());
            sh.setColumnWidth(i, anchos[i] * 256);
        }

        int fila = filaCab + 1;
        boolean zebra = false;
        BigDecimal total = BigDecimal.ZERO;
        for (var p : r.pagos()) {
            Row row = sh.createRow(fila++);
            row.setHeightInPoints(20);
            texto(row, 0, p.recibo(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            fechaHora(row, 1, p.fechaHora(), e.celda(zebra, HorizontalAlignment.CENTER, FMT_FECHA_HORA));
            texto(row, 2, p.codigoContrato(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            texto(row, 3, p.cliente(), e.celda(zebra, HorizontalAlignment.LEFT, null));
            texto(row, 4, p.metodo(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            texto(row, 5, p.canal(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            texto(row, 6, p.operacion(), e.celda(zebra, HorizontalAlignment.CENTER, null));
            dinero(row, 7, p.monto(), e.celda(zebra, HorizontalAlignment.RIGHT, FMT_MONEDA));
            total = total.add(p.monto() != null ? p.monto() : BigDecimal.ZERO);
            zebra = !zebra;
        }

        if (r.pagos().isEmpty()) {
            Row vacio = sh.createRow(fila);
            vacio.setHeightInPoints(26);
            Cell c = vacio.createCell(0);
            c.setCellValue("No hay pagos registrados para las solicitudes de este reporte.");
            c.setCellStyle(e.info());
            sh.addMergedRegion(new CellRangeAddress(fila, fila, 0, ultima));
        } else {
            Row tot = sh.createRow(fila);
            tot.setHeightInPoints(24);
            for (int i = 0; i <= ultima; i++) tot.createCell(i).setCellStyle(e.total(null));
            tot.getCell(0).setCellValue("TOTAL COBRADO (" + r.pagos().size() + " pagos)");
            sh.addMergedRegion(new CellRangeAddress(fila, fila, 0, 6));
            tot.getCell(7).setCellValue(total.doubleValue());
            tot.getCell(7).setCellStyle(e.total(FMT_MONEDA));
            sh.setAutoFilter(new CellRangeAddress(filaCab, fila - 1, 0, ultima));
        }
        sh.createFreezePane(0, filaCab + 1);
        configurarImpresion(sh, filaCab);
    }

    // ───────────────────────── Estilos ─────────────────────────

    private static final class Estilos {
        private final XSSFWorkbook wb;
        private final Map<String, XSSFCellStyle> cache = new HashMap<>();

        Estilos(XSSFWorkbook wb) {
            this.wb = wb;
        }

        XSSFCellStyle marca() {
            return crear("marca", NAVY, GOLD, true, 16, HorizontalAlignment.LEFT, null, false, false);
        }

        XSSFCellStyle titulo() {
            return crear("titulo", NAVY, BLANCO, true, 13, HorizontalAlignment.LEFT, null, false, false);
        }

        XSSFCellStyle info() {
            return crear("info", GRIS, GRIS_T, false, 10, HorizontalAlignment.LEFT, null, false, false);
        }

        XSSFCellStyle seccion() {
            return crear("seccion", GOLD, NAVY, true, 11, HorizontalAlignment.LEFT, null, false, false);
        }

        XSSFCellStyle cabecera() {
            return crear("cabecera", NAVY, BLANCO, true, 10, HorizontalAlignment.CENTER, null, true, true);
        }

        XSSFCellStyle celda(boolean zebra, HorizontalAlignment h, String formato) {
            return crear("celda" + zebra + h + formato, zebra ? GRIS : BLANCO, TEXTO, false, 10, h, formato, true, false);
        }

        XSSFCellStyle kpi(String formato) {
            return crear("kpi" + formato, BLANCO, NAVY, true, 11, HorizontalAlignment.RIGHT, formato, true, false);
        }

        XSSFCellStyle total(String formato) {
            return crear("total" + formato, GOLD, NAVY, true, 11, HorizontalAlignment.RIGHT, formato, true, false);
        }

        XSSFCellStyle estadoGrupo(String grupo) {
            return switch (grupo) {
                case "PAGADO", "FINALIZADO" -> crear("eg-verde", VERDE_F, VERDE_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
                case "POR_PAGAR", "PENDIENTE" -> crear("eg-ambar", AMBAR_F, AMBAR_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
                case "EN_RUTA" -> crear("eg-azul", AZUL_F, AZUL_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
                default -> crear("eg-rojo", ROJO_F, ROJO_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
            };
        }

        XSSFCellStyle diferencia(Long minutos) {
            if (minutos == null) return crear("df-gris", GRIS_F, GRIS_T, false, 10, HorizontalAlignment.CENTER, null, true, false);
            if (Math.abs(minutos) <= 5) return crear("df-verde", VERDE_F, VERDE_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
            if (minutos > 0) return crear("df-rojo", ROJO_F, ROJO_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
            return crear("df-azul", AZUL_F, AZUL_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
        }

        XSSFCellStyle estadoPago(String estado) {
            return switch (estado) {
                case "Pagado" -> crear("ep-verde", VERDE_F, VERDE_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
                case "Pago parcial", "Por pagar" -> crear("ep-ambar", AMBAR_F, AMBAR_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
                case "Anulado" -> crear("ep-rojo", ROJO_F, ROJO_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
                default -> crear("ep-gris", GRIS_F, GRIS_T, true, 10, HorizontalAlignment.CENTER, null, true, false);
            };
        }

        private XSSFCellStyle crear(String clave, int[] fondo, int[] texto, boolean negrita, double tamano,
                                    HorizontalAlignment h, String formato, boolean borde, boolean ajustar) {
            return cache.computeIfAbsent(clave, k -> {
                XSSFCellStyle s = wb.createCellStyle();
                s.setFillForegroundColor(color(fondo));
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                XSSFFont f = wb.createFont();
                f.setFontName("Calibri");
                f.setBold(negrita);
                f.setFontHeightInPoints((short) Math.round(tamano));
                f.setColor(color(texto));
                s.setFont(f);
                s.setAlignment(h);
                s.setVerticalAlignment(VerticalAlignment.CENTER);
                s.setWrapText(ajustar);
                if (formato != null) {
                    s.setDataFormat(wb.createDataFormat().getFormat(formato));
                }
                if (borde) {
                    s.setBorderBottom(BorderStyle.THIN);
                    s.setBorderTop(BorderStyle.THIN);
                    s.setBorderLeft(BorderStyle.THIN);
                    s.setBorderRight(BorderStyle.THIN);
                    XSSFColor gris = color(new int[] { 0xCB, 0xD5, 0xE1 });
                    s.setBottomBorderColor(gris);
                    s.setTopBorderColor(gris);
                    s.setLeftBorderColor(gris);
                    s.setRightBorderColor(gris);
                }
                if (h == HorizontalAlignment.LEFT && !borde) {
                    s.setIndention((short) 1);
                }
                return s;
            });
        }

        private static XSSFColor color(int[] rgb) {
            return new XSSFColor(new byte[] { (byte) rgb[0], (byte) rgb[1], (byte) rgb[2] }, null);
        }
    }
}
