package com.example.alquiler_de_vehiculos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.Combustible;

public interface CombustibleRepository extends JpaRepository<Combustible, Integer> {
}
