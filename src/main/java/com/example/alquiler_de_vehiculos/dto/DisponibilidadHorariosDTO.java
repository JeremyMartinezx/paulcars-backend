package com.example.alquiler_de_vehiculos.dto;

import java.time.LocalDate;
import java.util.List;

public class DisponibilidadHorariosDTO {
    private String sede;
    private LocalDate fecha;
    private boolean disponible;
    private String mensaje;
    private List<String> horarios;
    private LocalDate proximaFechaDisponible;
    private String proximoHorarioDisponible;

    public DisponibilidadHorariosDTO() {}

    public DisponibilidadHorariosDTO(String sede, LocalDate fecha, boolean disponible, String mensaje,
                                     List<String> horarios, LocalDate proximaFechaDisponible,
                                     String proximoHorarioDisponible) {
        this.sede = sede;
        this.fecha = fecha;
        this.disponible = disponible;
        this.mensaje = mensaje;
        this.horarios = horarios;
        this.proximaFechaDisponible = proximaFechaDisponible;
        this.proximoHorarioDisponible = proximoHorarioDisponible;
    }

    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<String> getHorarios() {
        return horarios;
    }

    public void setHorarios(List<String> horarios) {
        this.horarios = horarios;
    }

    public LocalDate getProximaFechaDisponible() {
        return proximaFechaDisponible;
    }

    public void setProximaFechaDisponible(LocalDate proximaFechaDisponible) {
        this.proximaFechaDisponible = proximaFechaDisponible;
    }

    public String getProximoHorarioDisponible() {
        return proximoHorarioDisponible;
    }

    public void setProximoHorarioDisponible(String proximoHorarioDisponible) {
        this.proximoHorarioDisponible = proximoHorarioDisponible;
    }
}
