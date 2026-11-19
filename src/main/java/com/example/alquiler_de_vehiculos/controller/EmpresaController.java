package com.example.alquiler_de_vehiculos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.alquiler_de_vehiculos.config.EmpresaProperties;

/** Datos legales y de contacto de la empresa (membrete de reportes e impresiones). */
@RestController
@RequestMapping("/api/empresa")
public class EmpresaController {

    private final EmpresaProperties empresa;

    public EmpresaController(EmpresaProperties empresa) {
        this.empresa = empresa;
    }

    public record EmpresaDTO(String razonSocial, String nombreComercial, String ruc, String rucTexto,
                             String direccion, String telefono, String email, String web) {}

    @GetMapping
    public EmpresaDTO obtener() {
        return new EmpresaDTO(empresa.getRazonSocial(), empresa.getNombreComercial(), empresa.getRuc(),
                empresa.rucTexto(), empresa.getDireccion(), empresa.getTelefono(), empresa.getEmail(), empresa.getWeb());
    }
}
