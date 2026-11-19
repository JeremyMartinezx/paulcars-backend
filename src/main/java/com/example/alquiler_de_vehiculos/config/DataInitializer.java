package com.example.alquiler_de_vehiculos.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.List;

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
import com.example.alquiler_de_vehiculos.model.HistorialEstadoAlquiler;
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
        // Los modelos que registra el administrador se conservan: antes se borraban en cada arranque.

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

        // Solo un administrador y un asesor
        // Cada persona con sus nombres, apellidos y DNI por separado, y su correo personal; el cargo sale del rol
        crearUsuarioSiNoExiste("alexander.ramos@paulcars.com", "admin123", "alexander.ramos@paulcars.com",
                "Alexander", "Ramos Torres", "40123456", rolAdmin);

        crearUsuarioSiNoExiste("carlos.mendoza@paulcars.com", "asesor123", "carlos.mendoza@paulcars.com",
                "Carlos", "Mendoza Rivas", "45678912", rolEmpleado);
    }

    private static final String ASESOR_SEED = "Carlos Mendoza Rivas";
    private static final List<String> CHECKLIST_SEED = List.of(
            "Tarjeta de propiedad y SOAT vigentes",
            "Llanta de repuesto y gata",
            "Triángulo de seguridad y extintor",
            "Interior limpio y sin daños",
            "Carrocería revisada (rayones o abolladuras)",
            "Luces y accesorios en funcionamiento");

    /**
     * Datos de demostración: 5 clientes y 12 alquileres (2 por estado: pendiente, reservado por pagar,
     * reservado pagado, en ruta, finalizado y anulado), todos con su fecha y hora exacta de registro.
     * Solo se siembran cuando la tabla de alquileres está vacía.
     */
    // Los datos de demostración actuales se cargan con el script scripts/cargar_datos_demostracion.sh;
    // la semilla antigua solo se usa si se activa a propósito con app.sembrar-demo=true
    @org.springframework.beans.factory.annotation.Value("${app.sembrar-demo:false}")
    private boolean sembrarDemoAntigua;

    private void initClientesYOperaciones() {
        if (!sembrarDemoAntigua || alquilerRepository.count() > 0) {
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

        LocalDate hoy = LocalDate.now();
        // Hora actual sin segundos: los registros de "hoy" se calculan hacia atrás para que nunca queden en el futuro
        LocalDateTime ahora = LocalDateTime.now().withSecond(0).withNano(0);

        EstadoAlquiler stPendiente = crearEstadoAlquiler("PENDIENTE");
        EstadoAlquiler stReservado = crearEstadoAlquiler("RESERVADO");
        EstadoAlquiler stPagado = crearEstadoAlquiler("PAGADO");
        EstadoAlquiler stActivo = crearEstadoAlquiler("ACTIVO");
        EstadoAlquiler stFinalizado = crearEstadoAlquiler("FINALIZADO");
        EstadoAlquiler stCancelado = crearEstadoAlquiler("CANCELADO");
        EstadoAlquiler stRechazado = crearEstadoAlquiler("RECHAZADO");

        MetodoPago mpTarjeta = crearMetodoPago("TARJETA");
        MetodoPago mpYape = crearMetodoPago("YAPE");
        MetodoPago mpPlin = crearMetodoPago("PLIN");
        MetodoPago mpEfectivo = crearMetodoPago("EFECTIVO");

        // ───────── PENDIENTE (2): solicitudes por atender ─────────
        LocalDateTime reg = ahora.minusHours(2).minusMinutes(13);
        Alquiler p1 = registrarContratoSeed(reg, c2, buscarModelo("Corolla"), null,
                hoy.plusDays(1), hoy.plusDays(4), null, "09:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                true, false, false, stPendiente, null, null, null, null);
        historial(p1, new String[] { "PENDIENTE" }, new LocalDateTime[] { reg });

        reg = ahora.minusHours(5).minusMinutes(41);
        Alquiler p2 = registrarContratoSeed(reg, c4, buscarModelo("Swift"), null,
                hoy.plusDays(2), hoy.plusDays(5), null, "10:00", "18:00", "AEROPUERTO", "AEROPUERTO",
                false, false, true, stPendiente, null, null, null, null);
        historial(p2, new String[] { "PENDIENTE" }, new LocalDateTime[] { reg });

        // ───────── RESERVADO · POR PAGAR (2) ─────────
        reg = ahora.minusDays(1).minusHours(3).minusMinutes(7);
        Alquiler r1 = registrarContratoSeed(reg, c3, buscarModelo("Kicks"), buscarUnidad("NIS-901"),
                hoy.plusDays(1), hoy.plusDays(3), null, "11:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                false, true, false, stReservado, null, null, null, null);
        historial(r1, new String[] { "PENDIENTE", "RESERVADO" }, new LocalDateTime[] { reg, reg.plusMinutes(18) });

        reg = ahora.minusDays(1).minusHours(6).minusMinutes(52);
        Alquiler r2 = registrarContratoSeed(reg, c5, buscarModelo("Rio"), buscarUnidad("JKL-011"),
                hoy.plusDays(2), hoy.plusDays(4), null, "10:00", "18:00", "AEROPUERTO", "MIRAFLORES",
                false, false, true, stReservado, null, null, null, null);
        historial(r2, new String[] { "PENDIENTE", "RESERVADO" }, new LocalDateTime[] { reg, reg.plusMinutes(25) });

        // ───────── RESERVADO · PAGADO (2) ─────────
        reg = ahora.minusDays(2).minusHours(4).minusMinutes(36);
        Alquiler g1 = registrarContratoSeed(reg, c1, buscarModelo("3"), buscarUnidad("MZD-401"),
                hoy.plusDays(1), hoy.plusDays(4), null, "10:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                true, false, false, stPagado, null, null, null, null);
        historial(g1, new String[] { "PENDIENTE", "RESERVADO", "PAGADO" },
                new LocalDateTime[] { reg, reg.plusMinutes(12), reg.plusMinutes(31) });
        registrarPagoSeed(g1, mpTarjeta, reg.plusMinutes(31), "DIGITAL", "4532",
                "VISA • Titular: CARLOS MENDOZA", "TRX-CARD-90301144", "CULQI_GATEWAY_PCI_DSS");

        reg = ahora.minusDays(2).minusHours(1).minusMinutes(9);
        Alquiler g2 = registrarContratoSeed(reg, c4, buscarModelo("Hilux"), buscarUnidad("HLX-301"),
                hoy.plusDays(3), hoy.plusDays(6), null, "09:00", "18:00", "AEROPUERTO", "AEROPUERTO",
                true, true, false, stPagado, null, null, null, null);
        historial(g2, new String[] { "PENDIENTE", "RESERVADO", "PAGADO" },
                new LocalDateTime[] { reg, reg.plusMinutes(10), reg.plusMinutes(22) });
        registrarPagoSeed(g2, mpYape, reg.plusMinutes(22), "DIGITAL", null,
                "YAPE (976543219)", "TRX-YAPE-90302255", "YAPE_GATEWAY_CULQI");

        // ───────── RESERVADO · PAGADO con recojo HOY (2): ya se puede entregar llaves ─────────
        reg = ahora.minusDays(1).minusHours(2).minusMinutes(21);
        Alquiler g3 = registrarContratoSeed(reg, c3, buscarModelo("Tucson"), buscarUnidad("HYU-801"),
                hoy, hoy.plusDays(3), null, "10:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                true, false, false, stPagado, null, null, null, null);
        historial(g3, new String[] { "PENDIENTE", "RESERVADO", "PAGADO" },
                new LocalDateTime[] { reg, reg.plusMinutes(9), reg.plusMinutes(27) });
        registrarPagoSeed(g3, mpEfectivo, reg.plusMinutes(27), "COUNTER", null,
                null, "POS-EFE-003041", "COUNTER_PRESENCIAL");

        reg = ahora.minusDays(1).minusHours(7).minusMinutes(48);
        Alquiler g4 = registrarContratoSeed(reg, c1, buscarModelo("Versa"), buscarUnidad("NSS-201"),
                hoy, hoy.plusDays(2), null, "11:00", "18:00", "AEROPUERTO", "AEROPUERTO",
                false, true, false, stPagado, null, null, null, null);
        historial(g4, new String[] { "PENDIENTE", "RESERVADO", "PAGADO" },
                new LocalDateTime[] { reg, reg.plusMinutes(14), reg.plusMinutes(35) });
        registrarPagoSeed(g4, mpYape, reg.plusMinutes(35), "DIGITAL", null,
                "YAPE (987451230)", "TRX-YAPE-90303388", "YAPE_GATEWAY_CULQI");

        // ───────── EN RUTA (2): llaves entregadas ─────────
        reg = ahora.minusDays(4).minusHours(2).minusMinutes(18);
        Alquiler a1 = registrarContratoSeed(reg, c1, buscarModelo("Ranger"), buscarUnidad("RNG-601"),
                hoy.minusDays(1), hoy.plusDays(2), null, "09:00", "18:00", "AEROPUERTO", "AEROPUERTO",
                true, false, false, stActivo, BigDecimal.valueOf(7000.00), null, null, null);
        LocalDateTime entrega1 = hoy.minusDays(1).atTime(9, 12);
        marcarEntrega(a1, entrega1, 8400, "LLENO");
        historial(a1, new String[] { "PENDIENTE", "RESERVADO", "PAGADO", "ACTIVO" },
                new LocalDateTime[] { reg, reg.plusMinutes(15), reg.plusMinutes(34), entrega1 });
        registrarPagoSeed(a1, mpEfectivo, reg.plusMinutes(34), "COUNTER", null,
                null, "POS-EFE-002033", "COUNTER_PRESENCIAL");

        reg = ahora.minusDays(5).minusHours(3).minusMinutes(44);
        Alquiler a2 = registrarContratoSeed(reg, c3, buscarModelo("ASX"), buscarUnidad("ASX-101"),
                hoy.minusDays(2), hoy.plusDays(1), null, "10:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                true, false, true, stActivo, BigDecimal.valueOf(5000.00), null, null, null);
        // El cliente llegó 35 minutos después de la hora pactada
        LocalDateTime entrega2 = hoy.minusDays(2).atTime(10, 35);
        marcarEntrega(a2, entrega2, 11200, "3/4");
        historial(a2, new String[] { "PENDIENTE", "RESERVADO", "PAGADO", "ACTIVO" },
                new LocalDateTime[] { reg, reg.plusMinutes(20), reg.plusMinutes(41), entrega2 });
        registrarPagoSeed(a2, mpPlin, reg.plusMinutes(41), "DIGITAL", null,
                "PLIN (INTERBANK - 984567123)", "TRX-PLIN-90202299", "PLIN_GATEWAY_INTEROPERABLE");

        // ───────── FINALIZADO (2): ya devueltos ─────────
        reg = ahora.minusDays(11).minusHours(1).minusMinutes(26);
        Alquiler f1 = registrarContratoSeed(reg, c2, buscarModelo("Swift"), buscarUnidad("SZK-701"),
                hoy.minusDays(9), hoy.minusDays(6), hoy.minusDays(6), "09:00", "17:00", "AEROPUERTO", "AEROPUERTO",
                false, false, true, stFinalizado, BigDecimal.valueOf(2500.00), BigDecimal.valueOf(2500.00),
                hoy.minusDays(6).atTime(17, 20),
                "Devolución con 20 minutos de retraso. Sin incidencias en carrocería ni interior.");
        LocalDateTime entregaF1 = hoy.minusDays(9).atTime(9, 8);
        marcarEntrega(f1, entregaF1, 6120, "LLENO");
        historial(f1, new String[] { "PENDIENTE", "RESERVADO", "PAGADO", "ACTIVO", "FINALIZADO" },
                new LocalDateTime[] { reg, reg.plusMinutes(14), reg.plusMinutes(29), entregaF1, hoy.minusDays(6).atTime(17, 20) });
        registrarPagoSeed(f1, mpTarjeta, reg.plusMinutes(29), "DIGITAL", "4532",
                "VISA • Titular: VALERIA TORRES", "TRX-CARD-90101122", "CULQI_GATEWAY_PCI_DSS");

        reg = ahora.minusDays(8).minusHours(4).minusMinutes(3);
        Alquiler f2 = registrarContratoSeed(reg, c5, buscarModelo("Rio"), buscarUnidad("JKL-012"),
                hoy.minusDays(7), hoy.minusDays(4), hoy.minusDays(4), "09:00", "18:00", "MIRAFLORES", "AEROPUERTO",
                false, false, false, stFinalizado, BigDecimal.valueOf(2500.00), BigDecimal.valueOf(2500.00),
                hoy.minusDays(4).atTime(17, 40),
                "Vehículo recibido antes del horario pactado y en perfecto estado mecánico y estético.");
        LocalDateTime entregaF2 = hoy.minusDays(7).atTime(8, 58);
        marcarEntrega(f2, entregaF2, 15230, "LLENO");
        historial(f2, new String[] { "PENDIENTE", "RESERVADO", "PAGADO", "ACTIVO", "FINALIZADO" },
                new LocalDateTime[] { reg, reg.plusMinutes(16), reg.plusMinutes(38), entregaF2, hoy.minusDays(4).atTime(17, 40) });
        registrarPagoSeed(f2, mpYape, reg.plusMinutes(38), "DIGITAL", null,
                "YAPE (965432187)", "TRX-YAPE-90106677", "YAPE_GATEWAY_CULQI");

        // ───────── ANULADO (2): una cancelada y una rechazada ─────────
        reg = ahora.minusDays(3).minusHours(2).minusMinutes(30);
        Alquiler x1 = registrarContratoSeed(reg, c4, buscarModelo("Corolla"), null,
                hoy.plusDays(2), hoy.plusDays(5), null, "09:00", "18:00", "MIRAFLORES", "MIRAFLORES",
                false, false, false, stCancelado, null, null, null, null);
        historial(x1, new String[] { "PENDIENTE", "CANCELADO" }, new LocalDateTime[] { reg, reg.plusHours(5) });

        reg = ahora.minusDays(2).minusHours(6).minusMinutes(12);
        Alquiler x2 = registrarContratoSeed(reg, c2, buscarModelo("Versa"), null,
                hoy.plusDays(1), hoy.plusDays(3), null, "12:00", "18:00", "MIRAFLORES", "AEROPUERTO",
                false, false, false, stRechazado, null, null, null, null);
        x2.setMotivoRechazo("DNI_NO_COINCIDE");
        historial(x2, new String[] { "PENDIENTE", "RECHAZADO" }, new LocalDateTime[] { reg, reg.plusMinutes(47) });
    }

    // Reemplaza el historial automático por la línea de tiempo real del contrato de ejemplo
    private void historial(Alquiler alq, String[] estados, LocalDateTime[] momentos) {
        alq.getHistorialEstados().clear();
        String anterior = null;
        for (int i = 0; i < estados.length; i++) {
            alq.getHistorialEstados().add(new HistorialEstadoAlquiler(alq, anterior, estados[i], momentos[i]));
            anterior = estados[i];
        }
        alq.setFechaUltimoEstado(momentos[momentos.length - 1]);
        alquilerRepository.save(alq);
    }

    // Acta de entrega de llaves del contrato de ejemplo
    private void marcarEntrega(Alquiler alq, LocalDateTime momento, int km, String combustible) {
        alq.setFechaEntrega(momento);
        alq.setKmEntrega(km);
        alq.setCombustibleEntrega(combustible);
        alq.setChecklistEntrega(String.join("|", CHECKLIST_SEED));
        alq.setObservacionesEntrega("Sin observaciones.");
        alq.setEntregadoPor(ASESOR_SEED);
        alquilerRepository.save(alq);
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

    private Alquiler registrarContratoSeed(LocalDateTime registro, Cliente cliente, VehiculoModelo modelo, VehiculoUnidad unidad,
                                           LocalDate fechaInicio, LocalDate fechaFinEstimada,
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
        alq.setFechaReserva(registro.toLocalDate());
        alq.setFechaRegistro(registro);
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
        // El historial real lo arma historial(): se descarta el registro automático de setEstado
        alq.getHistorialEstados().clear();
        alq.setGarantiaRetenida(garantiaRetenida);
        alq.setGarantiaLiberada(garantiaLiberada);
        alq.setFechaCheckout(fechaCheckout);
        alq.setActaObservaciones(actaObservaciones);
        return alquilerRepository.save(alq);
    }

    private void registrarPagoSeed(Alquiler alquiler, MetodoPago metodo, LocalDateTime cuando,
                                   String canal, String ultimosDigitos, String bancoOrigen,
                                   String numeroOperacion, String validadoPor) {
        BigDecimal monto = BigDecimal.valueOf(alquiler.getPrecioTotal());
        Pago pago = new Pago();
        pago.setAlquiler(alquiler);
        pago.setMetodoPago(metodo);
        pago.setMontoTotal(TarifarioPaulCars.redondear(monto));
        pago.setFechaPago(cuando.toLocalDate());
        pago.setFechaHoraPago(cuando);
        pago.setEstadoPago("Pagado");
        pago.setTipoPago("ALQUILER");
        pago.setCanal(canal);
        pago.setRegistradoPor("COUNTER".equalsIgnoreCase(canal) ? ASESOR_SEED : "Pago en línea");
        if ("EFECTIVO".equalsIgnoreCase(metodo.getNombreMetodo())) {
            pago.setMontoRecibido(TarifarioPaulCars.redondear(monto));
            pago.setVuelto(BigDecimal.ZERO);
        }
        pago.setUltimosDigitos(ultimosDigitos);
        pago.setBancoOrigen(bancoOrigen);
        pago.setNumeroOperacion(numeroOperacion);
        pago.setValidadoPor(validadoPor);
        pago.setFechaValidacion(cuando);
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

    /** Cuenta del personal (administrador o asesor): nombres, apellidos y DNI en campos propios. */
    private void crearUsuarioSiNoExiste(String username, String rawPassword, String email,
                                        String nombres, String apellidos, String dni, Rol rol) {
        if (usuarioRepository.findByUsernameIgnoreCase(username).isEmpty()) {
            Usuario u = new Usuario();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(rawPassword));
            u.setEmailRecuperacion(email);
            u.setNombres(nombres);
            u.setApellidos(apellidos);
            u.setDni(dni);
            u.setNombreCompleto(nombres + " " + apellidos);
            u.setRol(rol);
            u.setEstado(true);
            u.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(u);
            log.info("Usuario seed creado: [{}] con rol [{}]", username, rol.getNombreRol());
        }
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
