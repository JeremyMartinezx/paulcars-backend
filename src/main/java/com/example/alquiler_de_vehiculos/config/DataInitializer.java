package com.example.alquiler_de_vehiculos.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.alquiler_de_vehiculos.model.Alquiler;
import com.example.alquiler_de_vehiculos.model.Cliente;
import com.example.alquiler_de_vehiculos.model.Combustible;
import com.example.alquiler_de_vehiculos.model.EstadoAlquiler;
import com.example.alquiler_de_vehiculos.model.EstadoVehiculo;
import com.example.alquiler_de_vehiculos.model.ImagenVehiculo;
import com.example.alquiler_de_vehiculos.model.Marca;
import com.example.alquiler_de_vehiculos.model.MetodoPago;
import com.example.alquiler_de_vehiculos.model.Modelo;
import com.example.alquiler_de_vehiculos.model.Pago;
import com.example.alquiler_de_vehiculos.model.Rol;
import com.example.alquiler_de_vehiculos.model.TipoVehiculo;
import com.example.alquiler_de_vehiculos.model.Usuario;
import com.example.alquiler_de_vehiculos.model.VehiculoModelo;
import com.example.alquiler_de_vehiculos.model.VehiculoUnidad;
import com.example.alquiler_de_vehiculos.repository.AlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.ClienteRepository;
import com.example.alquiler_de_vehiculos.repository.CombustibleRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoAlquilerRepository;
import com.example.alquiler_de_vehiculos.repository.EstadoVehiculoRepository;
import com.example.alquiler_de_vehiculos.repository.MarcaRepository;
import com.example.alquiler_de_vehiculos.repository.MetodoPagoRepository;
import com.example.alquiler_de_vehiculos.repository.ModeloRepository;
import com.example.alquiler_de_vehiculos.repository.PagoRepository;
import com.example.alquiler_de_vehiculos.repository.RolRepository;
import com.example.alquiler_de_vehiculos.repository.TipoVehiculoRepository;
import com.example.alquiler_de_vehiculos.repository.UsuarioRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoModeloRepository;
import com.example.alquiler_de_vehiculos.repository.VehiculoUnidadRepository;
import com.example.alquiler_de_vehiculos.util.TarifarioPaulCars;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EstadoVehiculoRepository estadoVehiculoRepository;
    private final EstadoAlquilerRepository estadoAlquilerRepository;
    private final RolRepository rolRepository;
    private final MarcaRepository marcaRepository;
    private final ModeloRepository modeloRepository;
    private final TipoVehiculoRepository tipoVehiculoRepository;
    private final CombustibleRepository combustibleRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final VehiculoModeloRepository vehiculoModeloRepository;
    private final VehiculoUnidadRepository vehiculoUnidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final AlquilerRepository alquilerRepository;
    private final PagoRepository pagoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(EstadoVehiculoRepository estadoVehiculoRepository,
                           EstadoAlquilerRepository estadoAlquilerRepository,
                           RolRepository rolRepository,
                           MarcaRepository marcaRepository,
                           ModeloRepository modeloRepository,
                           TipoVehiculoRepository tipoVehiculoRepository,
                           CombustibleRepository combustibleRepository,
                           MetodoPagoRepository metodoPagoRepository,
                           VehiculoModeloRepository vehiculoModeloRepository,
                           VehiculoUnidadRepository vehiculoUnidadRepository,
                           UsuarioRepository usuarioRepository,
                           ClienteRepository clienteRepository,
                           AlquilerRepository alquilerRepository,
                           PagoRepository pagoRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.estadoVehiculoRepository = estadoVehiculoRepository;
        this.estadoAlquilerRepository = estadoAlquilerRepository;
        this.rolRepository = rolRepository;
        this.marcaRepository = marcaRepository;
        this.modeloRepository = modeloRepository;
        this.tipoVehiculoRepository = tipoVehiculoRepository;
        this.combustibleRepository = combustibleRepository;
        this.metodoPagoRepository = metodoPagoRepository;
        this.vehiculoModeloRepository = vehiculoModeloRepository;
        this.vehiculoUnidadRepository = vehiculoUnidadRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.alquilerRepository = alquilerRepository;
        this.pagoRepository = pagoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Inicializando esquema y semillero operativo de 'paulcars_db'...");
        limpiarEsquemaObsoleto();
        initCatalogosBase();
        initFlotaVehiculos();
        initUsuariosCorporativos();
        initClientesYOperaciones();
        log.info("Base de datos 'paulcars_db' inicializada y poblada exitosamente.");
    }

    private void limpiarEsquemaObsoleto() {
        String[] sqls = {
                "SET FOREIGN_KEY_CHECKS = 0",
                "DROP TABLE IF EXISTS inspecciones_carroceria",
                "DROP TABLE IF EXISTS vehiculos",
                "ALTER TABLE alquileres DROP COLUMN id_vehiculo",
                "ALTER TABLE imagenes_vehiculo DROP COLUMN id_vehiculo",
                "ALTER TABLE pagos DROP COLUMN comprobante_url",
                "ALTER TABLE pagos DROP COLUMN motivo_rechazo",
                "SET FOREIGN_KEY_CHECKS = 1"
        };
        for (String sql : sqls) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception ignored) {
            }
        }
    }

    private void initCatalogosBase() {
        crearEstadoVehiculo("DISPONIBLE");
        crearEstadoVehiculo("EN_RUTA");
        crearEstadoVehiculo("ALQUILADO");
        crearEstadoVehiculo("MANTENIMIENTO");
        crearEstadoVehiculo("LIMPIEZA");
        crearEstadoVehiculo("NO DISPONIBLE");

        crearEstadoAlquiler("PENDIENTE");
        crearEstadoAlquiler("RESERVADO");
        crearEstadoAlquiler("PAGADO");
        crearEstadoAlquiler("CONFIRMADO");
        crearEstadoAlquiler("ACTIVO");
        crearEstadoAlquiler("FINALIZADO");
        crearEstadoAlquiler("CANCELADO");
        crearEstadoAlquiler("RECHAZADO");
        crearEstadoAlquiler("RECHAZADO_DOCUMENTACION");
        crearEstadoAlquiler("CANCELADO_SIN_STOCK");

        crearMetodoPago("TARJETA");
        crearMetodoPago("EFECTIVO");
        crearMetodoPago("YAPE");
        crearMetodoPago("PLIN");
        crearMetodoPago("TRANSFERENCIA");

        crearRol("ADMIN", "Administrador principal del sistema");
        crearRol("EMPLEADO", "Personal de atención en counter y operaciones de flota");
        crearRol("CLIENTE", "Cliente titular de reservas y alquileres");

        obtenerOCrearTipo("SEDAN");
        obtenerOCrearTipo("SUV");
        obtenerOCrearTipo("PICKUP");
        obtenerOCrearTipo("HATCHBACK");

        obtenerOCrearCombustible("GASOLINA");
        obtenerOCrearCombustible("DIESEL");
        obtenerOCrearCombustible("HIBRIDO");
    }

    private record UnidadSeed(String placa, String vin, String motor, int km, String sede, String estadoInicial) {}

    private void initFlotaVehiculos() {
        Set<String> modelosOficiales = Set.of(
                "Corolla", "Versa", "3",
                "Tucson", "Kicks", "ASX",
                "Hilux", "L200", "Ranger",
                "Rio", "Swift"
        );
        for (VehiculoModelo m : vehiculoModeloRepository.findAll()) {
            if (!modelosOficiales.contains(m.getNombreModelo())) {
                if (m.getUnidades() != null) {
                    vehiculoUnidadRepository.deleteAll(m.getUnidades());
                }
                vehiculoModeloRepository.delete(m);
            }
        }

        asegurarModeloYUnidades(
                "Toyota", "Corolla", 2022, "SEDAN", "GASOLINA",
                BigDecimal.valueOf(120), BigDecimal.valueOf(15), BigDecimal.valueOf(3500), "AEROPUERTO",
                List.of("/images/vehiculos/corolla.jpg", "/images/vehiculos/corolla_side.jpg", "/images/vehiculos/corolla_rear.jpg", "/images/vehiculos/corolla_interior.jpg"),
                List.of(
                        new UnidadSeed("ABC-121", "VINTOY00000000101", "MTR001-A", 12500, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("ABC-122", "VINTOY00000000102", "MTR001-B", 18200, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("ABC-123", "VINTOY00000000103", "MTR001-C", 9400, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("ABC-124", "VINTOY00000000104", "MTR001-D", 15300, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("ABC-125", "VINTOY00000000105", "MTR001-E", 11200, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("ABC-126", "VINTOY00000000106", "MTR001-F", 14100, "MIRAFLORES", "MANTENIMIENTO")
                ));

        asegurarModeloYUnidades(
                "Nissan", "Versa", 2023, "SEDAN", "GASOLINA",
                BigDecimal.valueOf(110), BigDecimal.valueOf(14), BigDecimal.valueOf(3500), "AEROPUERTO",
                List.of("/images/vehiculos/versa.jpg", "/images/vehiculos/versa_side.jpg", "/images/vehiculos/versa_rear.jpg", "/images/vehiculos/versa_interior.jpg"),
                List.of(
                        new UnidadSeed("NSS-201", "VINNIS00000000201", "MTR006-A", 8200, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("NSS-202", "VINNIS00000000202", "MTR006-B", 11400, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("NSS-203", "VINNIS00000000203", "MTR006-C", 14500, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("NSS-204", "VINNIS00000000204", "MTR006-D", 9900, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("NSS-205", "VINNIS00000000205", "MTR006-E", 13200, "AEROPUERTO", "LIMPIEZA")
                ));

        asegurarModeloYUnidades(
                "Mazda", "3", 2023, "SEDAN", "GASOLINA",
                BigDecimal.valueOf(125), BigDecimal.valueOf(15), BigDecimal.valueOf(3500), "MIRAFLORES",
                List.of("/images/vehiculos/mazda3.jpg", "/images/vehiculos/mazda3_side.jpg", "/images/vehiculos/mazda3_rear.jpg", "/images/vehiculos/mazda3_interior.jpg"),
                List.of(
                        new UnidadSeed("MZD-401", "VINMZD00000000401", "MTR008-A", 13200, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("MZD-402", "VINMZD00000000402", "MTR008-B", 8500, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("MZD-403", "VINMZD00000000403", "MTR008-C", 10900, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("MZD-404", "VINMZD00000000404", "MTR008-D", 14700, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("MZD-405", "VINMZD00000000405", "MTR008-E", 11800, "MIRAFLORES", "MANTENIMIENTO")
                ));

        asegurarModeloYUnidades(
                "Hyundai", "Tucson", 2023, "SUV", "GASOLINA",
                BigDecimal.valueOf(180), BigDecimal.valueOf(22), BigDecimal.valueOf(5000), "AEROPUERTO",
                List.of("/images/vehiculos/tucson.jpg", "/images/vehiculos/tucson_side.jpg", "/images/vehiculos/tucson_rear.jpg", "/images/vehiculos/tucson_interior.jpg"),
                List.of(
                        new UnidadSeed("HYU-801", "VINHYU00000000801", "MTR011-A", 10500, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("HYU-802", "VINHYU00000000802", "MTR011-B", 13200, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("HYU-803", "VINHYU00000000803", "MTR011-C", 7800, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("HYU-804", "VINHYU00000000804", "MTR011-D", 15400, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("HYU-805", "VINHYU00000000805", "MTR011-E", 9200, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("HYU-806", "VINHYU00000000806", "MTR011-F", 11900, "MIRAFLORES", "LIMPIEZA"),
                        new UnidadSeed("HYU-807", "VINHYU00000000807", "MTR011-G", 14300, "AEROPUERTO", "MANTENIMIENTO")
                ));

        asegurarModeloYUnidades(
                "Nissan", "Kicks", 2024, "SUV", "HIBRIDO",
                BigDecimal.valueOf(160), BigDecimal.valueOf(20), BigDecimal.valueOf(5000), "MIRAFLORES",
                List.of("/images/vehiculos/kicks.jpg", "/images/vehiculos/kicks_side.jpg", "/images/vehiculos/kicks_rear.jpg", "/images/vehiculos/kicks_interior.jpg"),
                List.of(
                        new UnidadSeed("NIS-901", "VINNIS00000000901", "MTR012-A", 6900, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("NIS-902", "VINNIS00000000902", "MTR012-B", 11800, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("NIS-903", "VINNIS00000000903", "MTR012-C", 9400, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("NIS-904", "VINNIS00000000904", "MTR012-D", 12700, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("NIS-905", "VINNIS00000000905", "MTR012-E", 8600, "MIRAFLORES", "LIMPIEZA")
                ));

        asegurarModeloYUnidades(
                "Mitsubishi", "ASX", 2023, "SUV", "GASOLINA",
                BigDecimal.valueOf(155), BigDecimal.valueOf(19), BigDecimal.valueOf(5000), "MIRAFLORES",
                List.of("/images/vehiculos/asx.jpg", "/images/vehiculos/asx_side.jpg", "/images/vehiculos/asx_rear.jpg", "/images/vehiculos/asx_interior.jpg"),
                List.of(
                        new UnidadSeed("ASX-101", "VINMIT00000001101", "MTR014-A", 11200, "MIRAFLORES", "EN_RUTA"),
                        new UnidadSeed("ASX-102", "VINMIT00000001102", "MTR014-B", 15400, "MIRAFLORES", "MANTENIMIENTO"),
                        new UnidadSeed("ASX-103", "VINMIT00000001103", "MTR014-C", 9300, "AEROPUERTO", "MANTENIMIENTO"),
                        new UnidadSeed("ASX-104", "VINMIT00000001104", "MTR014-D", 13700, "AEROPUERTO", "LIMPIEZA"),
                        new UnidadSeed("ASX-105", "VINMIT00000001105", "MTR014-E", 10100, "MIRAFLORES", "LIMPIEZA")
                ));

        asegurarModeloYUnidades(
                "Toyota", "Hilux", 2024, "PICKUP", "DIESEL",
                BigDecimal.valueOf(230), BigDecimal.valueOf(28), BigDecimal.valueOf(7000), "AEROPUERTO",
                List.of("/images/vehiculos/hilux.jpg", "/images/vehiculos/hilux_side.jpg", "/images/vehiculos/hilux_rear.jpg", "/images/vehiculos/hilux_interior.jpg"),
                List.of(
                        new UnidadSeed("HLX-301", "VINTOY00000000301", "MTR003-A", 9800, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("HLX-302", "VINTOY00000000302", "MTR003-B", 14300, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("HLX-303", "VINTOY00000000303", "MTR003-C", 18100, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("HLX-304", "VINTOY00000000304", "MTR003-D", 11200, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("HLX-305", "VINTOY00000000305", "MTR003-E", 15400, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("HLX-306", "VINTOY00000000306", "MTR003-F", 8700, "MIRAFLORES", "MANTENIMIENTO"),
                        new UnidadSeed("HLX-307", "VINTOY00000000307", "MTR003-G", 13900, "AEROPUERTO", "LIMPIEZA")
                ));

        asegurarModeloYUnidades(
                "Mitsubishi", "L200", 2023, "PICKUP", "DIESEL",
                BigDecimal.valueOf(215), BigDecimal.valueOf(26), BigDecimal.valueOf(7000), "MIRAFLORES",
                List.of("/images/vehiculos/l200.jpg", "/images/vehiculos/l200_side.jpg", "/images/vehiculos/l200_rear.jpg", "/images/vehiculos/l200_interior.jpg"),
                List.of(
                        new UnidadSeed("L20-501", "VINMIT00000001501", "MTR017-A", 13100, "MIRAFLORES", "EN_RUTA"),
                        new UnidadSeed("L20-502", "VINMIT00000001502", "MTR017-B", 18200, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("L20-503", "VINMIT00000001503", "MTR017-C", 9700, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("L20-504", "VINMIT00000001504", "MTR017-D", 15600, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("L20-505", "VINMIT00000001505", "MTR017-E", 11800, "MIRAFLORES", "MANTENIMIENTO")
                ));

        asegurarModeloYUnidades(
                "Ford", "Ranger", 2024, "PICKUP", "DIESEL",
                BigDecimal.valueOf(235), BigDecimal.valueOf(29), BigDecimal.valueOf(7000), "AEROPUERTO",
                List.of("/images/vehiculos/ranger.jpg", "/images/vehiculos/ranger_side.jpg", "/images/vehiculos/ranger_rear.jpg", "/images/vehiculos/ranger_interior.jpg"),
                List.of(
                        new UnidadSeed("RNG-601", "VINFRD00000001601", "MTR018-A", 8400, "AEROPUERTO", "EN_RUTA"),
                        new UnidadSeed("RNG-602", "VINFRD00000001602", "MTR018-B", 12300, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("RNG-603", "VINFRD00000001603", "MTR018-C", 10100, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("RNG-604", "VINFRD00000001604", "MTR018-D", 15800, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("RNG-605", "VINFRD00000001605", "MTR018-E", 9200, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("RNG-606", "VINFRD00000001606", "MTR018-F", 13700, "MIRAFLORES", "LIMPIEZA")
                ));

        asegurarModeloYUnidades(
                "Kia", "Rio", 2022, "HATCHBACK", "GASOLINA",
                BigDecimal.valueOf(90), BigDecimal.valueOf(12), BigDecimal.valueOf(2500), "AEROPUERTO",
                List.of("/images/vehiculos/rio.jpg", "/images/vehiculos/rio_side.jpg", "/images/vehiculos/rio_rear.jpg", "/images/vehiculos/rio_interior.jpg"),
                List.of(
                        new UnidadSeed("JKL-011", "VINKIA00000001801", "MTR004-A", 16400, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("JKL-012", "VINKIA00000001802", "MTR004-B", 18900, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("JKL-013", "VINKIA00000001803", "MTR004-C", 14300, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("JKL-014", "VINKIA00000001804", "MTR004-D", 19200, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("JKL-015", "VINKIA00000001805", "MTR004-E", 12100, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("JKL-016", "VINKIA00000001806", "MTR004-F", 15800, "MIRAFLORES", "MANTENIMIENTO")
                ));

        asegurarModeloYUnidades(
                "Suzuki", "Swift", 2024, "HATCHBACK", "GASOLINA",
                BigDecimal.valueOf(88), BigDecimal.valueOf(11), BigDecimal.valueOf(2500), "AEROPUERTO",
                List.of("/images/vehiculos/swift.jpg", "/images/vehiculos/swift_side.jpg", "/images/vehiculos/swift_rear.jpg", "/images/vehiculos/swift_interior.jpg"),
                List.of(
                        new UnidadSeed("SZK-701", "VINSZK00000002001", "MTR021-A", 8600, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("SZK-702", "VINSZK00000002002", "MTR021-B", 11200, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("SZK-703", "VINSZK00000002003", "MTR021-C", 9400, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("SZK-704", "VINSZK00000002004", "MTR021-D", 10300, "MIRAFLORES", "DISPONIBLE"),
                        new UnidadSeed("SZK-705", "VINSZK00000002005", "MTR021-E", 12700, "AEROPUERTO", "DISPONIBLE"),
                        new UnidadSeed("SZK-706", "VINSZK00000002006", "MTR021-F", 7900, "MIRAFLORES", "LIMPIEZA")
                ));
    }

    private VehiculoModelo asegurarModeloYUnidades(String nombreMarca, String nombreModelo, int anio, String nombreTipo, String nombreCombustible,
                                                   BigDecimal precioDia, BigDecimal precioHora, BigDecimal garantia,
                                                   String sede, List<String> fotos, List<UnidadSeed> unidadesSeed) {
        Marca marca = obtenerOCrearMarca(nombreMarca);
        obtenerOCrearModelo(nombreModelo, marca);
        TipoVehiculo tipo = obtenerOCrearTipo(nombreTipo);
        Combustible combustible = obtenerOCrearCombustible(nombreCombustible);

        VehiculoModelo modelo = vehiculoModeloRepository.findAll().stream()
                .filter(m -> m.getMarca() != null && nombreMarca.equalsIgnoreCase(m.getMarca().getNombreMarca())
                        && nombreModelo.equalsIgnoreCase(m.getNombreModelo()))
                .findFirst()
                .orElse(null);

        if (modelo == null) {
            modelo = new VehiculoModelo();
            modelo.setMarca(marca);
            modelo.setNombreModelo(nombreModelo);
            modelo.setAnio(anio);
            modelo.setTipo(tipo);
            modelo.setCombustible(combustible);
            modelo.setPrecioDia(precioDia);
            modelo.setPrecioHora(precioHora);
            modelo.setGarantia(garantia != null ? garantia : BigDecimal.valueOf(3500.00));
            modelo.setActivo(true);
            modelo.setSede(sede);
            if (fotos != null && !fotos.isEmpty()) {
                modelo.setImagenUrl(fotos.get(0));
            }
            modelo = vehiculoModeloRepository.save(modelo);
        }

        if (modelo.getImagenes() == null || modelo.getImagenes().isEmpty()) {
            if (fotos != null && !fotos.isEmpty()) {
                if (modelo.getImagenes() == null) {
                    modelo.setImagenes(new LinkedHashSet<>());
                }
                for (int i = 0; i < fotos.size(); i++) {
                    modelo.getImagenes().add(new ImagenVehiculo(modelo, fotos.get(i), i + 1));
                }
                modelo.setImagenUrl(fotos.get(0));
                modelo = vehiculoModeloRepository.save(modelo);
            }
        }

        for (UnidadSeed uSeed : unidadesSeed) {
            EstadoVehiculo estadoUnidad = crearEstadoVehiculo(uSeed.estadoInicial != null ? uSeed.estadoInicial : "DISPONIBLE");
            VehiculoUnidad u = vehiculoUnidadRepository.findByPlacaIgnoreCase(uSeed.placa).orElse(null);
            if (u == null) {
                u = new VehiculoUnidad();
                u.setVehiculoModelo(modelo);
                u.setPlaca(uSeed.placa);
                u.setNumeroVin(uSeed.vin);
                u.setNumeroMotor(uSeed.motor);
                u.setColor(modelo.getColor());
                u.setKilometraje(uSeed.km);
                u.setSede(uSeed.sede != null ? uSeed.sede : sede);
                u.setEstado(estadoUnidad);
                vehiculoUnidadRepository.save(u);
            } else {
                u.setColor(modelo.getColor());
                u.setEstado(estadoUnidad);
                vehiculoUnidadRepository.save(u);
            }
        }

        return modelo;
    }

    private void initUsuariosCorporativos() {
        Rol rolAdmin = rolRepository.findByNombreRolIgnoreCase("ADMIN").orElseThrow();
        Rol rolEmpleado = rolRepository.findByNombreRolIgnoreCase("EMPLEADO").orElseThrow();

        crearUsuarioSiNoExiste("admin@paulcars.com", "admin123", "admin@paulcars.com",
                "Paul Cars Administrador", rolAdmin, null, true);

        crearUsuarioSiNoExiste("asesor@paulcars.com", "asesor123", "asesor@paulcars.com",
                "Carlos Mendoza - Asesor Principal", rolEmpleado, null, true);

        crearUsuarioSiNoExiste("asesor1@paulcars.com", "asesor123", "asesor1@paulcars.com",
                "Carlos Mendoza · Counter Miraflores", rolEmpleado, null, true);

        crearUsuarioSiNoExiste("counter.aeropuerto@paulcars.com", "asesor123", "counter.aeropuerto@paulcars.com",
                "Sofía Rodríguez · Counter Aeropuerto", rolEmpleado, null, true);

        crearUsuarioSiNoExiste("carlos.mendoza@paulcars.com", "asesor123", "carlos.mendoza@paulcars.com",
                "Carlos Mendoza", rolEmpleado, null, true);

        crearUsuarioSiNoExiste("sofia.rodriguez@paulcars.com", "asesor123", "sofia.rodriguez@paulcars.com",
                "Sofía Rodríguez", rolEmpleado, null, true);
    }

    private void initClientesYOperaciones() {
        if (alquilerRepository.count() > 0) {
            return;
        }

        Rol rolCliente = rolRepository.findByNombreRolIgnoreCase("CLIENTE").orElseThrow();

        Cliente c1 = asegurarCliente(
                "Carlos Alberto", "Mendoza Ríos", "DNI", "72458910",
                "987451230", "carlos.mendoza92@gmail.com", "Q72458910",
                "Av. José Larco 740, Miraflores, Lima", "ACTIVO", rolCliente);

        Cliente c2 = asegurarCliente(
                "Valeria Fernanda", "Torres Paredes", "DNI", "74819203",
                "991234567", "valeria.torres.p@gmail.com", "Q74819203",
                "Calle Los Pinos 320, San Isidro, Lima", "ACTIVO", rolCliente);

        Cliente c3 = asegurarCliente(
                "Diego Alejandro", "Álvarez Vargas", "DNI", "71938475",
                "984567123", "diego.alvarez.v@gmail.com", "Q71938475",
                "Av. Primavera 1450, Santiago de Surco, Lima", "ACTIVO", rolCliente);

        Cliente c4 = asegurarCliente(
                "Camila Lucía", "Salazar Montes", "DNI", "75647382",
                "976543219", "camila.salazar.m@gmail.com", "Q75647382",
                "Av. Salaverry 2410, Jesús María, Lima", "ACTIVO", rolCliente);

        Cliente c5 = asegurarCliente(
                "Rodrigo Sebastián", "Castillo Guzmán", "DNI", "70829164",
                "965432187", "rodrigo.castillo.g@gmail.com", "Q70829164",
                "Malecón Cisneros 890, Miraflores, Lima", "ACTIVO", rolCliente);

        asegurarCliente(
                "Lucía Gabriela", "Fernández Castro", "DNI", "76123984",
                "954321876", "lucia.fernandez.c@gmail.com", null,
                "Av. Benavides 1890, Miraflores, Lima", "ACTIVO", rolCliente);

        asegurarCliente(
                "Mateo Nicolás", "Ramírez Delgado", null, null,
                "943218765", "mateo.ramirez.d@gmail.com", null,
                null, "ACTIVO", rolCliente);

        LocalDate hoy = LocalDate.now();

        EstadoAlquiler stPendiente = crearEstadoAlquiler("PENDIENTE");
        EstadoAlquiler stReservado = crearEstadoAlquiler("RESERVADO");
        EstadoAlquiler stPagado = crearEstadoAlquiler("PAGADO");
        EstadoAlquiler stActivo = crearEstadoAlquiler("ACTIVO");
        EstadoAlquiler stFinalizado = crearEstadoAlquiler("FINALIZADO");

        MetodoPago mpTarjeta = crearMetodoPago("TARJETA");
        MetodoPago mpYape = crearMetodoPago("YAPE");
        MetodoPago mpPlin = crearMetodoPago("PLIN");
        MetodoPago mpEfectivo = crearMetodoPago("EFECTIVO");

        Alquiler fin1 = registrarContratoSeed(
                c1, buscarModelo("Corolla"), buscarUnidad("ABC-121"),
                hoy.minusDays(19), hoy.minusDays(18), hoy.minusDays(15), hoy.minusDays(15),
                "10:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                true, false, false, stFinalizado,
                BigDecimal.valueOf(3500.00), BigDecimal.valueOf(3500.00),
                hoy.minusDays(15).atTime(17, 45),
                "Recepción conforme en counter Miraflores. Kilometraje y nivel de combustible verificados sin observaciones.");
        registrarPagoSeed(fin1, mpTarjeta, hoy.minusDays(18), "DIGITAL", "4532",
                "VISA • Titular: CARLOS MENDOZA (Token Culqi: tok_culqi_901011)", "TRX-CARD-90101122", "CULQI_GATEWAY_PCI_DSS");

        Alquiler fin2 = registrarContratoSeed(
                c2, buscarModelo("Swift"), buscarUnidad("SZK-701"),
                hoy.minusDays(15), hoy.minusDays(14), hoy.minusDays(11), hoy.minusDays(11),
                "09:00", "17:00", "AEROPUERTO", "AEROPUERTO",
                false, false, true, stFinalizado,
                BigDecimal.valueOf(2500.00), BigDecimal.valueOf(2500.00),
                hoy.minusDays(11).atTime(16, 50),
                "Devolución puntual en counter Aeropuerto Jorge Chávez. Sin incidencias en carrocería ni interior.");
        registrarPagoSeed(fin2, mpPlin, hoy.minusDays(14), "DIGITAL", null,
                "PLIN (BBVA - 991234567)", "TRX-PLIN-90102233", "PLIN_GATEWAY_INTEROPERABLE");

        Alquiler fin3 = registrarContratoSeed(
                c1, buscarModelo("Tucson"), buscarUnidad("HYU-801"),
                hoy.minusDays(11), hoy.minusDays(10), hoy.minusDays(6), hoy.minusDays(6),
                "11:00", "18:00", "AEROPUERTO", "MIRAFLORES",
                true, true, false, stFinalizado,
                BigDecimal.valueOf(5000.00), BigDecimal.valueOf(5000.00),
                hoy.minusDays(6).atTime(17, 30),
                "Unidad devuelta en óptimas condiciones operativas. Garantía HOLD liberada al 100%.");
        registrarPagoSeed(fin3, mpYape, hoy.minusDays(10), "DIGITAL", null,
                "YAPE (987451230)", "TRX-YAPE-90103344", "YAPE_GATEWAY_CULQI");

        Alquiler fin4 = registrarContratoSeed(
                c3, buscarModelo("Hilux"), buscarUnidad("HLX-302"),
                hoy.minusDays(10), hoy.minusDays(9), hoy.minusDays(5), hoy.minusDays(5),
                "08:30", "18:00", "AEROPUERTO", "AEROPUERTO",
                true, false, false, stFinalizado,
                BigDecimal.valueOf(7000.00), BigDecimal.valueOf(7000.00),
                hoy.minusDays(5).atTime(18, 0),
                "Inspección de retorno aprobada. Unidad trasladada a lavado estándar de patio.");
        registrarPagoSeed(fin4, mpTarjeta, hoy.minusDays(9), "DIGITAL", "8841",
                "MASTERCARD • Titular: DIEGO ALVAREZ (Token Culqi: tok_culqi_901044)", "TRX-CARD-90104455", "CULQI_GATEWAY_PCI_DSS");

        Alquiler fin5 = registrarContratoSeed(
                c4, buscarModelo("Versa"), buscarUnidad("NSS-201"),
                hoy.minusDays(7), hoy.minusDays(6), hoy.minusDays(4), hoy.minusDays(4),
                "10:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                false, true, false, stFinalizado,
                BigDecimal.valueOf(3500.00), BigDecimal.valueOf(3500.00),
                hoy.minusDays(4).atTime(17, 15),
                "Check-out completado sin cargos adicionales. Entrega de acta de conformidad al titular.");
        registrarPagoSeed(fin5, mpEfectivo, hoy.minusDays(6), "COUNTER", null,
                null, "POS-EFE-001055", "COUNTER_PRESENCIAL");

        Alquiler fin6 = registrarContratoSeed(
                c5, buscarModelo("Rio"), buscarUnidad("JKL-012"),
                hoy.minusDays(6), hoy.minusDays(5), hoy.minusDays(2), hoy.minusDays(2),
                "09:00", "18:00", "MIRAFLORES", "AEROPUERTO",
                false, false, false, stFinalizado,
                BigDecimal.valueOf(2500.00), BigDecimal.valueOf(2500.00),
                hoy.minusDays(2).atTime(17, 40),
                "Vehículo recibido dentro del horario pactado y en perfecto estado mecánico y estético.");
        registrarPagoSeed(fin6, mpYape, hoy.minusDays(5), "DIGITAL", null,
                "YAPE (965432187)", "TRX-YAPE-90106677", "YAPE_GATEWAY_CULQI");

        Alquiler act1 = registrarContratoSeed(
                c1, buscarModelo("Ranger"), buscarUnidad("RNG-601"),
                hoy.minusDays(3), hoy.minusDays(2), hoy, null,
                "09:00", "18:00", "AEROPUERTO", "AEROPUERTO",
                true, false, false, stActivo,
                BigDecimal.valueOf(7000.00), null, null, null);
        registrarPagoSeed(act1, mpTarjeta, hoy.minusDays(2), "DIGITAL", "4532",
                "VISA • Titular: CARLOS MENDOZA (Token Culqi: tok_culqi_902011)", "TRX-CARD-90201188", "CULQI_GATEWAY_PCI_DSS");

        Alquiler act2 = registrarContratoSeed(
                c3, buscarModelo("ASX"), buscarUnidad("ASX-101"),
                hoy.minusDays(2), hoy.minusDays(1), hoy.plusDays(2), null,
                "10:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                true, false, true, stActivo,
                BigDecimal.valueOf(5000.00), null, null, null);
        registrarPagoSeed(act2, mpPlin, hoy.minusDays(1), "DIGITAL", null,
                "PLIN (INTERBANK - 984567123)", "TRX-PLIN-90202299", "PLIN_GATEWAY_INTEROPERABLE");

        Alquiler act3 = registrarContratoSeed(
                c5, buscarModelo("L200"), buscarUnidad("L20-501"),
                hoy.minusDays(3), hoy.minusDays(2), hoy.plusDays(1), null,
                "08:30", "17:00", "MIRAFLORES", "AEROPUERTO",
                false, true, false, stActivo,
                BigDecimal.valueOf(7000.00), null, null, null);
        registrarPagoSeed(act3, mpEfectivo, hoy.minusDays(2), "COUNTER", null,
                null, "POS-EFE-002033", "COUNTER_PRESENCIAL");

        Alquiler pag1 = registrarContratoSeed(
                c1, buscarModelo("3"), buscarUnidad("MZD-401"),
                hoy.minusDays(1), hoy, hoy.plusDays(3), null,
                "10:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                true, false, false, stPagado,
                null, null, null, null);
        registrarPagoSeed(pag1, mpTarjeta, hoy.minusDays(1), "DIGITAL", "4532",
                "VISA • Titular: CARLOS MENDOZA (Token Culqi: tok_culqi_903011)", "TRX-CARD-90301144", "CULQI_GATEWAY_PCI_DSS");

        Alquiler pag2 = registrarContratoSeed(
                c5, buscarModelo("Hilux"), buscarUnidad("HLX-301"),
                hoy, hoy.plusDays(2), hoy.plusDays(5), null,
                "09:00", "18:00", "AEROPUERTO", "AEROPUERTO",
                true, true, false, stPagado,
                null, null, null, null);
        registrarPagoSeed(pag2, mpYape, hoy, "DIGITAL", null,
                "YAPE (965432187)", "TRX-YAPE-90302255", "YAPE_GATEWAY_CULQI");

        registrarContratoSeed(
                c1, buscarModelo("Kicks"), buscarUnidad("NIS-901"),
                hoy.minusDays(1), hoy, hoy.plusDays(2), null,
                "11:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                false, true, false, stReservado,
                null, null, null, null);

        registrarContratoSeed(
                c4, buscarModelo("Rio"), buscarUnidad("JKL-011"),
                hoy, hoy.plusDays(1), hoy.plusDays(4), null,
                "10:00", "18:00", "AEROPUERTO", "MIRAFLORES",
                false, false, true, stReservado,
                null, null, null, null);

        registrarContratoSeed(
                c1, buscarModelo("Corolla"), null,
                hoy, hoy.plusDays(1), hoy.plusDays(4), null,
                "09:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                true, false, false, stPendiente,
                null, null, null, null);

        registrarContratoSeed(
                c3, buscarModelo("Tucson"), null,
                hoy, hoy.plusDays(2), hoy.plusDays(6), null,
                "10:00", "18:00", "AEROPUERTO", "AEROPUERTO",
                true, true, false, stPendiente,
                null, null, null, null);

        registrarContratoSeed(
                c4, buscarModelo("Swift"), null,
                hoy, hoy.plusDays(1), hoy.plusDays(3), null,
                "12:00", "18:00", "MIRAFLORES", "AEROPUERTO",
                false, false, true, stPendiente,
                null, null, null, null);
    }

    private Cliente asegurarCliente(String nombres, String apellidos, String tipoDoc, String numDoc,
                                    String telefono, String email, String licencia, String direccion,
                                    String estadoCliente, Rol rolCliente) {
        Cliente cliente = clienteRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    Cliente c = new Cliente();
                    c.setNombres(nombres);
                    c.setApellidos(apellidos);
                    c.setTipoDocumento(tipoDoc);
                    c.setNumeroDocumento(numDoc);
                    c.setTelefono(telefono);
                    c.setEmail(email);
                    c.setLicenciaConducir(licencia);
                    c.setDireccion(direccion);
                    c.setEstadoCliente(estadoCliente);
                    return clienteRepository.save(c);
                });
        crearUsuarioSiNoExiste(email, "cliente123", email,
                nombres + " " + apellidos, rolCliente, cliente, true);
        return cliente;
    }

    private Alquiler registrarContratoSeed(Cliente cliente, VehiculoModelo modelo, VehiculoUnidad unidad,
                                           LocalDate fechaReserva, LocalDate fechaInicio, LocalDate fechaFinEstimada,
                                           LocalDate fechaFinReal, String horaRecojo, String horaDevolucion,
                                           String sedeRecojo, String sedeDevolucion,
                                           boolean seguro, boolean conductor, boolean silla,
                                           EstadoAlquiler estado, BigDecimal garantiaRetenida,
                                           BigDecimal garantiaLiberada, LocalDateTime fechaCheckout,
                                           String actaObservaciones) {
        int dias = (int) Math.max(1, ChronoUnit.DAYS.between(fechaInicio, fechaFinEstimada));
        BigDecimal precioDia = TarifarioPaulCars.nz(modelo.getPrecioDia());
        BigDecimal base = precioDia.multiply(BigDecimal.valueOf(dias));
        BigDecimal extras = TarifarioPaulCars.extrasPorDia(seguro, conductor, silla).multiply(BigDecimal.valueOf(dias));
        BigDecimal total = base.add(extras);

        Alquiler alq = new Alquiler();
        alq.setCliente(cliente);
        alq.setVehiculoModelo(modelo);
        alq.setVehiculoUnidad(unidad);
        alq.setFechaReserva(fechaReserva);
        alq.setFechaInicio(fechaInicio);
        alq.setFechaFinEstimada(fechaFinEstimada);
        alq.setFechaFinReal(fechaFinReal);
        alq.setHoraRecojo(horaRecojo);
        alq.setHoraDevolucion(horaDevolucion);
        alq.setSedeRecojo(sedeRecojo);
        alq.setSedeDevolucion(sedeDevolucion);
        alq.setDias(dias);
        alq.setPrecioBase(TarifarioPaulCars.redondear(base).doubleValue());
        alq.setMontoExtras(TarifarioPaulCars.redondear(extras).doubleValue());
        alq.setPrecioTotal(TarifarioPaulCars.redondear(total).doubleValue());
        alq.setServiciosExtras(TarifarioPaulCars.textoExtras(seguro, conductor, silla));
        alq.setEstado(estado);
        alq.setGarantiaRetenida(garantiaRetenida);
        alq.setGarantiaLiberada(garantiaLiberada);
        alq.setFechaCheckout(fechaCheckout);
        alq.setActaObservaciones(actaObservaciones);
        return alquilerRepository.save(alq);
    }

    private void registrarPagoSeed(Alquiler alquiler, MetodoPago metodo, LocalDate fechaPago,
                                   String canal, String ultimosDigitos, String bancoOrigen,
                                   String numeroOperacion, String validadoPor) {
        BigDecimal monto = BigDecimal.valueOf(alquiler.getPrecioTotal());
        Pago pago = new Pago();
        pago.setAlquiler(alquiler);
        pago.setMetodoPago(metodo);
        pago.setMontoTotal(TarifarioPaulCars.redondear(monto));
        pago.setFechaPago(fechaPago);
        pago.setEstadoPago("Pagado");
        pago.setTipoPago("ALQUILER");
        pago.setCanal(canal);
        if ("EFECTIVO".equalsIgnoreCase(metodo.getNombreMetodo())) {
            pago.setMontoRecibido(TarifarioPaulCars.redondear(monto));
            pago.setVuelto(BigDecimal.ZERO);
        }
        pago.setUltimosDigitos(ultimosDigitos);
        pago.setBancoOrigen(bancoOrigen);
        pago.setNumeroOperacion(numeroOperacion);
        pago.setValidadoPor(validadoPor);
        pago.setFechaValidacion(fechaPago.atTime(10, 15));
        pagoRepository.save(pago);
    }

    private VehiculoModelo buscarModelo(String nombreModelo) {
        return vehiculoModeloRepository.findAll().stream()
                .filter(m -> nombreModelo.equalsIgnoreCase(m.getNombreModelo()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Modelo no encontrado en seed: " + nombreModelo));
    }

    private VehiculoUnidad buscarUnidad(String placa) {
        return vehiculoUnidadRepository.findByPlacaIgnoreCase(placa)
                .orElseThrow(() -> new IllegalStateException("Unidad no encontrada en seed: " + placa));
    }

    private void crearUsuarioSiNoExiste(String username, String rawPassword, String email,
                                        String nombreCompleto, Rol rol, Cliente cliente, boolean estado) {
        if (usuarioRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            Usuario u = new Usuario();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(rawPassword));
            u.setEmailRecuperacion(email);
            u.setNombreCompleto(nombreCompleto);
            u.setRol(rol);
            u.setCliente(cliente);
            u.setEstado(estado);
            u.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(u);
            log.info("Usuario seed creado: [{}] con rol [{}]", username, rol.getNombreRol());
        }
    }

    private MetodoPago crearMetodoPago(String nombre) {
        return metodoPagoRepository.findByNombreMetodo(nombre)
                .orElseGet(() -> metodoPagoRepository.save(new MetodoPago(null, nombre)));
    }

    private EstadoVehiculo crearEstadoVehiculo(String nombre) {
        return estadoVehiculoRepository.findByNombreEstadoIgnoreCase(nombre)
                .orElseGet(() -> estadoVehiculoRepository.save(new EstadoVehiculo(null, nombre)));
    }

    private EstadoAlquiler crearEstadoAlquiler(String nombre) {
        return estadoAlquilerRepository.findByNombreEstadoIgnoreCase(nombre)
                .orElseGet(() -> estadoAlquilerRepository.save(new EstadoAlquiler(null, nombre)));
    }

    private Rol crearRol(String nombre, String descripcion) {
        return rolRepository.findByNombreRolIgnoreCase(nombre)
                .orElseGet(() -> rolRepository.save(new Rol(null, nombre, descripcion)));
    }

    private Marca obtenerOCrearMarca(String nombre) {
        return marcaRepository.findAll().stream()
                .filter(m -> m.getNombreMarca().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseGet(() -> marcaRepository.save(new Marca(null, nombre)));
    }

    private Modelo obtenerOCrearModelo(String nombre, Marca marca) {
        return modeloRepository.findAll().stream()
                .filter(m -> m.getModelo().equalsIgnoreCase(nombre) && m.getMarca().getIdMarca().equals(marca.getIdMarca()))
                .findFirst()
                .orElseGet(() -> modeloRepository.save(new Modelo(null, nombre, marca)));
    }

    private TipoVehiculo obtenerOCrearTipo(String nombre) {
        return tipoVehiculoRepository.findAll().stream()
                .filter(t -> t.getNombreTipo().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseGet(() -> tipoVehiculoRepository.save(new TipoVehiculo(null, nombre)));
    }

    private Combustible obtenerOCrearCombustible(String nombre) {
        return combustibleRepository.findAll().stream()
                .filter(c -> c.getNombreCombustible().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseGet(() -> combustibleRepository.save(new Combustible(null, nombre)));
    }
}
