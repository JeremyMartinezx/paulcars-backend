package com.example.alquiler_de_vehiculos.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.config.EmpresaProperties;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO.FilaSolicitud;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Reporte resumido y configurable: el administrador elige qué cuadros incluir (totales, por estado, por vehículo,
 * por método de pago, por sede y, si lo necesita, el detalle de contratos) y se genera en Excel o PDF.
 */
@Service
public class ReporteResumenService {

    public static final Set<String> SECCIONES = Set.of("totales", "estados", "vehiculos", "clientes", "metodos", "sedes", "detalle");

    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color NAVY = new Color(0x0D, 0x1B, 0x3E);

    private final EmpresaProperties empresa;

    public ReporteResumenService(EmpresaProperties empresa) {
        this.empresa = empresa;
    }

    /** Un cuadro del reporte; las columnas numéricas se alinean a la derecha. */
    private record Cuadro(String titulo, List<String> cabeceras, List<String[]> filas, boolean ultimaFilaTotal, int primerasColumnasTexto) {}

    // ───────────────────────── Contenido ─────────────────────────

    private static final Set<String> CON_PAGO = Set.of("PAGADO", "EN_RUTA", "FINALIZADO");

    private List<Cuadro> armar(ReporteSolicitudesDTO r, Set<String> secciones) {
        List<Cuadro> cuadros = new ArrayList<>();
        // El ingreso es lo cobrado de los contratos con pago; lo pendiente, por pagar o anulado se muestra aparte en el detalle
        List<FilaSolicitud> conPago = r.filas().stream().filter(x -> CON_PAGO.contains(x.grupoEstado())).toList();

        if (secciones.contains("totales") && !conPago.isEmpty()) {
            BigDecimal neto = conPago.stream().map(FilaSolicitud::montoPagado).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ticket = neto.divide(BigDecimal.valueOf(conPago.size()), 2, RoundingMode.HALF_UP);
            List<String[]> f = new ArrayList<>();
            f.add(new String[] { "Contratos con pago", String.valueOf(conPago.size()) });
            f.add(new String[] { "INGRESO NETO", soles(neto) });
            f.add(new String[] { "Ticket promedio por contrato", soles(ticket) });
            BigDecimal porCobrar = r.filas().stream().filter(x -> "POR_PAGAR".equals(x.grupoEstado()))
                    .map(FilaSolicitud::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (porCobrar.signum() > 0) f.add(new String[] { "Por cobrar (reservas sin pagar)", soles(porCobrar) });
            BigDecimal retenido = r.filas().stream().filter(x -> "ANULADO".equals(x.grupoEstado()))
                    .map(FilaSolicitud::montoPagado).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (retenido.signum() > 0) f.add(new String[] { "Retenido de reservas canceladas (no incluido en el ingreso)", soles(retenido) });
            cuadros.add(new Cuadro("Resumen", List.of("Concepto", "Valor"), f, false, 1));
        }

        if (secciones.contains("estados")) {
            String[][] grupos = { { "PAGADO", "Reservado y pagado" }, { "EN_RUTA", "En ruta" }, { "FINALIZADO", "Finalizado" } };
            List<String[]> f = new ArrayList<>();
            long totalN = 0;
            BigDecimal totalC = BigDecimal.ZERO;
            for (String[] g : grupos) {
                List<FilaSolicitud> fs = conPago.stream().filter(x -> g[0].equals(x.grupoEstado())).toList();
                if (fs.isEmpty()) continue;
                BigDecimal cobrado = fs.stream().map(FilaSolicitud::montoPagado).reduce(BigDecimal.ZERO, BigDecimal::add);
                f.add(new String[] { g[1], String.valueOf(fs.size()), soles(cobrado) });
                totalN += fs.size();
                totalC = totalC.add(cobrado);
            }
            // Con un solo estado la tabla repetiría el resumen
            if (f.size() > 1) {
                f.add(new String[] { "TOTAL", String.valueOf(totalN), soles(totalC) });
                cuadros.add(new Cuadro("Ingreso por estado del contrato", List.of("Estado", "Contratos", "Cobrado"), f, true, 1));
            }
        }

        String extra = r.filtrosExtra() == null ? "" : r.filtrosExtra();
        boolean filtroVehiculo = extra.contains("Vehículo:") || extra.contains("Placa:");
        boolean filtroCliente = r.busqueda() != null && !r.busqueda().isBlank();

        // Con un vehículo, una placa o un cliente filtrado, esas tablas repetirían el detalle: solo se muestran en consultas generales
        if (secciones.contains("vehiculos") && !filtroVehiculo) {
            Map<String, BigDecimal> cobrado = new LinkedHashMap<>();
            Map<String, Long> contratos = new LinkedHashMap<>();
            for (FilaSolicitud x : conPago) {
                String placa = x.placa() == null || x.placa().isBlank() ? "-" : x.placa();
                String clave = x.vehiculo() + "|" + placa;
                cobrado.merge(clave, x.montoPagado(), BigDecimal::add);
                contratos.merge(clave, 1L, Long::sum);
            }
            if (cobrado.size() > 1) {
                List<String[]> f = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                long tContratos = 0;
                for (String clave : cobrado.keySet().stream()
                        .sorted(Comparator.comparing((String k) -> cobrado.get(k)).reversed().thenComparing(Comparator.naturalOrder())).toList()) {
                    String[] pp = clave.split("\\|", 2);
                    f.add(new String[] { pp[0], pp[1], String.valueOf(contratos.get(clave)), soles(cobrado.get(clave)) });
                    total = total.add(cobrado.get(clave));
                    tContratos += contratos.get(clave);
                }
                f.add(new String[] { "TOTAL", "", String.valueOf(tContratos), soles(total) });
                cuadros.add(new Cuadro("Ingreso por vehículo y placa", List.of("Vehículo", "Placa", "Contratos", "Cobrado"), f, true, 2));
            }
        }

        if (secciones.contains("clientes") && !filtroCliente) {
            Map<String, BigDecimal> cobrado = new LinkedHashMap<>();
            Map<String, Long> contratos = new LinkedHashMap<>();
            for (FilaSolicitud x : conPago) {
                String clave = x.cliente() + "|" + x.documento();
                cobrado.merge(clave, x.montoPagado(), BigDecimal::add);
                contratos.merge(clave, 1L, Long::sum);
            }
            if (cobrado.size() > 1) {
                List<String[]> f = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                long tContratos = 0;
                for (String clave : cobrado.keySet().stream()
                        .sorted(Comparator.comparing((String k) -> cobrado.get(k)).reversed().thenComparing(Comparator.naturalOrder())).toList()) {
                    String[] pp = clave.split("\\|", 2);
                    f.add(new String[] { pp[0], pp[1], String.valueOf(contratos.get(clave)), soles(cobrado.get(clave)) });
                    total = total.add(cobrado.get(clave));
                    tContratos += contratos.get(clave);
                }
                f.add(new String[] { "TOTAL", "", String.valueOf(tContratos), soles(total) });
                cuadros.add(new Cuadro("Ingreso por cliente", List.of("Cliente", "DNI", "Contratos", "Cobrado"), f, true, 2));
            }
        }

        if (secciones.contains("metodos")) {
            Set<String> codigos = new java.util.HashSet<>();
            conPago.forEach(x -> codigos.add(x.codigo()));
            Map<String, BigDecimal> monto = new LinkedHashMap<>();
            Map<String, Long> pagos = new LinkedHashMap<>();
            for (var pg : r.pagos()) {
                if (!codigos.contains(pg.codigoContrato())) continue;
                monto.merge(pg.metodo(), pg.monto(), BigDecimal::add);
                pagos.merge(pg.metodo(), 1L, Long::sum);
            }
            if (monto.size() > 1) {
                List<String[]> f = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                long tPagos = 0;
                for (String m : monto.keySet().stream().sorted(Comparator.comparing((String k) -> monto.get(k)).reversed()).toList()) {
                    f.add(new String[] { m, String.valueOf(pagos.get(m)), soles(monto.get(m)) });
                    total = total.add(monto.get(m));
                    tPagos += pagos.get(m);
                }
                f.add(new String[] { "TOTAL", String.valueOf(tPagos), soles(total) });
                cuadros.add(new Cuadro("Ingreso por método de pago", List.of("Método", "Pagos", "Monto"), f, true, 1));
            }
        }

        if (secciones.contains("detalle")) {
            // Cada situación va en su propio cuadro y con su propio total: lo pendiente o anulado no se mezcla con lo cobrado
            agregarDetalle(cuadros, r, "Detalle de contratos", CON_PAGO, "Cobrado", "pagado");
            agregarDetalle(cuadros, r, "Reservados por pagar", Set.of("POR_PAGAR"), "Por cobrar", "total");
            agregarDetalle(cuadros, r, "Solicitudes pendientes (todavía sin aprobar)", Set.of("PENDIENTE"), "Monto solicitado", "total");
            agregarDetalle(cuadros, r, "Anulados con dinero retenido (sin devolución)", Set.of("ANULADO"), "Retenido", "retenido", x -> x.montoPagado().signum() > 0);
            agregarDetalle(cuadros, r, "Anulados o cancelados sin dinero cobrado", Set.of("ANULADO"), "", "ninguno", x -> x.montoPagado().signum() <= 0);
        }
        return cuadros;
    }

    private record Col(String titulo, java.util.function.Function<FilaSolicitud, String> valor, boolean dinero) {}

    /**
     * Un cuadro por situación, solo con las columnas que significan algo en ella: no se repiten montos iguales
     * ni se muestran columnas que siempre valdrían cero o guion.
     */
    private void agregarDetalle(List<Cuadro> cuadros, ReporteSolicitudesDTO r, String titulo, Set<String> grupos,
                                String tituloDinero, String campoDinero) {
        agregarDetalle(cuadros, r, titulo, grupos, tituloDinero, campoDinero, x -> true);
    }

    private void agregarDetalle(List<Cuadro> cuadros, ReporteSolicitudesDTO r, String titulo, Set<String> grupos,
                                String tituloDinero, String campoDinero, java.util.function.Predicate<FilaSolicitud> filtro) {
        DateTimeFormatter corto = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        List<FilaSolicitud> filas = r.filas().stream().filter(x -> grupos.contains(x.grupoEstado()) && filtro.test(x)).toList();
        if (filas.isEmpty()) return;

        List<Col> cols = new ArrayList<>();
        cols.add(new Col("N° contrato", FilaSolicitud::codigo, false));
        cols.add(new Col("Registrado", x -> x.fechaRegistro() != null ? x.fechaRegistro().format(corto) : "-", false));
        cols.add(new Col("Cliente", FilaSolicitud::cliente, false));
        cols.add(new Col("DNI", FilaSolicitud::documento, false));
        cols.add(new Col("Vehículo", FilaSolicitud::vehiculo, false));
        cols.add(new Col("Placa", x -> x.placa() == null || x.placa().isBlank() ? "-" : x.placa(), false));
        cols.add(new Col("Recojo", x -> fechaHora(x.fechaRecojo(), x.horaRecojo()), false));
        cols.add(new Col("Devolución", x -> fechaHora(x.fechaDevolucion(), x.horaDevolucion()), false));
        cols.add(new Col("Estado", FilaSolicitud::estado, false));
        cols.add(new Col("Método de pago", x -> x.metodosPago() == null || x.metodosPago().isBlank() ? "-" : x.metodosPago(), false));

        // Se quitan las columnas de texto que no distinguen nada: todas con guion o todas con el mismo valor
        List<Col> texto = new ArrayList<>();
        for (Col c : cols) {
            Set<String> valores = new java.util.HashSet<>();
            for (FilaSolicitud x : filas) valores.add(c.valor().apply(x));
            boolean vacia = valores.size() == 1 && valores.contains("-");
            boolean constante = valores.size() == 1 && ("Estado".equals(c.titulo()));
            if (!vacia && !constante) texto.add(c);
        }

        // Dinero: una sola columna con el monto que corresponde a la situación; el total y el saldo solo si difieren del pagado
        List<Col> dinero = new ArrayList<>();
        boolean conPago = "pagado".equals(campoDinero);
        boolean difiere = conPago && filas.stream().anyMatch(x -> x.montoTotal().compareTo(x.montoPagado()) != 0 || x.saldo().signum() > 0);
        if ("total".equals(campoDinero)) {
            dinero.add(new Col(tituloDinero, x -> soles(x.montoTotal()), true));
        } else if (conPago) {
            if (difiere) dinero.add(new Col("Total", x -> soles(x.montoTotal()), true));
            dinero.add(new Col(tituloDinero, x -> soles(x.montoPagado()), true));
            if (difiere) dinero.add(new Col("Saldo", x -> soles(x.saldo()), true));
        } else if ("retenido".equals(campoDinero)) {
            dinero.add(new Col(tituloDinero, x -> soles(x.montoPagado()), true));
        }
        // "ninguno": sin columna de dinero

        List<Col> todas = new ArrayList<>(texto);
        todas.addAll(dinero);
        List<String> cabeceras = todas.stream().map(Col::titulo).toList();
        List<String[]> f = new ArrayList<>();
        for (FilaSolicitud x : filas) f.add(todas.stream().map(c -> c.valor().apply(x)).toArray(String[]::new));

        String[] total = new String[todas.size()];
        java.util.Arrays.fill(total, "");
        total[0] = "TOTAL (" + filas.size() + ")";
        for (int i = texto.size(); i < todas.size(); i++) {
            BigDecimal suma = BigDecimal.ZERO;
            for (String[] fila : f) suma = suma.add(new BigDecimal(fila[i].substring(3).replace(",", "")));
            total[i] = soles(suma);
        }
        f.add(total);
        cuadros.add(new Cuadro(titulo, cabeceras, f, true, texto.size()));
    }

    private static String fechaHora(java.time.LocalDate fecha, String hora) {
        if (fecha == null) return "-";
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM")) + (hora != null && !hora.isBlank() ? " " + hora : "");
    }

    private static String soles(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
        return "S/ " + String.format(Locale.US, "%,.2f", x);
    }

    private String subtitulo(ReporteSolicitudesDTO r) {
        String periodo = r.desde().equals(r.hasta()) ? r.desde().format(F_FECHA) : r.desde().format(F_FECHA) + " al " + r.hasta().format(F_FECHA);
        StringBuilder sb = new StringBuilder("Periodo: ").append(periodo);
        if (r.sede() != null && !"TODAS".equals(r.sede())) sb.append("  |  Sede: ").append(r.sede());
        if (r.categoria() != null && !"TODAS".equals(r.categoria())) sb.append("  |  Categoría: ").append(r.categoria());
        if (r.filtrosExtra() != null && !r.filtrosExtra().isBlank()) sb.append(r.filtrosExtra());
        if (r.busqueda() != null && !r.busqueda().isBlank()) sb.append("  |  Búsqueda: \"").append(r.busqueda()).append("\"");
        return sb.toString();
    }

    // ───────────────────────── Excel ─────────────────────────

    public byte[] excel(ReporteSolicitudesDTO r, Set<String> secciones) {
        List<Cuadro> cuadros = armar(r, secciones);
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet("Reporte");
            sh.setDisplayGridlines(false);

            XSSFFont blanca = wb.createFont();
            blanca.setBold(true);
            blanca.setColor(new XSSFColor(new byte[] { (byte) 255, (byte) 255, (byte) 255 }, null));
            XSSFFont negrita = wb.createFont();
            negrita.setBold(true);
            XSSFFont titulo = wb.createFont();
            titulo.setBold(true);
            titulo.setFontHeightInPoints((short) 14);

            XSSFCellStyle cab = wb.createCellStyle();
            cab.setFillForegroundColor(new XSSFColor(new byte[] { 0x0D, 0x1B, 0x3E }, null));
            cab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cab.setFont(blanca);
            cab.setAlignment(HorizontalAlignment.CENTER);
            XSSFCellStyle txt = wb.createCellStyle();
            txt.setBorderBottom(BorderStyle.THIN);
            XSSFCellStyle num = wb.createCellStyle();
            num.setBorderBottom(BorderStyle.THIN);
            num.setAlignment(HorizontalAlignment.RIGHT);
            XSSFCellStyle txtTotal = wb.createCellStyle();
            txtTotal.setFont(negrita);
            txtTotal.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0xDB, (byte) 0xE3, (byte) 0xEE }, null));
            txtTotal.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            XSSFCellStyle numTotal = wb.createCellStyle();
            numTotal.cloneStyleFrom(txtTotal);
            numTotal.setAlignment(HorizontalAlignment.RIGHT);
            var formato = wb.createDataFormat();
            XSSFCellStyle dinero = wb.createCellStyle();
            dinero.cloneStyleFrom(num);
            dinero.setDataFormat(formato.getFormat("\"S/ \"#,##0.00"));
            XSSFCellStyle dineroTotal = wb.createCellStyle();
            dineroTotal.cloneStyleFrom(numTotal);
            dineroTotal.setDataFormat(formato.getFormat("\"S/ \"#,##0.00"));
            int[] anchos = new int[14];
            for (int i = 0; i < anchos.length; i++) anchos[i] = 10;
            XSSFCellStyle subt = wb.createCellStyle();
            subt.setFont(negrita);
            XSSFCellStyle tit = wb.createCellStyle();
            tit.setFont(titulo);

            int fila = 0;
            Row r0 = sh.createRow(fila++);
            r0.createCell(0).setCellValue(empresa.getNombreComercial().toUpperCase() + " · Reporte de gestión");
            r0.getCell(0).setCellStyle(tit);
            sh.createRow(fila++).createCell(0).setCellValue(subtitulo(r));
            sh.createRow(fila++).createCell(0).setCellValue("Generado el " + r.generadoEn().format(F_HORA) + " por " + r.generadoPor());
            fila++;

            int anchoMax = 2;
            for (Cuadro c : cuadros) {
                anchoMax = Math.max(anchoMax, c.cabeceras().size());
                Row rt = sh.createRow(fila++);
                Cell ct = rt.createCell(0);
                ct.setCellValue(c.titulo());
                ct.setCellStyle(subt);

                Row rc = sh.createRow(fila++);
                for (int i = 0; i < c.cabeceras().size(); i++) {
                    Cell cell = rc.createCell(i);
                    cell.setCellValue(c.cabeceras().get(i));
                    cell.setCellStyle(cab);
                    anchos[Math.min(i, anchos.length - 1)] = Math.max(anchos[Math.min(i, anchos.length - 1)], c.cabeceras().get(i).length() + 4);
                }
                for (int k = 0; k < c.filas().size(); k++) {
                    boolean total = c.ultimaFilaTotal() && k == c.filas().size() - 1;
                    Row row = sh.createRow(fila++);
                    String[] vals = c.filas().get(k);
                    for (int i = 0; i < vals.length; i++) {
                        Cell cell = row.createCell(i);
                        boolean numerica = i >= c.primerasColumnasTexto();
                        String v = vals[i];
                        if (numerica && v.startsWith("S/ ")) {
                            cell.setCellValue(Double.parseDouble(v.substring(3).replace(",", "")));
                            cell.setCellStyle(total ? dineroTotal : dinero);
                        } else if (numerica && v.matches("\\d+")) {
                            cell.setCellValue(Double.parseDouble(v));
                            cell.setCellStyle(total ? numTotal : num);
                        } else {
                            cell.setCellValue(v);
                            cell.setCellStyle(total ? (numerica ? numTotal : txtTotal) : (numerica ? num : txt));
                        }
                        anchos[Math.min(i, anchos.length - 1)] = Math.max(anchos[Math.min(i, anchos.length - 1)], Math.min(v.length() + 2, 42));
                    }
                }
                fila++;
            }
            for (int i = 0; i < anchoMax; i++) sh.setColumnWidth(i, Math.max(anchos[i], 14) * 256);
            wb.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel: " + e.getMessage(), e);
        }
    }

    // ───────────────────────── PDF ─────────────────────────

    public byte[] pdf(ReporteSolicitudesDTO r, Set<String> secciones) {
        List<Cuadro> cuadros = armar(r, secciones);
        boolean ancho = secciones.contains("detalle");
        Document doc = new Document(ancho ? PageSize.A4.rotate() : PageSize.A4, 30, 30, 32, 32);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font fTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, NAVY);
            Font fSub = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font fSec = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, NAVY);
            Font fCab = FontFactory.getFont(FontFactory.HELVETICA_BOLD, ancho ? 7.5f : 9, Color.WHITE);
            Font fCel = FontFactory.getFont(FontFactory.HELVETICA, ancho ? 7.5f : 9, Color.BLACK);
            Font fTot = FontFactory.getFont(FontFactory.HELVETICA_BOLD, ancho ? 7.5f : 9, Color.BLACK);

            doc.add(new Paragraph(empresa.getNombreComercial().toUpperCase() + " · Reporte de gestión", fTitulo));
            doc.add(new Paragraph(subtitulo(r), fSub));
            Paragraph gen = new Paragraph("Generado el " + r.generadoEn().format(F_HORA) + " por " + r.generadoPor(), fSub);
            gen.setSpacingAfter(10);
            doc.add(gen);

            for (Cuadro c : cuadros) {
                Paragraph t = new Paragraph(c.titulo(), fSec);
                t.setSpacingBefore(8);
                t.setSpacingAfter(4);
                doc.add(t);
                PdfPTable tabla = new PdfPTable(c.cabeceras().size());
                tabla.setWidthPercentage(100);
                tabla.setHeaderRows(1);
                if (c.cabeceras().size() == 13) {
                    try {
                        tabla.setWidths(new float[] { 9, 7, 13, 8, 11, 7, 8, 8, 11, 8, 7, 7, 7 });
                    } catch (DocumentException ignorada) {
                        // se usan anchos iguales
                    }
                }
                for (String h : c.cabeceras()) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, fCab));
                    cell.setBackgroundColor(NAVY);
                    cell.setPadding(5);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabla.addCell(cell);
                }
                for (int k = 0; k < c.filas().size(); k++) {
                    boolean total = c.ultimaFilaTotal() && k == c.filas().size() - 1;
                    String[] vals = c.filas().get(k);
                    for (int i = 0; i < vals.length; i++) {
                        PdfPCell cell = new PdfPCell(new Phrase(vals[i], total ? fTot : fCel));
                        cell.setPadding(4);
                        cell.setBorderColor(new Color(0xDB, 0xE3, 0xEE));
                        if (total) cell.setBackgroundColor(new Color(0xDB, 0xE3, 0xEE));
                        cell.setHorizontalAlignment(i >= c.primerasColumnasTexto() ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                        tabla.addCell(cell);
                    }
                }
                doc.add(tabla);
            }
            if (cuadros.isEmpty()) {
                doc.add(new Paragraph("No hay contratos con los filtros elegidos.", fSub));
            }
            doc.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }
}
