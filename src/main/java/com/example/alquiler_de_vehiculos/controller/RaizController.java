package com.example.alquiler_de_vehiculos.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Respuesta mínima en la raíz: sirve para comprobar que el servicio está activo, sin revelar tecnología ni rutas. */
@RestController
public class RaizController {

    @GetMapping("/")
    public Map<String, String> raiz() {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("service", "Paul Car's API");
        r.put("status", "ok");
        return r;
    }
}
