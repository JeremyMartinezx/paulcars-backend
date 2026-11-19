# Historial de versiones — Backend

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versionado semántico.

## [1.1.1] — 2026-11-18

### Corregido
- `DataInitializer`: se eliminó una importación sin uso (`java.util.Set`) que quedó tras conservar los modelos registrados por el administrador.

### Documentación
- README actualizado, historial de versiones y flujo de trabajo con Git.

## [1.1.0] — 2026-11-18

### Agregado
- **Disponibilidad por sede:** la placa se asigna solo desde la sede de recojo y existe `GET /api/vehiculos/disponibilidad-sedes`.
- **Entrega y recepción de llaves** con acta (kilometraje, combustible y checklist) y validación de que la placa esté en la sede y lista.
- **Cancelaciones y reembolsos** 100 % / 50 % / 0 %, motivo «vehículo no disponible» y cancelación automática de reservas vencidas o de clientes que no se presentan.
- **Daños del vehículo** en entrega y recepción, con retención de la garantía.
- **Comprobantes:** boleta electrónica (modo demo o Nubefact), acta de entrega y de finalización en PDF.
- **Reportes** de resumen, contratos en PDF y Excel, y cierre de caja.
- **Pagos** con tarjeta, efectivo, Yape, Plin y transferencia.
- **Actualización en vivo:** endpoint de cambios para refrescar las pantallas.
- Historial de estados de cada alquiler y propiedades configurables de empresa, SUNAT y política de cancelación.

### Cambiado
- El color elegido al registrar un modelo se guarda en la base de datos.
- Perfiles `dev` y `prod` con variables de entorno para la base de datos, JWT y correo.

### Corregido
- Los modelos registrados por el administrador ya no se borran al reiniciar el servidor.

## [1.0.0] — 2026-11-06

### Agregado
- Versión inicial: autenticación JWT, gestión de clientes, vehículos, alquileres, pagos, usuarios y catálogo base.
