package com.example.alquiler_de_vehiculos.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.alquiler_de_vehiculos.config.EmpresaProperties;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO.FilaMonto;
import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO.FilaVehiculo;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * PDF formal del Resumen ejecutivo: membrete de la empresa, periodo y filtros,
 * indicadores principales, ingresos por sede / categoría / método y top de vehículos.
 */
@Service
public class ReportePdfService {

    private static final Color NAVY = new Color(0x0D, 0x1B, 0x3E);
    private static final Color GOLD = new Color(0xC9, 0xA2, 0x27);
    private static final Color GRIS_TEXTO = new Color(0x47, 0x55, 0x69);
    private static final Color GRIS_SUAVE = new Color(0xF1, 0xF5, 0xF9);
    private static final Color GRIS_BORDE = new Color(0xCB, 0xD5, 0xE1);

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EmpresaProperties empresa;

    public ReportePdfService(EmpresaProperties empresa) {
        this.empresa = empresa;
    }

    public byte[] ejecutivo(ReporteEjecutivoDTO r, String generadoPor) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 30, 62);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new Pie(empresa));
            doc.addTitle("Resumen ejecutivo " + r.desde() + " a " + r.hasta());
            doc.addAuthor(empresa.getRazonSocial());
            doc.addCreator(empresa.getNombreComercial());
            doc.open();

            doc.add(cabecera(r, generadoPor));
            espacio(doc, 10);
            doc.add(kpis(r));
            espacio(doc, 10);

            doc.add(tablaMontos("Ingresos por sede", "Sede", r.ingresosPorSede()));
            espacio(doc, 8);
            doc.add(tablaMontos("Ingresos por categoría", "Categoría", r.ingresosPorCategoria()));
            espacio(doc, 8);
            doc.add(tablaMontos("Ingresos por método de pago", "Método", r.ingresosPorMetodo()));
            espacio(doc, 8);
            doc.add(tablaVehiculos(r.topVehiculos()));

            espacio(doc, 10);
            Paragraph nota = new Paragraph("Los ingresos corresponden a los pagos registrados en el periodo. La ocupación de flota se calcula "
                    + "sobre los días alquilados respecto a la capacidad total de las unidades filtradas.",
                    fuente(7.5f, Font.ITALIC, GRIS_TEXTO));
            nota.setAlignment(Element.ALIGN_JUSTIFIED);
            doc.add(nota);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del resumen ejecutivo: " + e.getMessage(), e);
        }
    }

    private PdfPTable cabecera(ReporteEjecutivoDTO r, String generadoPor) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[] { 2.3f, 1.35f });
        t.setWidthPercentage(100);

        PdfPCell emisor = new PdfPCell();
        emisor.setBackgroundColor(NAVY);
        emisor.setBorder(Rectangle.NO_BORDER);
        emisor.setPadding(10);
        try (InputStream in = getClass().getResourceAsStream("/pdf/logo.png")) {
            if (in != null) {
                Image img = Image.getInstance(in.readAllBytes());
                img.scaleToFit(120, 50);
                emisor.addElement(img);
            } else {
                emisor.addElement(new Phrase(empresa.getNombreComercial().toUpperCase(), fuente(18, Font.BOLD, Color.WHITE)));
            }
        } catch (Exception e) {
            emisor.addElement(new Phrase(empresa.getNombreComercial().toUpperCase(), fuente(18, Font.BOLD, Color.WHITE)));
        }
        Paragraph razon = new Paragraph(empresa.getRazonSocial(), fuente(10, Font.BOLD, Color.WHITE));
        razon.setSpacingBefore(4);
        emisor.addElement(razon);
        Color claro = new Color(0xCB, 0xD5, 0xE1);
        emisor.addElement(new Paragraph(empresa.rucTexto(), fuente(8.5f, Font.BOLD, GOLD)));
        emisor.addElement(new Paragraph(empresa.getDireccion(), fuente(8, Font.NORMAL, claro)));
        emisor.addElement(new Paragraph("Tel. " + empresa.getTelefono() + "  |  " + empresa.getEmail(), fuente(8, Font.NORMAL, claro)));
        t.addCell(emisor);

        PdfPCell caja = new PdfPCell();
        caja.setBorderColor(NAVY);
        caja.setBorderWidth(1.4f);
        caja.setPadding(10);
        caja.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph titulo = new Paragraph("RESUMEN EJECUTIVO", fuente(13, Font.BOLD, NAVY));
        titulo.setAlignment(Element.ALIGN_CENTER);
        Paragraph periodo = new Paragraph(r.desde().format(FECHA) + " al " + r.hasta().format(FECHA), fuente(11, Font.BOLD, NAVY));
        periodo.setAlignment(Element.ALIGN_CENTER);
        periodo.setSpacingBefore(6);
        Paragraph filtros = new Paragraph("Sede: " + r.sede() + "  |  Categoría: " + r.categoria()
                + ("TODOS".equals(r.vehiculo()) ? "" : "  |  Vehículo: " + r.vehiculo()), fuente(8, Font.NORMAL, GRIS_TEXTO));
        filtros.setAlignment(Element.ALIGN_CENTER);
        filtros.setSpacingBefore(4);
        Paragraph gen = new Paragraph("Generado el " + LocalDateTime.now().format(FECHA_HORA)
                + (generadoPor != null ? " por " + generadoPor : ""), fuente(7.5f, Font.NORMAL, GRIS_TEXTO));
        gen.setAlignment(Element.ALIGN_CENTER);
        gen.setSpacingBefore(3);
        caja.addElement(titulo);
        caja.addElement(periodo);
        caja.addElement(filtros);
        caja.addElement(gen);
        t.addCell(caja);

        PdfPCell linea = new PdfPCell(new Phrase(" ", fuente(2, Font.NORMAL, GOLD)));
        linea.setColspan(2);
        linea.setBackgroundColor(GOLD);
        linea.setBorder(Rectangle.NO_BORDER);
        linea.setFixedHeight(3);
        t.addCell(linea);
        return t;
    }

    private PdfPTable kpis(ReporteEjecutivoDTO r) {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        double diasPorAlquiler = r.alquileresEnPeriodo() > 0 ? (double) r.diasTotalesAlquilados() / r.alquileresEnPeriodo() : 0;
        String[][] datos = {
                { "Ingresos del periodo", soles(r.ingresosTotales()), r.alquileresEnPeriodo() + " contratos iniciados" },
                { "Ocupación de flota", String.format(Locale.US, "%.1f%%", r.ocupacionFlota()), r.diasTotalesAlquilados() + " días alquilados" },
                { "Ticket promedio", soles(r.ticketPromedio()), "por contrato" },
                { "Contratos finalizados", String.valueOf(r.alquileresFinalizados()),
                        String.format(Locale.US, "%.1f días por alquiler", diasPorAlquiler) }
        };
        for (String[] d : datos) {
            PdfPCell c = new PdfPCell();
            c.setBackgroundColor(GRIS_SUAVE);
            c.setBorderColor(GRIS_BORDE);
            c.setBorderWidth(0.6f);
            c.setPadding(8);
            c.addElement(new Paragraph(d[0].toUpperCase(), fuente(7, Font.BOLD, GRIS_TEXTO)));
            Paragraph v = new Paragraph(d[1], fuente(15, Font.BOLD, NAVY));
            v.setSpacingBefore(3);
            c.addElement(v);
            Paragraph s = new Paragraph(d[2], fuente(7.5f, Font.NORMAL, GRIS_TEXTO));
            s.setSpacingBefore(2);
            c.addElement(s);
            t.addCell(c);
        }
        return t;
    }

    private PdfPTable tablaMontos(String titulo, String colEtiqueta, List<FilaMonto> filas) {
        PdfPTable t = new PdfPTable(new float[] { 3f, 1f, 1.4f });
        t.setWidthPercentage(100);
        t.setKeepTogether(true);
        titulo(t, titulo, 3);
        encabezados(t, colEtiqueta, "Pagos", "Total");
        if (filas == null || filas.isEmpty()) {
            PdfPCell c = new PdfPCell(new Phrase("Sin pagos en el periodo", fuente(8.5f, Font.ITALIC, GRIS_TEXTO)));
            c.setColspan(3);
            c.setBorderColor(GRIS_BORDE);
            c.setBorderWidth(0.5f);
            c.setPadding(6);
            t.addCell(c);
            return t;
        }
        BigDecimal suma = BigDecimal.ZERO;
        long cantidad = 0;
        for (FilaMonto f : filas) {
            celda(t, f.etiqueta(), false, Element.ALIGN_LEFT);
            celda(t, String.valueOf(f.cantidad()), false, Element.ALIGN_CENTER);
            celda(t, soles(f.total()), false, Element.ALIGN_RIGHT);
            suma = suma.add(f.total() != null ? f.total() : BigDecimal.ZERO);
            cantidad += f.cantidad();
        }
        celda(t, "TOTAL", true, Element.ALIGN_LEFT);
        celda(t, String.valueOf(cantidad), true, Element.ALIGN_CENTER);
        celda(t, soles(suma), true, Element.ALIGN_RIGHT);
        return t;
    }

    private PdfPTable tablaVehiculos(List<FilaVehiculo> filas) {
        PdfPTable t = new PdfPTable(new float[] { 0.4f, 2.4f, 1.1f, 1.2f, 1f, 0.8f, 1.4f, 1f });
        t.setWidthPercentage(100);
        titulo(t, "Top vehículos más rentables", 8);
        encabezados(t, "#", "Vehículo", "Placa", "Categoría", "Alquileres", "Días", "Ingresos", "Utilización");
        if (filas == null || filas.isEmpty()) {
            PdfPCell c = new PdfPCell(new Phrase("No hay vehículos con estos filtros", fuente(8.5f, Font.ITALIC, GRIS_TEXTO)));
            c.setColspan(8);
            c.setBorderColor(GRIS_BORDE);
            c.setBorderWidth(0.5f);
            c.setPadding(6);
            t.addCell(c);
            return t;
        }
        int n = 1;
        for (FilaVehiculo v : filas) {
            celda(t, String.valueOf(n++), false, Element.ALIGN_CENTER);
            celda(t, v.vehiculo(), false, Element.ALIGN_LEFT);
            celda(t, v.placa() != null ? v.placa() : "-", false, Element.ALIGN_CENTER);
            celda(t, v.categoria(), false, Element.ALIGN_CENTER);
            celda(t, String.valueOf(v.alquileres()), false, Element.ALIGN_CENTER);
            celda(t, String.valueOf(v.diasAlquilados()), false, Element.ALIGN_CENTER);
            celda(t, soles(v.ingresos()), false, Element.ALIGN_RIGHT);
            celda(t, String.format(Locale.US, "%.0f%%", v.utilizacion()), false, Element.ALIGN_CENTER);
        }
        return t;
    }

    private void titulo(PdfPTable t, String texto, int columnas) {
        PdfPCell h = new PdfPCell(new Phrase(texto.toUpperCase(), fuente(8.5f, Font.BOLD, Color.WHITE)));
        h.setColspan(columnas);
        h.setBackgroundColor(NAVY);
        h.setBorder(Rectangle.NO_BORDER);
        h.setPadding(5);
        h.setPaddingLeft(8);
        t.addCell(h);
    }

    private void encabezados(PdfPTable t, String... columnas) {
        for (String c : columnas) {
            PdfPCell cell = new PdfPCell(new Phrase(c, fuente(8, Font.BOLD, GRIS_TEXTO)));
            cell.setBackgroundColor(GRIS_SUAVE);
            cell.setBorderColor(GRIS_BORDE);
            cell.setBorderWidth(0.5f);
            cell.setPadding(4);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(cell);
        }
    }

    private void celda(PdfPTable t, String texto, boolean total, int alineacion) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : "-", fuente(8.5f, total ? Font.BOLD : Font.NORMAL, NAVY)));
        c.setBorderColor(GRIS_BORDE);
        c.setBorderWidth(0.5f);
        c.setPadding(4);
        c.setHorizontalAlignment(alineacion);
        if (total) c.setBackgroundColor(GRIS_SUAVE);
        t.addCell(c);
    }

    private String soles(BigDecimal valor) {
        return "S/ " + String.format(Locale.US, "%,.2f", valor != null ? valor : BigDecimal.ZERO);
    }

    private void espacio(Document doc, float altura) throws DocumentException {
        Paragraph p = new Paragraph(" ", fuente(altura, Font.NORMAL, Color.WHITE));
        p.setLeading(altura);
        doc.add(p);
    }

    private static Font fuente(float tamano, int estilo, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, tamano, estilo, color);
    }

    private static final class Pie extends PdfPageEventHelper {
        private final EmpresaProperties empresa;

        Pie(EmpresaProperties empresa) {
            this.empresa = empresa;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable pie = new PdfPTable(2);
            try {
                pie.setTotalWidth(document.right() - document.left());
                pie.setWidths(new float[] { 3f, 1f });
                PdfPCell izq = new PdfPCell(new Phrase(empresa.getRazonSocial() + "  |  " + empresa.rucTexto()
                        + "\nDocumento generado electrónicamente"
                        + "\n" + empresa.lineaContacto(), fuente(7, Font.NORMAL, GRIS_TEXTO)));
                PdfPCell der = new PdfPCell(new Phrase("Página " + writer.getPageNumber(), fuente(7.5f, Font.NORMAL, GRIS_TEXTO)));
                der.setHorizontalAlignment(Element.ALIGN_RIGHT);
                for (PdfPCell c : new PdfPCell[] { izq, der }) {
                    c.setBorder(Rectangle.TOP);
                    c.setBorderColor(GRIS_BORDE);
                    c.setPaddingTop(4);
                    pie.addCell(c);
                }
                pie.writeSelectedRows(0, -1, document.left(), document.bottom() - 8, writer.getDirectContent());
            } catch (DocumentException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
