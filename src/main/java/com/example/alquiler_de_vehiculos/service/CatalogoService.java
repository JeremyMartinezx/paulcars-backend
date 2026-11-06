package com.example.alquiler_de_vehiculos.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.alquiler_de_vehiculos.model.Combustible;
import com.example.alquiler_de_vehiculos.model.Marca;
import com.example.alquiler_de_vehiculos.model.MetodoPago;
import com.example.alquiler_de_vehiculos.model.Modelo;
import com.example.alquiler_de_vehiculos.model.TipoVehiculo;
import com.example.alquiler_de_vehiculos.repository.CombustibleRepository;
import com.example.alquiler_de_vehiculos.repository.MarcaRepository;
import com.example.alquiler_de_vehiculos.repository.MetodoPagoRepository;
import com.example.alquiler_de_vehiculos.repository.ModeloRepository;
import com.example.alquiler_de_vehiculos.repository.TipoVehiculoRepository;

import jakarta.transaction.Transactional;

@Service
public class CatalogoService {

    private final MarcaRepository marcaRepository;
    private final ModeloRepository modeloRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final CombustibleRepository combustibleRepository;
    private final MetodoPagoRepository metodoPagoRepository;

    public CatalogoService(MarcaRepository marcaRepository,
                           ModeloRepository modeloRepository,
                           TipoVehiculoRepository tipoVehiculoRepository,
                           CombustibleRepository combustibleRepository,
                           MetodoPagoRepository metodoPagoRepository) {
        this.marcaRepository = marcaRepository;
        this.modeloRepository = modeloRepository;
        this.tipoVehiculoRepository = tipoVehiculoRepository;
        this.combustibleRepository = combustibleRepository;
        this.metodoPagoRepository = metodoPagoRepository;
    }

    public List<Marca> listarMarcas() {
        return marcaRepository.findAll();
    }

    @Transactional
    public Marca crearMarca(Marca marca) {
        marca.setIdMarca(null);
        return marcaRepository.save(marca);
    }

    @Transactional
    public Marca actualizarMarca(Integer id, Marca datos) {
        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Marca"));
        marca.setNombreMarca(datos.getNombreMarca());
        return marcaRepository.save(marca);
    }

    public List<Modelo> listarModelos() {
        return modeloRepository.findAll();
    }

    @Transactional
    public Modelo crearModelo(Modelo modelo) {
        Marca marca = marcaRepository.findById(modelo.getMarca().getIdMarca())
                .orElseThrow(() -> noEncontrado("Marca"));
        modelo.setIdModelo(null);
        modelo.setMarca(marca);
        return modeloRepository.save(modelo);
    }

    @Transactional
    public Modelo actualizarModelo(Integer id, Modelo datos) {
        Modelo modelo = modeloRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Modelo"));
        Marca marca = marcaRepository.findById(datos.getMarca().getIdMarca())
                .orElseThrow(() -> noEncontrado("Marca"));
        modelo.setModelo(datos.getModelo());
        modelo.setMarca(marca);
        return modeloRepository.save(modelo);
    }

    public List<TipoVehiculo> listarTiposVehiculo() {
        return tipoVehiculoRepository.findAll();
    }

    @Transactional
    public TipoVehiculo crearTipoVehiculo(TipoVehiculo tipo) {
        tipo.setIdTipo(null);
        return tipoVehiculoRepository.save(tipo);
    }

    @Transactional
    public TipoVehiculo actualizarTipoVehiculo(Integer id, TipoVehiculo datos) {
        TipoVehiculo tipo = tipoVehiculoRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Tipo de vehiculo"));
        tipo.setNombreTipo(datos.getNombreTipo());
        return tipoVehiculoRepository.save(tipo);
    }

    public List<Combustible> listarCombustibles() {
        return combustibleRepository.findAll();
    }

    @Transactional
    public Combustible crearCombustible(Combustible combustible) {
        combustible.setIdCombustible(null);
        return combustibleRepository.save(combustible);
    }

    @Transactional
    public Combustible actualizarCombustible(Integer id, Combustible datos) {
        Combustible combustible = combustibleRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Combustible"));
        combustible.setNombreCombustible(datos.getNombreCombustible());
        return combustibleRepository.save(combustible);
    }

    public List<MetodoPago> listarMetodosPago() {
        return metodoPagoRepository.findAll();
    }

    @Transactional
    public MetodoPago crearMetodoPago(MetodoPago metodoPago) {
        metodoPago.setIdMetodoPago(null);
        return metodoPagoRepository.save(metodoPago);
    }

    @Transactional
    public MetodoPago actualizarMetodoPago(Integer id, MetodoPago datos) {
        MetodoPago metodoPago = metodoPagoRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Metodo de pago"));
        metodoPago.setNombreMetodo(datos.getNombreMetodo());
        return metodoPagoRepository.save(metodoPago);
    }

    private ResponseStatusException noEncontrado(String entidad) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, entidad + " no encontrado");
    }
}

