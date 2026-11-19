package com.example.alquiler_de_vehiculos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.alquiler_de_vehiculos.config.PoliticaCancelacionProperties;

/** Cada 5 segundos cancela las reservas que no se pagaron a tiempo y las pagadas cuyo cliente no se presentó (si las opciones están activadas). */
@Component
public class ReservasVencidasJob {

    private static final Logger log = LoggerFactory.getLogger(ReservasVencidasJob.class);

    private final CancelacionService cancelacionService;
    private final PoliticaCancelacionProperties politica;

    public ReservasVencidasJob(CancelacionService cancelacionService, PoliticaCancelacionProperties politica) {
        this.cancelacionService = cancelacionService;
        this.politica = politica;
    }

    @Scheduled(fixedDelay = 5_000, initialDelay = 15_000)
    public void cancelarVencidas() {
        if (politica.isCancelarVencidasAuto()) {
            int n = cancelacionService.cancelarVencidas();
            if (n > 0) log.info("Se cancelaron {} reservas sin pago vencidas", n);
        }
        // Reservas próximas cuya placa salió de servicio: pasan solas a otra placa libre
        int movidas = cancelacionService.reasignarReservasSinUnidadApta();
        if (movidas > 0) log.info("Se reasignaron {} reservas a otra placa porque la suya salió de servicio", movidas);
        if (politica.isCancelarNoPresentadasAuto()) {
            int n = cancelacionService.cancelarNoPresentadas();
            if (n > 0) log.info("Se cancelaron {} reservas por \"No se presentó\"", n);
        }
    }
}
