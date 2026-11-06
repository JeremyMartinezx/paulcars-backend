package com.example.alquiler_de_vehiculos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AlquilerDeVehiculosApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlquilerDeVehiculosApplication.class, args);
	}

}
