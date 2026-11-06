# Paul Car's — Backend (API REST)

API REST para el sistema de alquiler de vehículos **Paul Car's**, construida con Spring Boot. Expone endpoints para autenticación (JWT), gestión de vehículos, clientes, alquileres, pagos, usuarios y catálogo base (marcas, modelos, tipos de vehículo, combustibles).

## Tecnologías

- Java 20
- Spring Boot 4.0.6 (Spring Web MVC, Spring Data JPA, Spring Security)
- MySQL 8
- JWT (io.jsonwebtoken / jwt)
- Lombok
- Maven

## Requisitos previos

- JDK 20 o superior
- Maven (o usa el wrapper incluido, `./mvnw`, que no requiere instalación)
- MySQL Server 8 corriendo localmente (o accesible por red)
- Un cliente de MySQL (MySQL Workbench, DBeaver, o la consola `mysql`)

## 1. Crear e importar la base de datos

Puedes crear la base de datos e importar todas las tablas relacionales y datos semilla ejecutando el script incluido en el repositorio:

```bash
mysql -u root -p < database/database_alquiler_de_vehiculos.sql
```

O si prefieres crearla vacía para que Hibernate genere las tablas al iniciar:

```sql
CREATE DATABASE alquiler_de_vehiculos CHARACTER SET utf8mb4;
```

## 2. Crear un usuario de base de datos dedicado

Por seguridad, la aplicación **no se conecta con el usuario `root`**. Crea un usuario dedicado con permisos solo sobre esta base de datos:

```sql
CREATE USER IF NOT EXISTS 'paulcars_app'@'localhost' IDENTIFIED BY 'PaulCars_Dev2026!';
GRANT ALL PRIVILEGES ON alquiler_de_vehiculos.* TO 'paulcars_app'@'localhost';
FLUSH PRIVILEGES;
```

> Si prefieres otra contraseña, cámbiala aquí y en la variable de entorno `DB_PASSWORD` (ver siguiente sección).

## 3. Configurar variables de entorno (opcional en desarrollo)

El proyecto usa **perfiles de Spring** (`application-dev.properties` para desarrollo, `application-prod.properties` para producción). En desarrollo, si no defines nada, se usan estos valores por defecto:

| Variable | Valor por defecto (dev) | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/alquiler_de_vehiculos?useSSL=false&serverTimezone=UTC` | URL de conexión a MySQL |
| `DB_USERNAME` | `paulcars_app` | Usuario de base de datos |
| `DB_PASSWORD` | `PaulCars_Dev2026!` | Contraseña del usuario |
| `JWT_SECRET` | (clave de desarrollo incluida) | Clave para firmar los tokens JWT |
| `JWT_EXPIRATION_MS` | `3600000` (1 hora) | Tiempo de expiración del token |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil activo |

Si quieres sobreescribir alguno, defínelo como variable de entorno antes de levantar la app, por ejemplo:

```bash
export DB_PASSWORD="otra-clave"
```

En **producción**, `application-prod.properties` no trae valores por defecto — hay que definir `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` explícitamente o la app no arranca (a propósito, para evitar usar credenciales de desarrollo por accidente).

## 4. Levantar el backend

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080`. Al arrancar por primera vez, Hibernate crea automáticamente todas las tablas, y `CatalogoInitializer` siembra datos base (roles ADMIN/EMPLEADO/CLIENTE, estados de vehículo/alquiler, marca Toyota, modelo Corolla, tipo SEDAN, combustible GASOLINA, método de pago TARJETA).

## 5. Usuarios de prueba

El primer usuario que se registre vía `POST /api/auth/registro` se convierte automáticamente en `ADMIN`. Los siguientes se registran como `CLIENTE`. Para crear usuarios `EMPLEADO` adicionales, usa el panel de administración del frontend (`/admin/usuarios`) una vez que tengas un `ADMIN` creado.

## 6. Correr las pruebas

```bash
./mvnw test
```

Las pruebas usan una base de datos H2 en memoria (ver `src/test/resources/application.properties`), así que nunca tocan tu base de datos MySQL real. Incluyen el CRUD completo de `Cliente` (crear, listar, actualizar, eliminar) y pruebas de seguridad (un `EMPLEADO` no puede eliminar, un usuario sin autenticar no puede leer datos protegidos).

## Estructura del proyecto

```
src/main/java/com/example/alquiler_de_vehiculos/
├── config/         # Seguridad (SecurityConfig), seed de catálogo (CatalogoInitializer)
├── controller/     # Endpoints REST
├── dto/            # Objetos de transferencia (requests)
├── model/          # Entidades JPA
├── repository/     # Repositorios Spring Data JPA
├── security/       # JWT (JwtService, JwtAuthenticationFilter, CustomUserDetailService)
└── service/        # Lógica de negocio
```

## Endpoints principales

| Recurso | Base | Notas |
|---|---|---|
| Autenticación | `/api/auth` | `login`, `registro` públicos |
| Vehículos | `/api/vehiculos` | Lectura: ADMIN/EMPLEADO. Eliminar: solo ADMIN |
| Clientes | `/api/clientes` | Lectura: ADMIN/EMPLEADO. Eliminar: solo ADMIN |
| Alquileres | `/api/alquileres` | Crear, finalizar, cancelar |
| Usuarios | `/api/usuarios` | Solo ADMIN (crear empleados/admins, activar/desactivar) |
| Pagos | `/api/pagos` | Listado completo: solo ADMIN |
| Catálogos | `/api/catalogos` | Marcas, modelos, tipos, combustibles. Crear/editar: solo ADMIN |