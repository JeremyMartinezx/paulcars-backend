SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT;
SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS;
SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION;
SET NAMES utf8mb4;
SET @OLD_TIME_ZONE=@@TIME_ZONE;
SET TIME_ZONE='+00:00';
SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO';
SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0;
CREATE DATABASE IF NOT EXISTS `paulcars_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT ENCRYPTION='N';
USE `paulcars_db`;
DROP TABLE IF EXISTS `alquileres`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `alquileres` (
  `dias` int DEFAULT NULL,
  `fecha_fin_estimada` date NOT NULL,
  `fecha_fin_real` date DEFAULT NULL,
  `fecha_inicio` date NOT NULL,
  `fecha_reserva` date NOT NULL,
  `garantia_liberada` decimal(10,2) DEFAULT NULL,
  `garantia_retenida` decimal(10,2) DEFAULT NULL,
  `id_alquiler` int NOT NULL AUTO_INCREMENT,
  `id_cliente` int NOT NULL,
  `id_estado_alquiler` int NOT NULL,
  `id_modelo` int NOT NULL,
  `id_unidad` int DEFAULT NULL,
  `monto_extras` double DEFAULT NULL,
  `precio_base` double DEFAULT NULL,
  `precio_total` double DEFAULT NULL,
  `fecha_checkout` datetime(6) DEFAULT NULL,
  `hora_devolucion` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hora_recojo` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `motivo_rechazo` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sede_devolucion` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sede_recojo` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `observacion_rechazo` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `servicios_extras` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `acta_observaciones` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id_alquiler`),
  KEY `FKc32vjwe393watfcig6e5udx5x` (`id_cliente`),
  KEY `FKsxe3g1397t4gb032j8qyvcscd` (`id_estado_alquiler`),
  KEY `FK11wg33wb29x0rphem24eqw276` (`id_modelo`),
  KEY `FKop6xmgh4220sjr27avj67kndv` (`id_unidad`),
  CONSTRAINT `FK11wg33wb29x0rphem24eqw276` FOREIGN KEY (`id_modelo`) REFERENCES `vehiculos_modelos` (`id_modelo`),
  CONSTRAINT `FKc32vjwe393watfcig6e5udx5x` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`),
  CONSTRAINT `FKop6xmgh4220sjr27avj67kndv` FOREIGN KEY (`id_unidad`) REFERENCES `vehiculos_unidades` (`id_unidad`),
  CONSTRAINT `FKsxe3g1397t4gb032j8qyvcscd` FOREIGN KEY (`id_estado_alquiler`) REFERENCES `estadosalquiler` (`id_estado_alquiler`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `alquileres` WRITE;
ALTER TABLE `alquileres` DISABLE KEYS;
INSERT INTO `alquileres` VALUES (3,'2026-10-16','2026-10-16','2026-10-13','2026-10-12',3500.00,3500.00,1,1,6,1,1,105,360,465,'2026-10-16 22:45:00.000000','18:00','10:00',NULL,'MIRAFLORES','MIRAFLORES',NULL,'Seguro Todo Riesgo','Recepción conforme en counter Miraflores. Kilometraje y nivel de combustible verificados sin observaciones.'),(3,'2026-10-20','2026-10-20','2026-10-17','2026-10-16',2500.00,2500.00,2,2,6,11,58,19.5,264,283.5,'2026-10-20 21:50:00.000000','17:00','09:00',NULL,'AEROPUERTO','AEROPUERTO',NULL,'Silla de Beb├®','Devolución puntual en counter Aeropuerto Jorge Chávez. Sin incidencias en carrocería ni interior.'),(4,'2026-10-25','2026-10-25','2026-10-21','2026-10-20',5000.00,5000.00,3,3,6,4,17,166,720,886,'2026-10-25 22:30:00.000000','18:00','11:00',NULL,'MIRAFLORES','AEROPUERTO',NULL,'Seguro Todo Riesgo, Conductor Adicional','Unidad devuelta en óptimas condiciones operativas. Garantía HOLD liberada al 100%.'),(4,'2026-10-26','2026-10-26','2026-10-22','2026-10-21',7000.00,7000.00,4,3,6,7,35,140,920,1060,'2026-10-26 23:00:00.000000','18:00','08:30',NULL,'AEROPUERTO','AEROPUERTO',NULL,'Seguro Todo Riesgo','Inspección de retorno aprobada. Unidad trasladada a lavado estándar de patio.'),(2,'2026-10-27','2026-10-27','2026-10-25','2026-10-24',3500.00,3500.00,5,4,6,2,7,13,220,233,'2026-10-27 22:15:00.000000','18:00','10:00',NULL,'MIRAFLORES','MIRAFLORES',NULL,'Conductor Adicional','Check-out completado sin cargos adicionales. Entrega de acta de conformidad al titular.'),(3,'2026-10-29','2026-10-29','2026-10-26','2026-10-25',2500.00,2500.00,6,5,6,10,53,0,270,270,'2026-10-29 22:40:00.000000','18:00','09:00',NULL,'AEROPUERTO','MIRAFLORES',NULL,'Ninguno','Veh├¡culo recibido dentro del horario pactado y en perfecto estado mec├ínico y est├®tico.'),(2,'2026-10-31',NULL,'2026-10-29','2026-10-28',NULL,7000.00,7,1,5,9,46,70,470,540,NULL,'18:00','09:00',NULL,'AEROPUERTO','AEROPUERTO',NULL,'Seguro Todo Riesgo',NULL),(3,'2026-11-02',NULL,'2026-10-30','2026-10-29',NULL,5000.00,8,3,5,6,29,124.5,465,589.5,NULL,'18:00','10:00',NULL,'MIRAFLORES','MIRAFLORES',NULL,'Seguro Todo Riesgo, Silla de Beb├®',NULL),(3,'2026-11-01',NULL,'2026-10-29','2026-10-28',NULL,7000.00,9,5,5,8,41,19.5,645,664.5,NULL,'17:00','08:30',NULL,'AEROPUERTO','MIRAFLORES',NULL,'Conductor Adicional',NULL),(3,'2026-11-03',NULL,'2026-10-31','2026-10-30',NULL,NULL,10,1,3,3,12,105,375,480,NULL,'18:00','10:00',NULL,'MIRAFLORES','MIRAFLORES',NULL,'Seguro Todo Riesgo',NULL),(3,'2026-11-05',NULL,'2026-11-02','2026-10-31',NULL,NULL,11,5,3,7,34,124.5,690,814.5,NULL,'18:00','09:00',NULL,'AEROPUERTO','AEROPUERTO',NULL,'Seguro Todo Riesgo, Conductor Adicional',NULL),(2,'2026-11-02',NULL,'2026-10-31','2026-10-30',NULL,NULL,12,1,2,5,24,13,320,333,NULL,'18:00','11:00',NULL,'MIRAFLORES','MIRAFLORES',NULL,'Conductor Adicional',NULL),(3,'2026-11-04',NULL,'2026-11-01','2026-10-31',NULL,NULL,13,4,2,10,52,19.5,270,289.5,NULL,'18:00','10:00',NULL,'MIRAFLORES','AEROPUERTO',NULL,'Silla de Beb├®',NULL),(3,'2026-11-04',NULL,'2026-11-01','2026-10-31',NULL,NULL,14,1,1,1,NULL,105,360,465,NULL,'18:00','09:00',NULL,'MIRAFLORES','MIRAFLORES',NULL,'Seguro Todo Riesgo',NULL),(4,'2026-11-06',NULL,'2026-11-02','2026-10-31',NULL,NULL,15,3,1,4,NULL,166,720,886,NULL,'18:00','10:00',NULL,'AEROPUERTO','AEROPUERTO',NULL,'Seguro Todo Riesgo, Conductor Adicional',NULL),(2,'2026-11-03',NULL,'2026-11-01','2026-10-31',NULL,NULL,16,4,1,11,NULL,13,176,189,NULL,'18:00','12:00',NULL,'AEROPUERTO','MIRAFLORES',NULL,'Silla de Beb├®',NULL),(1,'2026-11-02','2026-11-01','2026-11-01','2026-11-01',7000.00,7000.00,17,8,6,9,46,35,235,270,'2026-11-01 05:23:34.094904','10:00','10:00',NULL,'AEROPUERTO','AEROPUERTO',NULL,'Seguro Todo Riesgo','Se recibieron las llaves y el vehículo en su totalidad y en óptimas condiciones.'),(1,'2026-11-02',NULL,'2026-11-01','2026-11-01',NULL,7000.00,18,8,5,9,46,35,235,270,NULL,'10:00','10:00',NULL,'AEROPUERTO','AEROPUERTO',NULL,'Seguro Todo Riesgo',NULL),(2,'2026-11-04',NULL,'2026-11-02','2026-11-01',NULL,2500.00,19,8,3,10,54,70,180,250,NULL,'10:00','10:00',NULL,'MIRAFLORES','MIRAFLORES',NULL,'Seguro Todo Riesgo',NULL),(1,'2026-11-02',NULL,'2026-11-01','2026-11-01',NULL,NULL,20,8,1,10,NULL,35,90,125,NULL,'10:00','18:00',NULL,'AEROPUERTO','AEROPUERTO',NULL,'Seguro Todo Riesgo',NULL),(3,'2026-11-05',NULL,'2026-11-02','2026-11-01',NULL,NULL,21,8,1,9,NULL,124.5,705,829.5,NULL,'10:00','09:00',NULL,'AEROPUERTO','MIRAFLORES',NULL,'Seguro Todo Riesgo, Conductor Adicional',NULL),(3,'2026-11-05',NULL,'2026-11-02','2026-11-01',NULL,NULL,22,9,1,4,NULL,124.5,540,664.5,NULL,'10:00','10:00',NULL,'MIRAFLORES','MIRAFLORES',NULL,'Seguro Todo Riesgo, Conductor Adicional',NULL);
ALTER TABLE `alquileres` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `clientes`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `clientes` (
  `id_cliente` int NOT NULL AUTO_INCREMENT,
  `sancion_manual` bit(1) DEFAULT NULL,
  `fecha_sancion` datetime(6) DEFAULT NULL,
  `telefono` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `licencia_conducir` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_documento` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tipo_documento` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `estado_cliente` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `apellidos` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nombres` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sancion_por` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `direccion` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `motivo_sancion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_cliente`),
  UNIQUE KEY `UK1c96wv36rk2hwui7qhjks3mvg` (`email`),
  UNIQUE KEY `UK2viccgf178bd74vfbq8ctsv8t` (`numero_documento`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `clientes` WRITE;
ALTER TABLE `clientes` DISABLE KEYS;
INSERT INTO `clientes` VALUES (1,_binary '\0',NULL,'987451230','Q72458910','72458910','DNI','ACTIVO','Mendoza Ríos','Carlos Alberto','carlos.mendoza92@gmail.com',NULL,'Av. José Larco 740, Miraflores, Lima',NULL),(2,_binary '\0',NULL,'991234567','Q74819203','74819203','DNI','ACTIVO','Torres Paredes','Valeria Fernanda','valeria.torres.p@gmail.com',NULL,'Calle Los Pinos 320, San Isidro, Lima',NULL),(3,_binary '\0',NULL,'984567123','Q71938475','71938475','DNI','ACTIVO','Álvarez Vargas','Diego Alejandro','diego.alvarez.v@gmail.com',NULL,'Av. Primavera 1450, Santiago de Surco, Lima',NULL),(4,_binary '\0',NULL,'976543219','Q75647382','75647382','DNI','ACTIVO','Salazar Montes','Camila Lucía','camila.salazar.m@gmail.com',NULL,'Av. Salaverry 2410, Jesús María, Lima',NULL),(5,_binary '\0',NULL,'965432187','Q70829164','70829164','DNI','ACTIVO','Castillo Guzmán','Rodrigo Sebastián','rodrigo.castillo.g@gmail.com',NULL,'Malecón Cisneros 890, Miraflores, Lima',NULL),(6,_binary '\0',NULL,'954321876',NULL,'76123984','DNI','ACTIVO','Fernández Castro','Lucía Gabriela','lucia.fernandez.c@gmail.com',NULL,'Av. Benavides 1890, Miraflores, Lima',NULL),(7,_binary '\0',NULL,'943218765',NULL,NULL,NULL,'ACTIVO','Ramírez Delgado','Mateo Nicolás','mateo.ramirez.d@gmail.com',NULL,NULL,NULL),(8,_binary '\0',NULL,'932131231','Q32323232','72132323','DNI','ACTIVO','Martinez','Jeremy','jere@gmail.com',NULL,'Jr rubi 822',NULL),(9,_binary '\0',NULL,'932423432','Q34232323','21212121','DNI','ACTIVO','Perez','Luisa','luisa2@gmail.com',NULL,'Jr rubi 833',NULL);
ALTER TABLE `clientes` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `combustibles`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `combustibles` (
  `id_combustible` int NOT NULL AUTO_INCREMENT,
  `nombre_combustible` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_combustible`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `combustibles` WRITE;
ALTER TABLE `combustibles` DISABLE KEYS;
INSERT INTO `combustibles` VALUES (1,'GASOLINA'),(2,'DIESEL'),(3,'HIBRIDO');
ALTER TABLE `combustibles` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `estadosalquiler`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `estadosalquiler` (
  `id_estado_alquiler` int NOT NULL AUTO_INCREMENT,
  `nombre_estado` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_estado_alquiler`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `estadosalquiler` WRITE;
ALTER TABLE `estadosalquiler` DISABLE KEYS;
INSERT INTO `estadosalquiler` VALUES (1,'PENDIENTE'),(2,'RESERVADO'),(3,'PAGADO'),(4,'CONFIRMADO'),(5,'ACTIVO'),(6,'FINALIZADO'),(7,'CANCELADO'),(8,'RECHAZADO'),(9,'RECHAZADO_DOCUMENTACION'),(10,'CANCELADO_SIN_STOCK');
ALTER TABLE `estadosalquiler` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `estadosvehiculo`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `estadosvehiculo` (
  `id_estado` int NOT NULL AUTO_INCREMENT,
  `nombre_estado` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_estado`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `estadosvehiculo` WRITE;
ALTER TABLE `estadosvehiculo` DISABLE KEYS;
INSERT INTO `estadosvehiculo` VALUES (1,'DISPONIBLE'),(2,'EN_RUTA'),(3,'ALQUILADO'),(4,'MANTENIMIENTO'),(5,'LIMPIEZA'),(6,'NO DISPONIBLE');
ALTER TABLE `estadosvehiculo` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `imagenes_vehiculo`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `imagenes_vehiculo` (
  `id_imagen` int NOT NULL AUTO_INCREMENT,
  `id_modelo` int NOT NULL,
  `orden` int NOT NULL,
  `url_imagen` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_imagen`),
  KEY `FKbxg5vy75ot5gpf0m4bxtefmgx` (`id_modelo`),
  CONSTRAINT `FKbxg5vy75ot5gpf0m4bxtefmgx` FOREIGN KEY (`id_modelo`) REFERENCES `vehiculos_modelos` (`id_modelo`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `imagenes_vehiculo` WRITE;
ALTER TABLE `imagenes_vehiculo` DISABLE KEYS;
INSERT INTO `imagenes_vehiculo` VALUES (1,1,1,'/images/vehiculos/corolla.jpg'),(2,1,2,'/images/vehiculos/corolla_side.jpg'),(3,1,3,'/images/vehiculos/corolla_rear.jpg'),(4,1,4,'/images/vehiculos/corolla_interior.jpg'),(5,2,1,'/images/vehiculos/versa.jpg'),(6,2,2,'/images/vehiculos/versa_side.jpg'),(7,2,3,'/images/vehiculos/versa_rear.jpg'),(8,2,4,'/images/vehiculos/versa_interior.jpg'),(9,3,1,'/images/vehiculos/mazda3.jpg'),(10,3,2,'/images/vehiculos/mazda3_side.jpg'),(11,3,3,'/images/vehiculos/mazda3_rear.jpg'),(12,3,4,'/images/vehiculos/mazda3_interior.jpg'),(13,4,1,'/images/vehiculos/tucson.jpg'),(14,4,2,'/images/vehiculos/tucson_side.jpg'),(15,4,3,'/images/vehiculos/tucson_rear.jpg'),(16,4,4,'/images/vehiculos/tucson_interior.jpg'),(17,5,1,'/images/vehiculos/kicks.jpg'),(18,5,2,'/images/vehiculos/kicks_side.jpg'),(19,5,3,'/images/vehiculos/kicks_rear.jpg'),(20,5,4,'/images/vehiculos/kicks_interior.jpg'),(21,6,1,'/images/vehiculos/asx.jpg'),(22,6,2,'/images/vehiculos/asx_side.jpg'),(23,6,3,'/images/vehiculos/asx_rear.jpg'),(24,6,4,'/images/vehiculos/asx_interior.jpg'),(33,9,1,'/images/vehiculos/ranger.jpg'),(34,9,2,'/images/vehiculos/ranger_side.jpg'),(35,9,3,'/images/vehiculos/ranger_rear.jpg'),(36,9,4,'/images/vehiculos/ranger_interior.jpg'),(37,10,1,'/images/vehiculos/rio.jpg'),(38,10,2,'/images/vehiculos/rio_side.jpg'),(39,10,3,'/images/vehiculos/rio_rear.jpg'),(40,10,4,'/images/vehiculos/rio_interior.jpg'),(41,11,1,'/images/vehiculos/swift.jpg'),(42,11,2,'/images/vehiculos/swift_side.jpg'),(43,11,3,'/images/vehiculos/swift_rear.jpg'),(44,11,4,'/images/vehiculos/swift_interior.jpg'),(53,7,1,'/images/vehiculos/hilux.jpg'),(54,7,2,'/images/vehiculos/hilux_side.jpg'),(55,7,3,'/images/vehiculos/hilux_rear.jpg'),(56,7,4,'/images/vehiculos/hilux_interior.jpg'),(57,8,1,'/images/vehiculos/l200.jpg'),(58,8,2,'/images/vehiculos/l200_side.jpg'),(59,8,3,'/images/vehiculos/l200_rear.jpg'),(60,8,4,'/images/vehiculos/l200_interior.jpg');
ALTER TABLE `imagenes_vehiculo` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `marcas`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `marcas` (
  `id_marca` int NOT NULL AUTO_INCREMENT,
  `nombre_marca` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_marca`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `marcas` WRITE;
ALTER TABLE `marcas` DISABLE KEYS;
INSERT INTO `marcas` VALUES (1,'Toyota'),(2,'Nissan'),(3,'Mazda'),(4,'Hyundai'),(5,'Mitsubishi'),(6,'Ford'),(7,'Kia'),(8,'Suzuki');
ALTER TABLE `marcas` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `metodospago`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `metodospago` (
  `id_metodo_pago` int NOT NULL AUTO_INCREMENT,
  `nombre_metodo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_metodo_pago`),
  UNIQUE KEY `UKcqau3it90eecuxw3tf4anorkk` (`nombre_metodo`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `metodospago` WRITE;
ALTER TABLE `metodospago` DISABLE KEYS;
INSERT INTO `metodospago` VALUES (2,'EFECTIVO'),(4,'PLIN'),(1,'TARJETA'),(5,'TRANSFERENCIA'),(3,'YAPE');
ALTER TABLE `metodospago` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `modelos`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `modelos` (
  `id_marca` int NOT NULL,
  `id_modelo` int NOT NULL AUTO_INCREMENT,
  `nombre_modelo` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_modelo`),
  KEY `FKrvg5jqisfih1e9qe6478o0vr1` (`id_marca`),
  CONSTRAINT `FKrvg5jqisfih1e9qe6478o0vr1` FOREIGN KEY (`id_marca`) REFERENCES `marcas` (`id_marca`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `modelos` WRITE;
ALTER TABLE `modelos` DISABLE KEYS;
INSERT INTO `modelos` VALUES (1,1,'Corolla'),(2,2,'Versa'),(3,3,'3'),(4,4,'Tucson'),(2,5,'Kicks'),(5,6,'ASX'),(1,7,'Hilux'),(5,8,'L200'),(6,9,'Ranger'),(7,10,'Rio'),(8,11,'Swift');
ALTER TABLE `modelos` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `notificaciones_leidas`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `notificaciones_leidas` (
  `id_notificacion_leida` int NOT NULL AUTO_INCREMENT,
  `fecha_lectura` datetime(6) NOT NULL,
  `clave` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_notificacion_leida`),
  UNIQUE KEY `UKov8cbnt6sj8xud07fxjdso195` (`username`,`clave`)
) ENGINE=InnoDB AUTO_INCREMENT=64 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `notificaciones_leidas` WRITE;
ALTER TABLE `notificaciones_leidas` DISABLE KEYS;
INSERT INTO `notificaciones_leidas` VALUES (1,'2026-10-31 08:11:33.315959','ADM-RET-HOY-7-2026-10-31','admin@paulcars.com'),(2,'2026-10-31 08:12:37.487349','ADM-SOLICITUD-13','admin@paulcars.com'),(3,'2026-10-31 08:12:46.640824','ADM-ENTREGA-12-2026-10-31','admin@paulcars.com'),(4,'2026-10-31 08:12:55.536870','ADM-ENTREGA-10-2026-10-31','admin@paulcars.com'),(5,'2026-10-31 08:16:31.357763','ADM-SOLICITUD-15','admin@paulcars.com'),(6,'2026-10-31 08:16:31.359583','ADM-SOLICITUD-11','admin@paulcars.com'),(7,'2026-10-31 08:16:31.361212','ADM-SOLICITUD-14','admin@paulcars.com'),(8,'2026-10-31 08:16:31.363899','ADM-SOLICITUD-16','admin@paulcars.com'),(9,'2026-10-31 08:16:31.368744','ADM-SOLICITUD-10','admin@paulcars.com'),(10,'2026-10-31 08:16:31.371696','ADM-SOLICITUD-12','admin@paulcars.com'),(11,'2026-10-31 08:16:31.373425','ADM-SOLICITUD-8','admin@paulcars.com'),(12,'2026-10-31 08:16:31.375425','ADM-SOLICITUD-7','admin@paulcars.com'),(13,'2026-10-31 08:16:31.377426','ADM-SOLICITUD-9','admin@paulcars.com'),(14,'2026-10-31 08:16:31.379427','ADM-SOLICITUD-6','admin@paulcars.com'),(15,'2026-10-31 08:16:31.382471','ADM-SOLICITUD-5','admin@paulcars.com'),(16,'2026-10-31 08:16:31.384480','ADM-SOLICITUD-4','admin@paulcars.com'),(17,'2026-10-31 08:16:31.387363','ADM-SOLICITUD-3','admin@paulcars.com'),(18,'2026-10-31 08:16:31.389655','ADM-SOLICITUD-2','admin@paulcars.com'),(19,'2026-10-31 08:16:31.391473','ADM-SOLICITUD-1','admin@paulcars.com'),(20,'2026-10-31 08:16:31.394160','DEV-9-2026-11-01','admin@paulcars.com'),(21,'2026-10-31 08:16:31.396169','ADM-APROBADA-11','admin@paulcars.com'),(22,'2026-10-31 08:16:31.398491','ADM-PAGO-11','admin@paulcars.com'),(23,'2026-10-31 08:16:31.400069','ADM-APROBADA-13','admin@paulcars.com'),(24,'2026-10-31 08:16:31.401641','ADM-APROBADA-10','admin@paulcars.com'),(25,'2026-10-31 08:16:31.403244','ADM-PAGO-10','admin@paulcars.com'),(26,'2026-10-31 08:16:31.405356','ADM-APROBADA-12','admin@paulcars.com'),(27,'2026-10-31 08:16:31.407581','ADM-APROBADA-8','admin@paulcars.com'),(28,'2026-10-31 08:16:31.409703','ADM-PAGO-8','admin@paulcars.com'),(29,'2026-10-31 08:16:31.411338','ADM-ENRUTA-8','admin@paulcars.com'),(30,'2026-10-31 08:16:31.412905','ADM-FINALIZADO-6','admin@paulcars.com'),(31,'2026-10-31 08:16:31.413954','ADM-APROBADA-7','admin@paulcars.com'),(32,'2026-10-31 08:16:31.415005','ADM-PAGO-7','admin@paulcars.com'),(33,'2026-10-31 08:16:31.416616','ADM-ENRUTA-7','admin@paulcars.com'),(34,'2026-10-31 08:16:31.417656','ADM-APROBADA-9','admin@paulcars.com'),(35,'2026-10-31 08:16:31.418689','ADM-PAGO-9','admin@paulcars.com'),(36,'2026-10-31 08:16:31.419727','ADM-ENRUTA-9','admin@paulcars.com'),(37,'2026-10-31 08:16:31.420732','ADM-FINALIZADO-5','admin@paulcars.com'),(38,'2026-10-31 08:16:31.420732','ADM-APROBADA-6','admin@paulcars.com'),(39,'2026-10-31 08:16:31.422303','ADM-PAGO-6','admin@paulcars.com'),(40,'2026-10-31 08:16:31.423310','ADM-ENRUTA-6','admin@paulcars.com'),(41,'2026-10-31 08:16:31.424311','ADM-FINALIZADO-4','admin@paulcars.com'),(42,'2026-10-31 08:16:31.425816','ADM-FINALIZADO-3','admin@paulcars.com'),(43,'2026-10-31 08:16:31.426818','ADM-APROBADA-5','admin@paulcars.com'),(44,'2026-10-31 08:16:31.427818','ADM-PAGO-5','admin@paulcars.com'),(45,'2026-10-31 08:16:31.428824','ADM-ENRUTA-5','admin@paulcars.com'),(46,'2026-10-31 08:16:31.429820','ADM-APROBADA-4','admin@paulcars.com'),(47,'2026-10-31 08:16:31.430818','ADM-PAGO-4','admin@paulcars.com'),(48,'2026-10-31 08:16:31.431870','ADM-ENRUTA-4','admin@paulcars.com'),(49,'2026-10-31 08:16:31.433877','ADM-APROBADA-3','admin@paulcars.com'),(50,'2026-10-31 08:16:31.434871','ADM-PAGO-3','admin@paulcars.com'),(51,'2026-10-31 08:16:31.435870','ADM-ENRUTA-3','admin@paulcars.com'),(52,'2026-10-31 08:16:31.435870','ADM-FINALIZADO-2','admin@paulcars.com'),(53,'2026-10-31 08:16:31.437871','ADM-APROBADA-2','admin@paulcars.com'),(54,'2026-10-31 08:16:31.438870','ADM-PAGO-2','admin@paulcars.com'),(55,'2026-10-31 08:16:31.440871','ADM-ENRUTA-2','admin@paulcars.com'),(56,'2026-10-31 08:16:31.440871','ADM-FINALIZADO-1','admin@paulcars.com'),(57,'2026-10-31 08:16:31.443114','ADM-APROBADA-1','admin@paulcars.com'),(58,'2026-10-31 08:16:31.444178','ADM-PAGO-1','admin@paulcars.com'),(59,'2026-10-31 08:16:31.445178','ADM-ENRUTA-1','admin@paulcars.com'),(60,'2026-11-01 05:19:49.750417','CLI-PERFIL-INCOMPLETO-8','jere@gmail.com'),(61,'2026-11-01 07:26:04.458137','ADM-TALLER-2026-11-01','admin@paulcars.com'),(62,'2026-11-01 08:03:57.442947','ADM-RESUMEN-2026-11-01','admin@paulcars.com'),(63,'2026-11-01 08:04:00.338717','ADM-STOCK-6','admin@paulcars.com');
ALTER TABLE `notificaciones_leidas` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `pagos`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `pagos` (
  `fecha_pago` date DEFAULT NULL,
  `id_alquiler` int NOT NULL,
  `id_metodo_pago` int NOT NULL,
  `id_pago` int NOT NULL AUTO_INCREMENT,
  `monto_recibido` decimal(10,2) DEFAULT NULL,
  `monto_total` decimal(10,2) NOT NULL,
  `ultimos_digitos` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vuelto` decimal(10,2) DEFAULT NULL,
  `fecha_validacion` datetime(6) DEFAULT NULL,
  `estado_pago` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_pago` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_operacion` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `canal` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `validado_por` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `banco_origen` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_pago`),
  KEY `FKqyuirfb85876vojmi8ss2kyls` (`id_alquiler`),
  KEY `FK474o9cjyc1poo1dlurbvadvfr` (`id_metodo_pago`),
  CONSTRAINT `FK474o9cjyc1poo1dlurbvadvfr` FOREIGN KEY (`id_metodo_pago`) REFERENCES `metodospago` (`id_metodo_pago`),
  CONSTRAINT `FKqyuirfb85876vojmi8ss2kyls` FOREIGN KEY (`id_alquiler`) REFERENCES `alquileres` (`id_alquiler`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `pagos` WRITE;
ALTER TABLE `pagos` DISABLE KEYS;
INSERT INTO `pagos` VALUES ('2026-10-13',1,1,1,NULL,465.00,'4532',NULL,'2026-10-13 15:15:00.000000','Pagado','ALQUILER','TRX-CARD-90101122','DIGITAL','CULQI_GATEWAY_PCI_DSS','VISA ÔÇó Titular: CARLOS MENDOZA (Token Culqi: tok_culqi_901011)'),('2026-10-17',2,4,2,NULL,283.50,NULL,NULL,'2026-10-17 15:15:00.000000','Pagado','ALQUILER','TRX-PLIN-90102233','DIGITAL','PLIN_GATEWAY_INTEROPERABLE','PLIN (BBVA - 991234567)'),('2026-10-21',3,3,3,NULL,886.00,NULL,NULL,'2026-10-21 15:15:00.000000','Pagado','ALQUILER','TRX-YAPE-90103344','DIGITAL','YAPE_GATEWAY_CULQI','YAPE (987451230)'),('2026-10-22',4,1,4,NULL,1060.00,'8841',NULL,'2026-10-22 15:15:00.000000','Pagado','ALQUILER','TRX-CARD-90104455','DIGITAL','CULQI_GATEWAY_PCI_DSS','MASTERCARD ÔÇó Titular: DIEGO ALVAREZ (Token Culqi: tok_culqi_901044)'),('2026-10-25',5,2,5,233.00,233.00,NULL,0.00,'2026-10-25 15:15:00.000000','Pagado','ALQUILER','POS-EFE-001055','COUNTER','COUNTER_PRESENCIAL',NULL),('2026-10-26',6,3,6,NULL,270.00,NULL,NULL,'2026-10-26 15:15:00.000000','Pagado','ALQUILER','TRX-YAPE-90106677','DIGITAL','YAPE_GATEWAY_CULQI','YAPE (965432187)'),('2026-10-29',7,1,7,NULL,540.00,'4532',NULL,'2026-10-29 15:15:00.000000','Pagado','ALQUILER','TRX-CARD-90201188','DIGITAL','CULQI_GATEWAY_PCI_DSS','VISA ÔÇó Titular: CARLOS MENDOZA (Token Culqi: tok_culqi_902011)'),('2026-10-30',8,4,8,NULL,589.50,NULL,NULL,'2026-10-30 15:15:00.000000','Pagado','ALQUILER','TRX-PLIN-90202299','DIGITAL','PLIN_GATEWAY_INTEROPERABLE','PLIN (INTERBANK - 984567123)'),('2026-10-29',9,2,9,664.50,664.50,NULL,0.00,'2026-10-29 15:15:00.000000','Pagado','ALQUILER','POS-EFE-002033','COUNTER','COUNTER_PRESENCIAL',NULL),('2026-10-30',10,1,10,NULL,480.00,'4532',NULL,'2026-10-30 15:15:00.000000','Pagado','ALQUILER','TRX-CARD-90301144','DIGITAL','CULQI_GATEWAY_PCI_DSS','VISA ÔÇó Titular: CARLOS MENDOZA (Token Culqi: tok_culqi_903011)'),('2026-10-31',11,3,11,NULL,814.50,NULL,NULL,'2026-10-31 15:15:00.000000','Pagado','ALQUILER','TRX-YAPE-90302255','DIGITAL','YAPE_GATEWAY_CULQI','YAPE (965432187)'),('2026-11-01',17,3,12,NULL,270.00,NULL,NULL,'2026-11-01 05:22:35.460290','Pagado','ALQUILER','TRX-YAPE-10555457','DIGITAL','YAPE_GATEWAY_CULQI','YAPE (932342323)'),('2026-11-01',18,1,13,NULL,270.00,'3213',NULL,'2026-11-01 05:27:14.189002','Pagado','ALQUILER','TRX-CARD-10834185','DIGITAL','CULQI_GATEWAY_PCI_DSS','AMEX • Titular: JEREMY MARTINEZ (Token Culqi: tok_culqi_live_1793510833067_v4qzm1i)'),('2026-11-01',19,2,14,250.00,250.00,NULL,0.00,'2026-11-01 05:29:23.813296','Pagado','ALQUILER',NULL,'COUNTER','COUNTER_PRESENCIAL',NULL);
ALTER TABLE `pagos` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `roles`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `roles` (
  `id_rol` int NOT NULL AUTO_INCREMENT,
  `nombre_rol` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `descripcion` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_rol`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `roles` WRITE;
ALTER TABLE `roles` DISABLE KEYS;
INSERT INTO `roles` VALUES (1,'ADMIN','Administrador principal del sistema'),(2,'EMPLEADO','Personal de atenci├│n en counter y operaciones de flota'),(3,'CLIENTE','Cliente titular de reservas y alquileres');
ALTER TABLE `roles` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `tiposvehiculo`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `tiposvehiculo` (
  `id_tipo` int NOT NULL AUTO_INCREMENT,
  `nombre_tipo` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_tipo`),
  UNIQUE KEY `UKq0hreoee71xpsovbrca6ucl79` (`nombre_tipo`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `tiposvehiculo` WRITE;
ALTER TABLE `tiposvehiculo` DISABLE KEYS;
INSERT INTO `tiposvehiculo` VALUES (4,'HATCHBACK'),(3,'PICKUP'),(1,'SEDAN'),(2,'SUV');
ALTER TABLE `tiposvehiculo` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `usuarios`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `usuarios` (
  `estado` bit(1) NOT NULL,
  `id_cliente` int DEFAULT NULL,
  `id_rol` int NOT NULL,
  `id_usuario` int NOT NULL AUTO_INCREMENT,
  `fecha_creacion` datetime(6) NOT NULL,
  `ultimo_acceso` datetime(6) DEFAULT NULL,
  `dni` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `apellidos` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombres` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email_recuperacion` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_completo` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `UKm2dvbwfge291euvmk6vkkocao` (`username`),
  UNIQUE KEY `UKk7pbarwjggf5lsltnkfjfxhj9` (`id_cliente`),
  KEY `FK3kl77pehgupicftwfreqnjkll` (`id_rol`),
  CONSTRAINT `FK3kl77pehgupicftwfreqnjkll` FOREIGN KEY (`id_rol`) REFERENCES `roles` (`id_rol`),
  CONSTRAINT `FKpl382ajo0dgn8d7o4gii3f1mq` FOREIGN KEY (`id_cliente`) REFERENCES `clientes` (`id_cliente`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `usuarios` WRITE;
ALTER TABLE `usuarios` DISABLE KEYS;
INSERT INTO `usuarios` VALUES (_binary '',NULL,1,1,'2026-10-31 06:41:32.894199','2026-11-01 07:59:40.476337','70000001','Paredes Silva','Víctor Manuel','admin@paulcars.com','Paul Cars Administrador','$2a$12$KqR7GFu3jElxkh.kx/KJn.n1DL1ZqWKCZXMjn1thkqfFPLdetqVLO','admin@paulcars.com'),(_binary '',NULL,2,2,'2026-10-31 06:41:33.169869','2026-10-31 18:20:10.600595','72172845','Mendoza Muñoz','Carlos Miguel','asesor1@paulcars.com','Carlos Mendoza - Counter Miraflores','$2a$12$DJoMEBYe/lPVv39OGIMyK.7LrtiTB.rVLrDWD043/XeFL8FNjqTpC','asesor1@paulcars.com'),(_binary '',NULL,2,3,'2026-10-31 06:41:33.437157',NULL,NULL,NULL,NULL,'counter.aeropuerto@paulcars.com','Sofía Rodríguez - Counter Aeropuerto','$2a$12$DJoMEBYe/lPVv39OGIMyK.7LrtiTB.rVLrDWD043/XeFL8FNjqTpC','counter.aeropuerto@paulcars.com'),(_binary '',NULL,2,4,'2026-10-31 06:41:33.708822',NULL,NULL,NULL,NULL,'carlos.mendoza@paulcars.com','Carlos Mendoza','$2a$12$DJoMEBYe/lPVv39OGIMyK.7LrtiTB.rVLrDWD043/XeFL8FNjqTpC','carlos.mendoza@paulcars.com'),(_binary '',NULL,2,5,'2026-10-31 06:41:33.983644',NULL,NULL,NULL,NULL,'sofia.rodriguez@paulcars.com','Sofía Rodríguez','$2a$12$DJoMEBYe/lPVv39OGIMyK.7LrtiTB.rVLrDWD043/XeFL8FNjqTpC','sofia.rodriguez@paulcars.com'),(_binary '',1,3,6,'2026-10-31 06:41:34.278155','2026-10-31 18:20:10.880204',NULL,NULL,NULL,'carlos.mendoza92@gmail.com','Carlos Alberto Mendoza Ríos','$2a$12$1VRgKwraj5kjMv.zsisV6ezT1/Hmrq9N6t1s0XbrWY5ttTvcTiXLO','carlos.mendoza92@gmail.com'),(_binary '',2,3,7,'2026-10-31 06:41:34.563176',NULL,NULL,NULL,NULL,'valeria.torres.p@gmail.com','Valeria Fernanda Torres Paredes','$2a$12$1VRgKwraj5kjMv.zsisV6ezT1/Hmrq9N6t1s0XbrWY5ttTvcTiXLO','valeria.torres.p@gmail.com'),(_binary '',3,3,8,'2026-10-31 06:41:34.837681',NULL,NULL,NULL,NULL,'diego.alvarez.v@gmail.com','Diego Alejandro Álvarez Vargas','$2a$12$1VRgKwraj5kjMv.zsisV6ezT1/Hmrq9N6t1s0XbrWY5ttTvcTiXLO','diego.alvarez.v@gmail.com'),(_binary '',4,3,9,'2026-10-31 06:41:35.112625',NULL,NULL,NULL,NULL,'camila.salazar.m@gmail.com','Camila Lucía Salazar Montes','$2a$12$1VRgKwraj5kjMv.zsisV6ezT1/Hmrq9N6t1s0XbrWY5ttTvcTiXLO','camila.salazar.m@gmail.com'),(_binary '',5,3,10,'2026-10-31 06:41:35.396218',NULL,NULL,NULL,NULL,'rodrigo.castillo.g@gmail.com','Rodrigo Sebastián Castillo Guzmán','$2a$12$1VRgKwraj5kjMv.zsisV6ezT1/Hmrq9N6t1s0XbrWY5ttTvcTiXLO','rodrigo.castillo.g@gmail.com'),(_binary '',6,3,11,'2026-10-31 06:41:35.678824',NULL,NULL,NULL,NULL,'lucia.fernandez.c@gmail.com','Lucía Gabriela Fernández Castro','$2a$12$1VRgKwraj5kjMv.zsisV6ezT1/Hmrq9N6t1s0XbrWY5ttTvcTiXLO','lucia.fernandez.c@gmail.com'),(_binary '',7,3,12,'2026-10-31 06:41:35.957525',NULL,NULL,NULL,NULL,'mateo.ramirez.d@gmail.com','Mateo Nicolás Ramírez Delgado','$2a$12$1VRgKwraj5kjMv.zsisV6ezT1/Hmrq9N6t1s0XbrWY5ttTvcTiXLO','mateo.ramirez.d@gmail.com'),(_binary '',NULL,2,13,'2026-10-31 06:41:33.169869','2026-11-01 05:55:24.503358','72172845','Mendoza Muñoz','Carlos Miguel','asesor@paulcars.com','Carlos Mendoza - Counter Miraflores','$2a$12$DJoMEBYe/lPVv39OGIMyK.7LrtiTB.rVLrDWD043/XeFL8FNjqTpC','asesor@paulcars.com'),(_binary '',8,3,14,'2026-11-01 05:19:18.682641','2026-11-01 05:35:13.179290',NULL,NULL,NULL,'jere@gmail.com',NULL,'$2a$12$9sWYf0c6SfNQA5mz4gMeJOQtWbNDXl/5iGxx/W85nYhpD7rcSY09u','jere@gmail.com'),(_binary '\0',NULL,2,15,'2026-11-01 05:31:41.801776','2026-11-01 06:42:34.020340','32323232','Muñoz','Alex','alex.munoz@paulcars.com','Alex Muñoz','$2a$12$2bWXJxw.D68aTTjvLKglguxiKlUcRj4O4UuzE./O1NpMmpT0nlFWW','alex.munoz@paulcars.com');
ALTER TABLE `usuarios` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `vehiculos_modelos`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `vehiculos_modelos` (
  `activo` bit(1) NOT NULL,
  `anio` int DEFAULT NULL,
  `garantia` decimal(10,2) DEFAULT NULL,
  `id_combustible` int NOT NULL,
  `id_marca` int NOT NULL,
  `id_modelo` int NOT NULL AUTO_INCREMENT,
  `id_tipo` int NOT NULL,
  `precio_dia` decimal(10,2) NOT NULL,
  `precio_hora` decimal(10,2) NOT NULL,
  `sede` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nombre_modelo` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `imagen_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_modelo`),
  KEY `FK6gew3xuciqmhoxccsw4x7177j` (`id_combustible`),
  KEY `FKl9uot9ntgmup8vol2x27y8ftb` (`id_marca`),
  KEY `FK5i1ifprqdsgps3nnqspf5x1ju` (`id_tipo`),
  CONSTRAINT `FK5i1ifprqdsgps3nnqspf5x1ju` FOREIGN KEY (`id_tipo`) REFERENCES `tiposvehiculo` (`id_tipo`),
  CONSTRAINT `FK6gew3xuciqmhoxccsw4x7177j` FOREIGN KEY (`id_combustible`) REFERENCES `combustibles` (`id_combustible`),
  CONSTRAINT `FKl9uot9ntgmup8vol2x27y8ftb` FOREIGN KEY (`id_marca`) REFERENCES `marcas` (`id_marca`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `vehiculos_modelos` WRITE;
ALTER TABLE `vehiculos_modelos` DISABLE KEYS;
INSERT INTO `vehiculos_modelos` VALUES (_binary '',2022,3500.00,1,1,1,1,120.00,15.00,'AEROPUERTO','Corolla','/images/vehiculos/corolla.jpg'),(_binary '',2023,3500.00,1,2,2,1,110.00,14.00,'AEROPUERTO','Versa','/images/vehiculos/versa.jpg'),(_binary '',2023,3500.00,1,3,3,1,125.00,15.00,'MIRAFLORES','3','/images/vehiculos/mazda3.jpg'),(_binary '',2023,5000.00,1,4,4,2,180.00,22.00,'AEROPUERTO','Tucson','/images/vehiculos/tucson.jpg'),(_binary '',2024,5000.00,3,2,5,2,160.00,20.00,'MIRAFLORES','Kicks','/images/vehiculos/kicks.jpg'),(_binary '',2023,5000.00,1,5,6,2,155.00,19.00,'MIRAFLORES','ASX','/images/vehiculos/asx.jpg'),(_binary '',2024,7000.00,2,1,7,3,230.00,28.00,'AEROPUERTO','Hilux','/images/vehiculos/hilux.jpg'),(_binary '\0',2023,7000.00,2,5,8,3,215.00,26.00,'MIRAFLORES','L200','/images/vehiculos/l200.jpg'),(_binary '',2024,7000.00,2,6,9,3,235.00,29.00,'AEROPUERTO','Ranger','/images/vehiculos/ranger.jpg'),(_binary '',2022,2500.00,1,7,10,4,90.00,12.00,'AEROPUERTO','Rio','/images/vehiculos/rio.jpg'),(_binary '',2024,2500.00,1,8,11,4,88.00,11.00,'AEROPUERTO','Swift','/images/vehiculos/swift.jpg');
ALTER TABLE `vehiculos_modelos` ENABLE KEYS;
UNLOCK TABLES;
DROP TABLE IF EXISTS `vehiculos_unidades`;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8mb4;
CREATE TABLE `vehiculos_unidades` (
  `id_estado` int NOT NULL,
  `id_modelo` int NOT NULL,
  `id_unidad` int NOT NULL AUTO_INCREMENT,
  `kilometraje` int DEFAULT NULL,
  `placa` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `color` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_motor` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `numero_vin` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sede` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id_unidad`),
  UNIQUE KEY `UKbmpfaekxfbu9uk5u0hpndlx46` (`placa`),
  KEY `FKciyqcbwvejs3vupugy90uingw` (`id_estado`),
  KEY `FKss5133fs9lr7pwmqec7d44ngg` (`id_modelo`),
  CONSTRAINT `FKciyqcbwvejs3vupugy90uingw` FOREIGN KEY (`id_estado`) REFERENCES `estadosvehiculo` (`id_estado`),
  CONSTRAINT `FKss5133fs9lr7pwmqec7d44ngg` FOREIGN KEY (`id_modelo`) REFERENCES `vehiculos_modelos` (`id_modelo`)
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET character_set_client = @saved_cs_client;
LOCK TABLES `vehiculos_unidades` WRITE;
ALTER TABLE `vehiculos_unidades` DISABLE KEYS;
INSERT INTO `vehiculos_unidades` VALUES (1,1,1,12500,'ABC-121','Blanco Glaciar','MTR001-A','VINTOY00000000101','AEROPUERTO'),(1,1,2,18200,'ABC-122','Blanco Glaciar','MTR001-B','VINTOY00000000102','AEROPUERTO'),(1,1,3,9400,'ABC-123','Blanco Glaciar','MTR001-C','VINTOY00000000103','MIRAFLORES'),(1,1,4,15300,'ABC-124','Blanco Glaciar','MTR001-D','VINTOY00000000104','MIRAFLORES'),(1,1,5,11200,'ABC-125','Blanco Glaciar','MTR001-E','VINTOY00000000105','AEROPUERTO'),(4,1,6,14100,'ABC-126','Blanco Glaciar','MTR001-F','VINTOY00000000106','MIRAFLORES'),(1,2,7,8200,'NSS-201','Blanco Polar','MTR006-A','VINNIS00000000201','AEROPUERTO'),(1,2,8,11400,'NSS-202','Blanco Polar','MTR006-B','VINNIS00000000202','MIRAFLORES'),(1,2,9,14500,'NSS-203','Blanco Polar','MTR006-C','VINNIS00000000203','AEROPUERTO'),(1,2,10,9900,'NSS-204','Blanco Polar','MTR006-D','VINNIS00000000204','MIRAFLORES'),(5,2,11,13200,'NSS-205','Blanco Polar','MTR006-E','VINNIS00000000205','AEROPUERTO'),(1,3,12,13200,'MZD-401','Gris Polymetal','MTR008-A','VINMZD00000000401','MIRAFLORES'),(1,3,13,8500,'MZD-402','Gris Polymetal','MTR008-B','VINMZD00000000402','MIRAFLORES'),(1,3,14,10900,'MZD-403','Gris Polymetal','MTR008-C','VINMZD00000000403','AEROPUERTO'),(1,3,15,14700,'MZD-404','Gris Polymetal','MTR008-D','VINMZD00000000404','AEROPUERTO'),(4,3,16,11800,'MZD-405','Gris Polymetal','MTR008-E','VINMZD00000000405','MIRAFLORES'),(1,4,17,10500,'HYU-801','Negro Fantasma','MTR011-A','VINHYU00000000801','AEROPUERTO'),(1,4,18,13200,'HYU-802','Negro Fantasma','MTR011-B','VINHYU00000000802','AEROPUERTO'),(1,4,19,7800,'HYU-803','Negro Fantasma','MTR011-C','VINHYU00000000803','MIRAFLORES'),(1,4,20,15400,'HYU-804','Negro Fantasma','MTR011-D','VINHYU00000000804','MIRAFLORES'),(1,4,21,9200,'HYU-805','Negro Fantasma','MTR011-E','VINHYU00000000805','AEROPUERTO'),(5,4,22,11900,'HYU-806','Negro Fantasma','MTR011-F','VINHYU00000000806','MIRAFLORES'),(4,4,23,14300,'HYU-807','Negro Fantasma','MTR011-G','VINHYU00000000807','AEROPUERTO'),(1,5,24,6900,'NIS-901','Azul Eléctrico','MTR012-A','VINNIS00000000901','MIRAFLORES'),(1,5,25,11800,'NIS-902','Azul Eléctrico','MTR012-B','VINNIS00000000902','MIRAFLORES'),(1,5,26,9400,'NIS-903','Azul Eléctrico','MTR012-C','VINNIS00000000903','AEROPUERTO'),(1,5,27,12700,'NIS-904','Azul Eléctrico','MTR012-D','VINNIS00000000904','AEROPUERTO'),(5,5,28,8600,'NIS-905','Azul Eléctrico','MTR012-E','VINNIS00000000905','MIRAFLORES'),(2,6,29,11200,'ASX-101','Gris Titanio','MTR014-A','VINMIT00000001101','MIRAFLORES'),(4,6,30,15400,'ASX-102','Gris Titanio','MTR014-B','VINMIT00000001102','MIRAFLORES'),(4,6,31,9300,'ASX-103','Gris Titanio','MTR014-C','VINMIT00000001103','AEROPUERTO'),(5,6,32,13700,'ASX-104','Gris Titanio','MTR014-D','VINMIT00000001104','AEROPUERTO'),(5,6,33,10100,'ASX-105','Gris Titanio','MTR014-E','VINMIT00000001105','MIRAFLORES'),(1,7,34,9800,'HLX-301','Plata','MTR003-A','VINTOY00000000301','AEROPUERTO'),(1,7,35,14300,'HLX-302','Plata','MTR003-B','VINTOY00000000302','AEROPUERTO'),(1,7,36,18100,'HLX-303','Plata','MTR003-C','VINTOY00000000303','MIRAFLORES'),(1,7,37,11200,'HLX-304','Plata','MTR003-D','VINTOY00000000304','MIRAFLORES'),(1,7,38,15400,'HLX-305','Plata','MTR003-E','VINTOY00000000305','AEROPUERTO'),(4,7,39,8700,'HLX-306','Plata','MTR003-F','VINTOY00000000306','MIRAFLORES'),(5,7,40,13900,'HLX-307','Plata','MTR003-G','VINTOY00000000307','AEROPUERTO'),(2,8,41,13100,'L20-501','Blanco Polar','MTR017-A','VINMIT00000001501','MIRAFLORES'),(1,8,42,18200,'L20-502','Blanco Polar','MTR017-B','VINMIT00000001502','MIRAFLORES'),(1,8,43,9700,'L20-503','Blanco Polar','MTR017-C','VINMIT00000001503','AEROPUERTO'),(1,8,44,15600,'L20-504','Blanco Polar','MTR017-D','VINMIT00000001504','AEROPUERTO'),(4,8,45,11800,'L20-505','Blanco Polar','MTR017-E','VINMIT00000001505','MIRAFLORES'),(2,9,46,8400,'RNG-601','Azul Relámpago','MTR018-A','VINFRD00000001601','AEROPUERTO'),(1,9,47,12300,'RNG-602','Azul Relámpago','MTR018-B','VINFRD00000001602','AEROPUERTO'),(1,9,48,10100,'RNG-603','Azul Relámpago','MTR018-C','VINFRD00000001603','MIRAFLORES'),(1,9,49,15800,'RNG-604','Azul Relámpago','MTR018-D','VINFRD00000001604','MIRAFLORES'),(1,9,50,9200,'RNG-605','Azul Relámpago','MTR018-E','VINFRD00000001605','AEROPUERTO'),(5,9,51,13700,'RNG-606','Azul Relámpago','MTR018-F','VINFRD00000001606','MIRAFLORES'),(1,10,52,16400,'JKL-011','Rojo Fuego','MTR004-A','VINKIA00000001801','AEROPUERTO'),(1,10,53,18900,'JKL-012','Rojo Fuego','MTR004-B','VINKIA00000001802','AEROPUERTO'),(1,10,54,14300,'JKL-013','Rojo Fuego','MTR004-C','VINKIA00000001803','MIRAFLORES'),(1,10,55,19200,'JKL-014','Rojo Fuego','MTR004-D','VINKIA00000001804','MIRAFLORES'),(1,10,56,12100,'JKL-015','Rojo Fuego','MTR004-E','VINKIA00000001805','AEROPUERTO'),(4,10,57,15800,'JKL-016','Rojo Fuego','MTR004-F','VINKIA00000001806','MIRAFLORES'),(1,11,58,8600,'SZK-701','Amarillo Deportivo','MTR021-A','VINSZK00000002001','AEROPUERTO'),(1,11,59,11200,'SZK-702','Amarillo Deportivo','MTR021-B','VINSZK00000002002','AEROPUERTO'),(1,11,60,9400,'SZK-703','Amarillo Deportivo','MTR021-C','VINSZK00000002003','MIRAFLORES'),(1,11,61,10300,'SZK-704','Amarillo Deportivo','MTR021-D','VINSZK00000002004','MIRAFLORES'),(1,11,62,12700,'SZK-705','Amarillo Deportivo','MTR021-E','VINSZK00000002005','AEROPUERTO'),(5,11,63,7900,'SZK-706','Amarillo Deportivo','MTR021-F','VINSZK00000002006','MIRAFLORES');
ALTER TABLE `vehiculos_unidades` ENABLE KEYS;
UNLOCK TABLES;
SET TIME_ZONE=@OLD_TIME_ZONE;
SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT;
SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS;
SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION;
SET SQL_NOTES=@OLD_SQL_NOTES;
