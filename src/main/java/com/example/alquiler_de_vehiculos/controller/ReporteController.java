package com.example.alquiler_de_vehiculos.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteOperativoDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteSolicitudesDTO;
import com.example.alquiler_de_vehiculos.service.ReporteExcelService;
import com.example.alquiler_de_vehiculos.service.ReportePdfService;
import com.example.alquiler_de_vehiculos.service.ReporteResumenService;
import com.example.alquiler_de_vehiculos.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final MediaType PDF = MediaType.APPLICATION_PDF;

    private final ReporteService reporteService;
    private final ReporteExcelService excelService;
    private final ReportePdfService pdfService;
    private final ReporteResumenService resumenService;

    public ReporteController(ReporteService reporteService, ReporteExcelService excelService, ReportePdfService pdfService,
                             ReporteResumenService resumenService) {
        this.reporteService = reporteService;
        this.excelService = excelService;
        this.pdfService = pdfService;
        this.resumenService = resumenService;
    }

    @GetMapping("/ejecutivo")
    @PreAuthorize("hasRole('ADMIN')")
    public ReporteEjecutivoDTO ejecutivo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String sede,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) String placa) {
        return reporteService.ejecutivo(desde, hasta, sede, categoria, modelo, placa);
    }

    @GetMapping("/unidades")
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<ReporteEjecutivoDTO.UnidadOpcion> unidades() {
        return reporteService.unidadesParaSelector();
    }

    @GetMapping("/ejecutivo/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> ejecutivoExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String sede,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) String placa) {
        ReporteEjecutivoDTO r = reporteService.ejecutivo(desde, hasta, sede, categoria, modelo, placa);
        return excel(excelService.ejecutivo(r), "Reporte-Ejecutivo-PaulCars_" + r.desde() + "_a_" + r.hasta() + ".xlsx");
    }

    @GetMapping("/ejecutivo/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> ejecutivoPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String sede,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) String placa) {
        ReporteEjecutivoDTO r = reporteService.ejecutivo(desde, hasta, sede, categoria, modelo, placa);
        byte[] pdf = pdfService.ejecutivo(r, reporteService.usuarioActual());
        return ResponseEntity.ok()
                .contentType(PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("Resumen-Ejecutivo-PaulCars_" + r.desde() + "_a_" + r.hasta() + ".pdf").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    @GetMapping("/solicitudes")
    @PreAuthorize("hasRole('ADMIN')")
    public ReporteSolicitudesDTO solicitudes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String sede,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false, name = "q") String busqueda,
            @RequestParam(required = false) String metodo,
            @RequestParam(required = false) String asesor,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) String placa) {
        return reporteService.solicitudes(desde, hasta, estado, sede, categoria, busqueda, metodo, asesor, modelo, placa);
    }

    @GetMapping("/solicitudes/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> solicitudesExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String sede,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false, name = "q") String busqueda,
            @RequestParam(required = false) String metodo,
            @RequestParam(required = false) String asesor,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) String placa) {
        ReporteSolicitudesDTO r = reporteService.solicitudes(desde, hasta, estado, sede, categoria, busqueda, metodo, asesor, modelo, placa);
        String rango = r.desde().equals(r.hasta()) ? r.desde().toString() : r.desde() + "_a_" + r.hasta();
        return excel(excelService.solicitudes(r), "Reporte-Solicitudes-PaulCars_" + rango + ".xlsx");
    }

    /** Reporte resumido: el administrador elige los cuadros (secciones) y el formato (Excel o PDF). */
    @GetMapping("/resumen/{formato}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> resumen(
            @org.springframework.web.bind.annotation.PathVariable String formato,
            @RequestParam(required = false, defaultValue = "totales,estados,vehiculos,clientes,metodos,detalle") String secciones,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String sede,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false, name = "q") String busqueda,
            @RequestParam(required = false) String metodo,
            @RequestParam(required = false) String asesor,
            @RequestParam(required = false) String modelo,
            @RequestParam(required = false) String placa) {
        java.util.Set<String> elegidas = new java.util.LinkedHashSet<>();
        for (String x : secciones.split(",")) {
            String limpio = x.trim().toLowerCase();
            if (ReporteResumenService.SECCIONES.contains(limpio)) elegidas.add(limpio);
        }
        // Si el filtro ya fija la placa, el cliente o el método, ese cuadro solo repetiría lo mismo
        if (placa != null && !placa.isBlank() && !"TODAS".equalsIgnoreCase(placa)) elegidas.remove("vehiculos");
        if (busqueda != null && !busqueda.isBlank()) elegidas.remove("clientes");
        if (metodo != null && !metodo.isBlank() && !"TODOS".equalsIgnoreCase(metodo)) elegidas.remove("metodos");
        // El resumen separa los estados por sí mismo; el estado solo se usa para filtrar el detalle
        ReporteSolicitudesDTO r = reporteService.solicitudes(desde, hasta, estado, sede, categoria, busqueda, metodo, asesor, modelo, placa);
        // Sin contratos en el periodo y con esos filtros no se genera un documento vacío: el frontend avisa al usuario
        if (r.filas().isEmpty()) return ResponseEntity.noContent().build();
        String rango = r.desde().equals(r.hasta()) ? r.desde().toString() : r.desde() + "_a_" + r.hasta();
        if ("excel".equalsIgnoreCase(formato)) {
            return excel(resumenService.excel(r, elegidas), "Reporte-PaulCars_" + rango + ".xlsx");
        }
        if ("pdf".equalsIgnoreCase(formato)) {
            return ResponseEntity.ok()
                    .contentType(PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename("Reporte-PaulCars_" + rango + ".pdf").build().toString())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(resumenService.pdf(r, elegidas));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/cierre-caja")
    @PreAuthorize("hasRole('ADMIN')")
    public com.example.alquiler_de_vehiculos.dto.CierreCajaDTO cierreCaja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return reporteService.cierreCaja(fecha);
    }

    @GetMapping("/operativo")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ReporteOperativoDTO operativo() {
        return reporteService.operativoDelDia();
    }

    private ResponseEntity<byte[]> excel(byte[] contenido, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(contenido);
    }
}
