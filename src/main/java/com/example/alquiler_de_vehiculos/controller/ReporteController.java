package com.example.alquiler_de_vehiculos.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.alquiler_de_vehiculos.dto.ReporteEjecutivoDTO;
import com.example.alquiler_de_vehiculos.dto.ReporteOperativoDTO;
import com.example.alquiler_de_vehiculos.service.ReporteService;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/ejecutivo")
    @PreAuthorize("hasRole('ADMIN')")
    public ReporteEjecutivoDTO ejecutivo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String sede,
            @RequestParam(required = false) String categoria) {
        return reporteService.ejecutivo(desde, hasta, sede, categoria);
    }

    @GetMapping("/operativo")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLEADO')")
    public ReporteOperativoDTO operativo() {
        return reporteService.operativoDelDia();
    }
}
