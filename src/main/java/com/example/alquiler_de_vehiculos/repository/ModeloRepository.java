package com.example.alquiler_de_vehiculos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.Modelo;

public interface ModeloRepository extends JpaRepository<Modelo, Integer> {
}
