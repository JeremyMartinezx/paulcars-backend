# Paul Car's — Backend (API REST)

API REST del sistema de alquiler de vehículos **Paul Car's** (Lima, Perú), construida con Spring Boot. Cubre autenticación con JWT, catálogo y flota por placa, reservas en línea y en mostrador, pagos, entrega y recepción de llaves con acta, daños, cancelaciones con reembolso, comprobantes en PDF, reportes y notificaciones.

## Tecnologías

- Java 21 · Spring Boot 4 (Web MVC, Data JPA, Security, Validation, Mail, Cache con Caffeine)
- MySQL 8 (en la nube: compatible con TiDB Cloud)
- JWT (access + refresh) y límite de intentos en el login
- OpenAPI / Swagger (`/swagger-ui.html`)
- Generación de PDF y Excel para comprobantes y reportes
- Maven (wrapper incluido: `./mvnw`)

## Módulos

| Módulo | Qué hace |
|---|---|
| Autenticación y usuarios | Login, renovación de token, cambio de contraseña, roles `ADMIN`, `EMPLEADO` y `CLIENTE` |
| Clientes | Alta con DNI, licencia y teléfono validados; historial de alquileres |
| Flota y catálogo | Modelos con fotos y color propio, unidades por placa y sede, mantenimiento preventivo por kilometraje, catálogos base (marcas, modelos, categorías, combustibles) |
| Disponibilidad | La placa se asigna solo desde la **sede de recojo**; sin stock en esa sede se rechaza la reserva. `GET /api/vehiculos/disponibilidad-sedes` informa el stock por sede y fechas |
| Alquileres | Solicitudes en línea, alquiler en mostrador, aprobación o rechazo con motivo, estados con historial |
| Pagos | Efectivo, tarjeta, Yape, Plin y transferencia; devoluciones y cierre de caja |
| Entrega y recepción | Entrega de llaves desde 30 min antes hasta 60 min después de la hora pactada; acta con kilometraje, combustible y checklist; la placa debe estar en la sede y lista |
| Cancelaciones | Reembolso 100 % / 50 % / 0 % según la anticipación; «vehículo no disponible» devuelve el 100 %; cancelación automática de reservas vencidas y de quienes no se presentan |
| Daños | Registro en entrega y recepción, evaluación y retención de la garantía |
| Comprobantes | Boleta electrónica (modo demo o Nubefact), acta de entrega y de finalización en PDF |
| Reportes | Resumen de hoy y del mes, reporte de contratos en PDF y Excel, solicitudes por motivo |
| Tiempo real | Endpoint de cambios para refrescar las pantallas sin recargar |

## Requisitos

- JDK 21 o superior
- MySQL 8 local o remoto

## Puesta en marcha local

1. Crea la base de datos:

   ```sql
   CREATE DATABASE paulcars_db CHARACTER SET utf8mb4;
   ```

2. Levanta el backend (perfil `dev` por defecto; Hibernate crea las tablas):

   ```bash
   ./mvnw spring-boot:run
   ```

3. La API queda en `http://localhost:8080`. Al arrancar, `DataInitializer` siembra roles, catálogos, el administrador, un asesor y la flota base (11 modelos con sus placas). Los modelos que registre el administrador se conservan entre reinicios.

El script `database/database_alquiler_de_vehiculos.sql` es una referencia del esquema.

## Perfiles y variables de entorno

| Variable | Uso | Valor por defecto (dev) |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` o `prod` | `dev` |
| `DB_URL` / `DATABASE_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/paulcars_db...` |
| `DB_USERNAME` / `DATABASE_USERNAME` | Usuario de la base | `root` |
| `DB_PASSWORD` / `DATABASE_PASSWORD` | Contraseña de la base | `root` |
| `DDL_AUTO` | `update` en dev, `validate` en prod | — |
| `JWT_SECRET` | Clave de firma de los tokens | clave de desarrollo |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | Cuenta SMTP para los correos | — |
| `NUBEFACT_URL` / `NUBEFACT_TOKEN` | Facturación electrónica real (sin ellas, boletas en modo demo) | vacío |
| `CANCELAR_VENCIDAS_AUTO`, `CANCELAR_NO_PRESENTADAS_AUTO` | Cancelaciones automáticas | `true` |
| `UPLOADS_DIR` | Carpeta de imágenes subidas | `uploads` |
| `PORT` | Puerto del servidor | `8080` |

En producción (`prod`) la conexión sale de variables de entorno y el esquema se **valida**, no se modifica.

## Despliegue

El repositorio incluye `Dockerfile`. En Render se crea un *Web Service* con el repositorio, `SPRING_PROFILES_ACTIVE=prod` y las variables de base de datos, JWT y correo. El frontend (Vercel) consume la API desde `https://<servicio>.onrender.com/api`.

## Estructura

```
src/main/java/com/example/alquiler_de_vehiculos/
├── config/       # Seguridad, CORS, OpenAPI, propiedades y DataInitializer
├── controller/   # Endpoints REST
├── dto/          # Solicitudes y respuestas
├── exception/    # Excepciones de negocio y manejador global
├── model/        # Entidades JPA
├── repository/   # Spring Data JPA
├── security/     # JWT, filtros y límite de intentos
├── service/      # Lógica de negocio, PDF, Excel, correos y tareas programadas
└── util/         # Tarifario y utilidades
```

## Endpoints principales

| Recurso | Base | Acceso |
|---|---|---|
| Autenticación | `/api/auth` | `login` y `refresh` públicos |
| Catálogo público | `/api/vehiculos`, `/api/empresa` | Lectura pública |
| Clientes | `/api/clientes` | ADMIN y EMPLEADO |
| Alquileres | `/api/alquileres` | Cliente (sus reservas), EMPLEADO y ADMIN |
| Pagos | `/api/pagos` | Cliente (su reserva), EMPLEADO y ADMIN |
| Reportes | `/api/reportes` | ADMIN |
| Usuarios y catálogos base | `/api/usuarios`, `/api/catalogos` | ADMIN |

## Pruebas

```bash
./mvnw test
```

## Control de versiones

El proyecto sigue un flujo de trabajo por ramas:

| Rama | Uso |
|---|---|
| `main` | Versión estable. Solo recibe *merges* desde `develop` y lleva las etiquetas de versión (`v1.0.0`, `v1.1.0`, ...). |
| `develop` | Integración: aquí se unen las ramas de trabajo ya terminadas. |
| `feature/*` | Una rama por tema (por ejemplo, `feature/pagos`), creada desde `develop` y unida con `merge --no-ff` para conservar el historial. |
| `fix/*`, `refactor/*`, `docs/*` | Correcciones, reorganizaciones y documentación. |

**Mensajes de commit:** `tipo(alcance): descripción`, con los tipos `feat`, `fix`, `refactor`, `style`, `docs`, `build`, `chore` y `test`.

**Versiones:** [versionado semántico](https://semver.org/lang/es/) (`MAJOR.MINOR.PATCH`). Los cambios de cada versión están en [CHANGELOG.md](CHANGELOG.md).
