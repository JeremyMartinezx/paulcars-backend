package com.example.alquiler_de_vehiculos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.alquiler_de_vehiculos.model.Marca;

public interface MarcaRepository extends JpaRepository<Marca, Integer> {
}