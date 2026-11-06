package com.example.alquiler_de_vehiculos.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.dto.PlinPagoRequest;
import com.example.alquiler_de_vehiculos.dto.RegistrarPagoRequest;
import com.example.alquiler_de_vehiculos.dto.TarjetaPagoRequest;
import com.example.alquiler_de_vehiculos.dto.TransaccionPagoResponse;
import com.example.alquiler_de_vehiculos.dto.YapePagoRequest;
import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.EstadoAlquiler;
import com.example.alquiler_de_vehiculos.model.MetodoPago;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoAlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoVehiculoRepository;
import com.example.alquiler_de_vehiculos.repository.MetodoPagoRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoUnidadRepository;
import com.example.alquiler_de_vehiculos.util.TarifarioPaulCars;

import jakarta.transaction.Transactional;

@Service
public class PagoService {

    private final PagoRepository pagoRepository;
    private final AlquilerRepository alquilerRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final EstadoAlquilerRepository estadoAlquilerRepository;
    private final EstadoVehiculoRepository estadoVehiculoRepository;
    private final VehiculoUnidadRepository vehiculoUnidadRepository;

    public PagoService(PagoRepository pagoRepository,
                       AlquilerRepository alquilerRepository,
                       MetodoPagoRepository metodoPagoRepository,
                       EstadoAlquilerRepository estadoAlquilerRepository,
                       EstadoVehiculoRepository estadoVehiculoRepository,
                       VehiculoUnidadRepository vehiculoUnidadRepository) {
        this.pagoRepository = pagoRepository;
        this.alquilerRepository = alquilerRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.estadoAlquilerRepository = estadoAlquilerRepository;
        this.estadoVehiculoRepository = estadoVehiculoRepository;
        this.vehiculoUnidadRepository = vehiculoUnidadRepository;
    }

