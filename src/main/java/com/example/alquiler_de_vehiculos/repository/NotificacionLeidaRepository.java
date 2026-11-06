package com.example.alquiler_de_vehiculos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.alquiler_de_vehiculos.model.NotificacionLeida;

public interface NotificacionLeidaRepository extends JpaRepository<NotificacionLeida, Integer> {

    List<NotificacionLeida> findByUsername(String username);

    boolean existsByUsernameAndClave(String username, String clave);
}
