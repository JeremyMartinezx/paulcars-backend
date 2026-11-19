package com.example.alquiler_de_vehiculos.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/**
 * Lleva la cuenta de "cuántas veces cambió algo importante" (alquileres, pagos, clientes, vehículos, usuarios).
 * Las pantallas del personal consultan este número cada pocos segundos y solo recargan sus listas cuando cambia,
 * así se ven los últimos cambios al instante sin recargar la página y sin saturar la API.
 *
 * El número sube únicamente cuando la transacción ya terminó (commit): nadie lee datos a medias.
 */
public class CambiosListener {

    private static final AtomicLong VERSION = new AtomicLong(System.currentTimeMillis());

    public static long version() {
        return VERSION.get();
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    public void alCambiar(Object entidad) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    VERSION.incrementAndGet();
                }
            });
        } else {
            VERSION.incrementAndGet();
        }
    }
}