    @Transactional
    public Pago registrarPago(RegistrarPagoRequest req) {
        BigDecimal monto = TarifarioPaulCars.redondear(req.monto());
        if (monto.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto debe ser mayor que cero");
        }
        Alquiler alquiler = alquilerRepository.findById(req.idAlquiler())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado"));
        MetodoPago metodo = null;
        if (req.idMetodoPago() != null) {
            metodo = metodoPagoRepository.findById(req.idMetodoPago()).orElse(null);
        }
        if (metodo == null && req.idMetodoPago() != null) {
            if (req.idMetodoPago() == 2) {
                metodo = metodoPagoRepository.findByNombreMetodo("EFECTIVO").orElse(null);
            } else if (req.idMetodoPago() == 1) {
                metodo = metodoPagoRepository.findByNombreMetodo("TARJETA").orElse(null);
            } else if (req.idMetodoPago() == 3) {
                metodo = metodoPagoRepository.findByNombreMetodo("YAPE").orElse(null);
            } else if (req.idMetodoPago() == 4) {
                metodo = metodoPagoRepository.findByNombreMetodo("TRANSFERENCIA").orElse(null);
            }
        }
        if (metodo == null) {
            metodo = metodoPagoRepository.findByNombreMetodo("EFECTIVO")
                    .orElseGet(() -> metodoPagoRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Metodo de pago no encontrado")));
        }

        String estado = alquiler.getEstado() != null ? alquiler.getEstado().getNombreEstado().toUpperCase() : "";
        if (!"RESERVADO".equals(estado) && !"ACTIVO".equals(estado) && !"APROBADO".equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se pueden registrar pagos en un alquiler en estado " + estado);
        }

        BigDecimal saldo = saldoContrato(alquiler);
        if (saldo.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este alquiler no tiene saldo pendiente de pago");
        }
        if (monto.compareTo(saldo) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El monto (S/ " + monto + ") supera el saldo pendiente (S/ " + saldo + ")");
        }

        Pago pago = new Pago();
        pago.setAlquiler(alquiler);
        pago.setMetodoPago(metodo);
        pago.setMontoTotal(monto);
        pago.setFechaPago(LocalDate.now());
        pago.setEstadoPago("Pagado");
        pago.setTipoPago("ALQUILER");

        String nombreMetodo = metodo.getNombreMetodo().toUpperCase();
        if (nombreMetodo.contains("EFECTIVO")) {
            BigDecimal recibido = TarifarioPaulCars.redondear(req.montoRecibido() != null ? req.montoRecibido() : monto);
            if (recibido.compareTo(monto) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El efectivo recibido (S/ " + recibido + ") es menor al monto a cobrar (S/ " + monto + ")");
            }
            pago.setMontoRecibido(recibido);
            pago.setVuelto(recibido.subtract(monto));
        } else if (nombreMetodo.contains("TARJETA")) {
            String digitos = req.ultimosDigitos() != null ? req.ultimosDigitos().trim() : "";
            if (!digitos.matches("\\d{4}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa los últimos 4 dígitos de la tarjeta");
            }
            if (req.numeroOperacion() == null || req.numeroOperacion().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa el número de operación del voucher POS");
            }
            pago.setUltimosDigitos(digitos);
            pago.setNumeroOperacion(req.numeroOperacion().trim());
        } else {
            if (req.bancoOrigen() == null || req.bancoOrigen().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa el banco o billetera de origen");
            }
            if (req.numeroOperacion() == null || req.numeroOperacion().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa el código de operación");
            }
            pago.setBancoOrigen(req.bancoOrigen().trim());
            pago.setNumeroOperacion(req.numeroOperacion().trim());
        }

        pago.setCanal("COUNTER");
        pago.setValidadoPor("COUNTER_PRESENCIAL");
        pago.setFechaValidacion(LocalDateTime.now());
        Pago guardado = pagoRepository.save(pago);

        if (Boolean.TRUE.equals(req.garantiaConfirmada()) && alquiler.getGarantiaRetenida() == null) {
            alquiler.setGarantiaRetenida(TarifarioPaulCars.garantiaHoldPorCategoria(alquiler.getVehiculo()));
        }
        if ("RESERVADO".equals(estado) || "PAGADO".equals(estado)) {
            LocalDate hoy = LocalDate.now();
            boolean esHoyOPasado = alquiler.getFechaInicio() != null && !alquiler.getFechaInicio().isAfter(hoy);
            if (esHoyOPasado && alquiler.getGarantiaRetenida() != null) {
                estadoAlquilerRepository.findByNombreEstadoIgnoreCase("ACTIVO").ifPresent(alquiler::setEstado);
                if (alquiler.getVehiculoUnidad() != null) {
                    estadoVehiculoRepository.findByNombreEstadoIgnoreCase("EN_RUTA").ifPresent(est -> {
                        alquiler.getVehiculoUnidad().setEstado(est);
                        vehiculoUnidadRepository.save(alquiler.getVehiculoUnidad());
                    });
                }
            } else {
                estadoAlquilerRepository.findByNombreEstadoIgnoreCase("PAGADO").ifPresent(alquiler::setEstado);
            }
        }
        alquilerRepository.save(alquiler);
        return guardado;
    }

    public BigDecimal saldoContrato(Alquiler alquiler) {
        BigDecimal total = alquiler.getPrecioTotal() != null && alquiler.getPrecioTotal() > 0
                ? BigDecimal.valueOf(alquiler.getPrecioTotal())
                : TarifarioPaulCars.nz(alquiler.getVehiculo().getPrecioDia())
                        .multiply(BigDecimal.valueOf(alquiler.getDias() != null ? alquiler.getDias() : 1));
        BigDecimal pagado = pagoRepository.findByAlquiler_IdAlquiler(alquiler.getIdAlquiler()).stream()
                .filter(p -> p.getTipoPago() == null || "ALQUILER".equalsIgnoreCase(p.getTipoPago()))
                .map(Pago::getMontoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return TarifarioPaulCars.redondear(total.subtract(pagado).max(BigDecimal.ZERO));
    }

    public List<Pago> obtenerPagosPorAlquiler(Integer idAlquiler) {
        return pagoRepository.findByAlquiler_IdAlquiler(idAlquiler);
    }

    public List<Pago> listarPagos() {
        return pagoRepository.findAll();
    }

    private MetodoPago resolverMetodo(String nombre, Integer idFallback) {
        return metodoPagoRepository.findByNombreMetodo(nombre)
                .or(() -> metodoPagoRepository.findAll().stream()
                        .filter(m -> m.getNombreMetodo().toUpperCase().contains(nombre.toUpperCase()))
                        .findFirst())
                .or(() -> metodoPagoRepository.findById(idFallback))
                .orElseGet(() -> {
                    MetodoPago m = new MetodoPago();
                    m.setNombreMetodo(nombre);
                    return metodoPagoRepository.save(m);
                });
    }

    private void marcarAlquilerPagado(Alquiler alquiler) {
        EstadoAlquiler estadoPagado = estadoAlquilerRepository.findByNombreEstadoIgnoreCase("PAGADO")
                .orElseGet(() -> estadoAlquilerRepository.findByNombreEstadoIgnoreCase("CONFIRMADO")
                        .orElseGet(() -> {
                            EstadoAlquiler nuevo = new EstadoAlquiler();
                            nuevo.setNombreEstado("PAGADO");
                            return estadoAlquilerRepository.save(nuevo);
                        }));
        alquiler.setEstado(estadoPagado);
    }

    @Transactional
    public TransaccionPagoResponse pagarConYape(YapePagoRequest req, String username) {
        Alquiler alquiler = alquilerRepository.findById(req.idAlquiler())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado"));

        BigDecimal monto = req.monto() != null && req.monto().compareTo(BigDecimal.ZERO) >= 0
                ? req.monto()
                : saldoContrato(alquiler);

        String trxId = "TRX-YAPE-" + (System.currentTimeMillis() % 100000000L);

        MetodoPago metodo = resolverMetodo("YAPE", 3);
        Pago pago = new Pago();
        pago.setAlquiler(alquiler);
        pago.setMetodoPago(metodo);
        pago.setMontoTotal(monto);
        pago.setFechaPago(LocalDate.now());
        pago.setEstadoPago("Pagado");
        pago.setTipoPago("ALQUILER");
        pago.setCanal("DIGITAL");
        pago.setBancoOrigen("YAPE (" + req.celular() + ")");
        pago.setNumeroOperacion(trxId);
        pago.setValidadoPor("YAPE_GATEWAY_CULQI");
        pago.setFechaValidacion(LocalDateTime.now());
        pagoRepository.save(pago);

        marcarAlquilerPagado(alquiler);
        String nuevoEstado = "PAGADO";
        alquilerRepository.save(alquiler);

        return TransaccionPagoResponse.exitoso(
                trxId,
                "Pago aprobado con éxito vía Yape (" + req.celular() + ")",
                "YAPE",
                monto,
                alquiler.getIdAlquiler(),
                nuevoEstado
        );
    }

    @Transactional
    public TransaccionPagoResponse pagarConPlin(PlinPagoRequest req, String username) {
        Alquiler alquiler = alquilerRepository.findById(req.idAlquiler())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado"));

        BigDecimal monto = req.monto() != null && req.monto().compareTo(BigDecimal.ZERO) >= 0
                ? req.monto()
                : saldoContrato(alquiler);

        String trxId = "TRX-PLIN-" + (System.currentTimeMillis() % 100000000L);

        MetodoPago metodo = resolverMetodo("PLIN", 3);
        Pago pago = new Pago();
        pago.setAlquiler(alquiler);
        pago.setMetodoPago(metodo);
        pago.setMontoTotal(monto);
        pago.setFechaPago(LocalDate.now());
        pago.setEstadoPago("Pagado");
        pago.setTipoPago("ALQUILER");
        pago.setCanal("DIGITAL");
        String banco = req.banco() != null && !req.banco().isBlank() ? req.banco().trim() : "INTEROPERABLE";
        String cel = req.celular() != null && !req.celular().isBlank() ? req.celular().trim() : "QR_DINAMICO";
        pago.setBancoOrigen("PLIN (" + banco + " - " + cel + ")");
        pago.setNumeroOperacion(trxId);
        pago.setValidadoPor("PLIN_GATEWAY_INTEROPERABLE");
        pago.setFechaValidacion(LocalDateTime.now());
        pagoRepository.save(pago);

        marcarAlquilerPagado(alquiler);
        String nuevoEstado = "PAGADO";
        alquilerRepository.save(alquiler);

        return TransaccionPagoResponse.exitoso(
                trxId,
                "Pago aprobado con éxito vía Plin Interoperable",
                "PLIN",
                monto,
                alquiler.getIdAlquiler(),
                nuevoEstado
        );
    }

    @Transactional
    public TransaccionPagoResponse pagarConTarjeta(TarjetaPagoRequest req, String username) {
        Alquiler alquiler = alquilerRepository.findById(req.idAlquiler())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alquiler no encontrado"));

        BigDecimal monto = req.monto() != null && req.monto().compareTo(BigDecimal.ZERO) >= 0
                ? req.monto()
                : saldoContrato(alquiler);

        String trxId = "TRX-CARD-" + (System.currentTimeMillis() % 100000000L);

        String ultimos4 = req.ultimosCuatroDigitos() != null && !req.ultimosCuatroDigitos().isBlank()
                ? req.ultimosCuatroDigitos().trim()
                : (req.numeroTarjeta() != null && req.numeroTarjeta().replaceAll("\\D", "").length() >= 4
                        ? req.numeroTarjeta().replaceAll("\\D", "").substring(req.numeroTarjeta().replaceAll("\\D", "").length() - 4)
                        : "0000");

        String marca = req.marca() != null && !req.marca().isBlank()
                ? req.marca().trim().toUpperCase()
                : (req.numeroTarjeta() != null && req.numeroTarjeta().startsWith("4") ? "VISA"
                        : (req.numeroTarjeta() != null && req.numeroTarjeta().startsWith("5") ? "MASTERCARD" : "TARJETA"));

        String titularStr = req.titular() != null && !req.titular().isBlank() ? req.titular().trim().toUpperCase() : "CLIENTE TITULAR";
        String tokenTrx = req.token() != null && !req.token().isBlank() ? req.token().trim() : "tok_culqi_" + trxId;

        MetodoPago metodo = resolverMetodo("TARJETA", 1);
        Pago pago = new Pago();
        pago.setAlquiler(alquiler);
        pago.setMetodoPago(metodo);
        pago.setMontoTotal(monto);
        pago.setFechaPago(LocalDate.now());
        pago.setEstadoPago("Pagado");
        pago.setTipoPago("ALQUILER");
        pago.setCanal("DIGITAL");
        pago.setUltimosDigitos(ultimos4);
        pago.setBancoOrigen(marca + " • Titular: " + titularStr + " (Token Culqi: " + tokenTrx + ")");
        pago.setNumeroOperacion(trxId);
        pago.setValidadoPor("CULQI_GATEWAY_PCI_DSS");
        pago.setFechaValidacion(LocalDateTime.now());
        pagoRepository.save(pago);

        marcarAlquilerPagado(alquiler);
        String nuevoEstado = "PAGADO";
        alquilerRepository.save(alquiler);

        return TransaccionPagoResponse.exitoso(
                trxId,
                "Pago aprobado con éxito con tarjeta " + marca + " terminada en " + ultimos4,
                "TARJETA",
                monto,
                alquiler.getIdAlquiler(),
                nuevoEstado
        );
    }
}
