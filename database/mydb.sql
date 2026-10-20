-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 19-10-2026 a las 23:23:44
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.0.30

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `mydb`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `archivo_historial`
--

CREATE TABLE `archivo_historial` (
  `idArchivo` int(11) NOT NULL,
  `tipoArchivo` varchar(255) NOT NULL,
  `numeroHistorial` int(11) NOT NULL,
  `nombreArchivo` varchar(255) DEFAULT NULL,
  `rutaArchivo` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `archivo_historial`
--

INSERT INTO `archivo_historial` (`idArchivo`, `tipoArchivo`, `numeroHistorial`, `nombreArchivo`, `rutaArchivo`) VALUES
(2, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 2, 'Entregable ingles (2).xlsx', 'uploads/historial/2/384fabe2-4efa-4671-9df5-6b390fd0ab8f-Entregable ingles (2).xlsx'),
(3, 'application/pdf', 46, 'examen_46_3.pdf', 'uploads/historial/46/3-examen_46_3.pdf'),
(4, 'application/pdf', 36, 'examen_36_4.pdf', 'uploads/historial/36/4-examen_36_4.pdf'),
(5, 'image/jpeg', 6, 'examen_6_5.jpg', 'uploads/historial/6/5-examen_6_5.jpg'),
(6, 'image/jpeg', 4, 'examen_4_6.jpg', 'uploads/historial/4/6-examen_4_6.jpg'),
(7, 'application/pdf', 28, 'examen_28_7.pdf', 'uploads/historial/28/7-examen_28_7.pdf'),
(8, 'application/pdf', 51, 'examen_51_8.pdf', 'uploads/historial/51/8-examen_51_8.pdf'),
(9, 'image/png', 51, 'examen_51_9.png', 'uploads/historial/51/9-examen_51_9.png'),
(10, 'image/png', 48, 'examen_48_10.png', 'uploads/historial/48/10-examen_48_10.png'),
(11, 'image/jpeg', 28, 'examen_28_11.jpg', 'uploads/historial/28/11-examen_28_11.jpg'),
(12, 'image/jpeg', 26, 'examen_26_12.jpg', 'uploads/historial/26/12-examen_26_12.jpg'),
(13, 'application/pdf', 153, 'examen_153_13.pdf', 'uploads/historial/153/13-examen_153_13.pdf'),
(14, 'image/jpeg', 80, 'examen_80_14.jpg', 'uploads/historial/80/14-examen_80_14.jpg'),
(15, 'image/jpeg', 110, 'examen_110_15.jpg', 'uploads/historial/110/15-examen_110_15.jpg'),
(16, 'image/png', 154, 'examen_154_16.png', 'uploads/historial/154/16-examen_154_16.png'),
(17, 'image/png', 153, 'examen_153_17.png', 'uploads/historial/153/17-examen_153_17.png'),
(18, 'image/png', 81, 'examen_81_18.png', 'uploads/historial/81/18-examen_81_18.png'),
(19, 'application/pdf', 71, 'examen_71_19.pdf', 'uploads/historial/71/19-examen_71_19.pdf'),
(20, 'image/png', 97, 'examen_97_20.png', 'uploads/historial/97/20-examen_97_20.png'),
(21, 'image/jpeg', 91, 'examen_91_21.jpg', 'uploads/historial/91/21-examen_91_21.jpg'),
(22, 'application/pdf', 143, 'examen_143_22.pdf', 'uploads/historial/143/22-examen_143_22.pdf'),
(23, 'image/jpeg', 59, 'examen_59_23.jpg', 'uploads/historial/59/23-examen_59_23.jpg'),
(24, 'image/jpeg', 143, 'examen_143_24.jpg', 'uploads/historial/143/24-examen_143_24.jpg'),
(25, 'image/png', 110, 'examen_110_25.png', 'uploads/historial/110/25-examen_110_25.png'),
(26, 'image/jpeg', 131, 'examen_131_26.jpg', 'uploads/historial/131/26-examen_131_26.jpg'),
(27, 'image/jpeg', 130, 'examen_130_27.jpg', 'uploads/historial/130/27-examen_130_27.jpg'),
(28, 'image/jpeg', 117, 'examen_117_28.jpg', 'uploads/historial/117/28-examen_117_28.jpg'),
(29, 'application/pdf', 107, 'examen_107_29.pdf', 'uploads/historial/107/29-examen_107_29.pdf'),
(30, 'application/pdf', 152, 'examen_152_30.pdf', 'uploads/historial/152/30-examen_152_30.pdf'),
(31, 'image/jpeg', 69, 'examen_69_31.jpg', 'uploads/historial/69/31-examen_69_31.jpg'),
(32, 'application/pdf', 144, 'examen_144_32.pdf', 'uploads/historial/144/32-examen_144_32.pdf'),
(33, 'application/pdf', 104, 'examen_104_33.pdf', 'uploads/historial/104/33-examen_104_33.pdf'),
(34, 'image/png', 173, 'examen_173_34.png', 'uploads/historial/173/34-examen_173_34.png'),
(35, 'image/png', 64, 'examen_64_35.png', 'uploads/historial/64/35-examen_64_35.png'),
(36, 'application/pdf', 167, 'examen_167_36.pdf', 'uploads/historial/167/36-examen_167_36.pdf'),
(37, 'image/jpeg', 174, 'examen_174_37.jpg', 'uploads/historial/174/37-examen_174_37.jpg'),
(38, 'image/png', 80, 'examen_80_38.png', 'uploads/historial/80/38-examen_80_38.png'),
(39, 'application/pdf', 141, 'examen_141_39.pdf', 'uploads/historial/141/39-examen_141_39.pdf'),
(40, 'image/jpeg', 86, 'examen_86_40.jpg', 'uploads/historial/86/40-examen_86_40.jpg'),
(41, 'image/jpeg', 142, 'examen_142_41.jpg', 'uploads/historial/142/41-examen_142_41.jpg'),
(42, 'image/jpeg', 133, 'examen_133_42.jpg', 'uploads/historial/133/42-examen_133_42.jpg'),
(43, 'application/pdf', 131, 'examen_131_43.pdf', 'uploads/historial/131/43-examen_131_43.pdf'),
(44, 'image/png', 97, 'examen_97_44.png', 'uploads/historial/97/44-examen_97_44.png'),
(45, 'image/png', 79, 'examen_79_45.png', 'uploads/historial/79/45-examen_79_45.png'),
(46, 'image/png', 119, 'examen_119_46.png', 'uploads/historial/119/46-examen_119_46.png'),
(47, 'image/jpeg', 63, 'examen_63_47.jpg', 'uploads/historial/63/47-examen_63_47.jpg'),
(48, 'image/jpeg', 94, 'examen_94_48.jpg', 'uploads/historial/94/48-examen_94_48.jpg'),
(49, 'application/pdf', 107, 'examen_107_49.pdf', 'uploads/historial/107/49-examen_107_49.pdf'),
(50, 'application/pdf', 141, 'examen_141_50.pdf', 'uploads/historial/141/50-examen_141_50.pdf'),
(51, 'application/pdf', 127, 'examen_127_51.pdf', 'uploads/historial/127/51-examen_127_51.pdf'),
(52, 'image/png', 103, 'examen_103_52.png', 'uploads/historial/103/52-examen_103_52.png'),
(53, 'image/jpeg', 258, 'examen_258_53.jpg', 'uploads/historial/258/53-examen_258_53.jpg'),
(54, 'application/pdf', 191, 'examen_191_54.pdf', 'uploads/historial/191/54-examen_191_54.pdf'),
(55, 'application/pdf', 194, 'examen_194_55.pdf', 'uploads/historial/194/55-examen_194_55.pdf'),
(56, 'image/png', 224, 'examen_224_56.png', 'uploads/historial/224/56-examen_224_56.png'),
(57, 'application/pdf', 219, 'examen_219_57.pdf', 'uploads/historial/219/57-examen_219_57.pdf'),
(58, 'image/png', 179, 'examen_179_58.png', 'uploads/historial/179/58-examen_179_58.png'),
(59, 'application/pdf', 180, 'examen_180_59.pdf', 'uploads/historial/180/59-examen_180_59.pdf'),
(60, 'application/pdf', 225, 'examen_225_60.pdf', 'uploads/historial/225/60-examen_225_60.pdf'),
(61, 'application/pdf', 208, 'examen_208_61.pdf', 'uploads/historial/208/61-examen_208_61.pdf'),
(62, 'application/pdf', 192, 'examen_192_62.pdf', 'uploads/historial/192/62-examen_192_62.pdf'),
(63, 'application/pdf', 194, 'examen_194_63.pdf', 'uploads/historial/194/63-examen_194_63.pdf'),
(64, 'application/pdf', 263, 'examen_263_64.pdf', 'uploads/historial/263/64-examen_263_64.pdf'),
(65, 'image/jpeg', 266, 'examen_266_65.jpg', 'uploads/historial/266/65-examen_266_65.jpg'),
(66, 'application/pdf', 228, 'examen_228_66.pdf', 'uploads/historial/228/66-examen_228_66.pdf'),
(67, 'image/png', 244, 'examen_244_67.png', 'uploads/historial/244/67-examen_244_67.png'),
(68, 'image/png', 200, 'examen_200_68.png', 'uploads/historial/200/68-examen_200_68.png'),
(69, 'application/pdf', 243, 'examen_243_69.pdf', 'uploads/historial/243/69-examen_243_69.pdf'),
(70, 'image/png', 243, 'examen_243_70.png', 'uploads/historial/243/70-examen_243_70.png'),
(71, 'application/pdf', 253, 'examen_253_71.pdf', 'uploads/historial/253/71-examen_253_71.pdf'),
(72, 'application/pdf', 238, 'examen_238_72.pdf', 'uploads/historial/238/72-examen_238_72.pdf'),
(73, 'image/jpeg', 208, 'examen_208_73.jpg', 'uploads/historial/208/73-examen_208_73.jpg'),
(74, 'image/jpeg', 266, 'examen_266_74.jpg', 'uploads/historial/266/74-examen_266_74.jpg'),
(75, 'image/png', 196, 'examen_196_75.png', 'uploads/historial/196/75-examen_196_75.png'),
(76, 'image/jpeg', 179, 'examen_179_76.jpg', 'uploads/historial/179/76-examen_179_76.jpg'),
(77, 'image/png', 237, 'examen_237_77.png', 'uploads/historial/237/77-examen_237_77.png'),
(78, 'application/pdf', 205, 'examen_205_78.pdf', 'uploads/historial/205/78-examen_205_78.pdf'),
(79, 'image/png', 200, 'examen_200_79.png', 'uploads/historial/200/79-examen_200_79.png'),
(80, 'image/png', 239, 'examen_239_80.png', 'uploads/historial/239/80-examen_239_80.png'),
(81, 'image/png', 277, 'examen_277_81.png', 'uploads/historial/277/81-examen_277_81.png'),
(82, 'image/png', 199, 'examen_199_82.png', 'uploads/historial/199/82-examen_199_82.png'),
(83, 'application/pdf', 214, 'examen_214_83.pdf', 'uploads/historial/214/83-examen_214_83.pdf'),
(84, 'image/png', 234, 'examen_234_84.png', 'uploads/historial/234/84-examen_234_84.png'),
(85, 'image/jpeg', 271, 'examen_271_85.jpg', 'uploads/historial/271/85-examen_271_85.jpg'),
(86, 'image/jpeg', 218, 'examen_218_86.jpg', 'uploads/historial/218/86-examen_218_86.jpg'),
(87, 'image/jpeg', 231, 'examen_231_87.jpg', 'uploads/historial/231/87-examen_231_87.jpg');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `barrio`
--

CREATE TABLE `barrio` (
  `idBarrio` int(11) NOT NULL,
  `nombre` varchar(45) NOT NULL,
  `idLocalidad` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `barrio`
--

INSERT INTO `barrio` (`idBarrio`, `nombre`, `idLocalidad`) VALUES
(1, 'Normandia', 1),
(2, 'Cedritos', 2),
(3, 'Santa Bárbara', 2),
(4, 'Bella Suiza', 2),
(5, 'Country Club', 2),
(6, 'Chapinero Central', 3),
(7, 'Rosales', 3),
(8, 'Chicó', 3),
(9, 'El Nogal', 3),
(10, 'Villa Luz', 1),
(11, 'Boyacá Real', 1),
(12, 'Santa Helenita', 1),
(13, 'Niza', 11),
(14, 'La Campiña', 11),
(15, 'Mazurén', 11),
(16, 'Tibabuyes', 11),
(17, 'Castilla', 9),
(18, 'Tintal', 9),
(19, 'Kennedy Central', 9),
(20, 'Patio Bonito', 9),
(21, 'Modelia', 10),
(22, 'Capellanía', 10),
(23, 'Hayuelos', 10),
(24, 'Villemar', 10),
(25, 'villa de cerro', 20),
(26, 'Provenza', 21),
(27, 'Manila', 21),
(28, 'Castropol', 22),
(29, 'La America', 23),
(30, 'La Frontera', 24),
(31, 'Tesoro', 24),
(32, 'Ciudad Jardin', 25),
(33, 'San Fernando Nuevo', 26),
(34, 'Menga', 27),
(35, 'Villa Country', 28),
(36, 'Alto Prado', 29),
(37, 'El Golf', 29),
(38, 'Riomar Norte', 30),
(39, 'Centro Historico', 31),
(40, 'Castillogrande', 32),
(41, 'Marbella', 32),
(42, 'El Cabrero', 33);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `calificacion`
--

CREATE TABLE `calificacion` (
  `idCalificacion` int(11) NOT NULL,
  `valor` int(5) NOT NULL,
  `descripcion` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `calificacion`
--

INSERT INTO `calificacion` (`idCalificacion`, `valor`, `descripcion`) VALUES
(1, 5, 'Excelente'),
(2, 4, 'Bueno'),
(3, 3, 'Regular');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `carnet_vacuna`
--

CREATE TABLE `carnet_vacuna` (
  `idCarnet` int(10) UNSIGNED NOT NULL,
  `fechaAplicacion` date NOT NULL,
  `proximaDosis` date NOT NULL,
  `dosis` varchar(45) NOT NULL,
  `observaciones` text DEFAULT NULL,
  `idLote` int(11) NOT NULL,
  `idUsuario` int(11) NOT NULL,
  `idVacuna` int(11) NOT NULL,
  `idMascota` int(10) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `carnet_vacuna`
--

INSERT INTO `carnet_vacuna` (`idCarnet`, `fechaAplicacion`, `proximaDosis`, `dosis`, `observaciones`, `idLote`, `idUsuario`, `idVacuna`, `idMascota`) VALUES
(2, '2026-09-15', '2026-09-30', '1er', '', 1, 2, 1, 1),
(3, '2026-01-22', '2026-10-19', '1era', 'Se aplico sin complicaciones', 11, 55, 3, 42),
(4, '2025-05-02', '2025-10-29', 'Unica', 'Sin reacciones adversas', 7, 55, 3, 47),
(5, '2025-01-04', '2025-07-03', '3era', 'Paciente toleró bien la dosis', 8, 55, 7, 35),
(6, '2025-08-24', '2026-02-20', 'Refuerzo', '', 6, 49, 5, 13),
(7, '2025-02-02', '2026-02-02', 'Refuerzo', '', 10, 2, 4, 14),
(8, '2025-04-14', '2026-04-14', '1era', '', 8, 56, 11, 46),
(9, '2025-09-08', '2026-09-08', '1era', 'Sin reacciones adversas', 7, 54, 4, 35),
(10, '2025-02-27', '2025-08-26', 'Unica', 'Se aplico sin complicaciones', 5, 50, 14, 59),
(11, '2026-08-15', '2027-08-15', '3era', 'Sin reacciones adversas', 4, 51, 15, 10),
(12, '2026-02-28', '2026-11-25', '3era', 'Sin reacciones adversas', 1, 55, 10, 57),
(13, '2026-03-14', '2027-03-14', 'Refuerzo', 'Paciente toleró bien la dosis', 1, 52, 11, 43),
(14, '2026-06-22', '2027-03-19', 'Refuerzo', 'Sin reacciones adversas', 7, 49, 5, 25),
(15, '2026-07-05', '2027-04-01', '2da', 'Sin reacciones adversas', 14, 50, 5, 55),
(16, '2026-04-20', '2026-10-17', 'Unica', 'Sin reacciones adversas', 7, 53, 9, 34),
(17, '2026-02-02', '2026-08-01', '3era', 'Paciente toleró bien la dosis', 4, 52, 11, 6),
(18, '2026-01-15', '2026-07-14', 'Unica', 'Paciente toleró bien la dosis', 12, 50, 1, 18),
(19, '2026-09-12', '2027-09-12', '1era', 'Sin reacciones adversas', 14, 48, 4, 52),
(20, '2026-05-06', '2026-11-02', '3era', 'Sin reacciones adversas', 5, 2, 4, 60),
(21, '2025-05-01', '2026-05-01', 'Refuerzo', 'Sin reacciones adversas', 15, 50, 12, 39),
(22, '2026-02-11', '2026-08-10', 'Unica', 'Se aplico sin complicaciones', 6, 51, 7, 30),
(23, '2025-11-12', '2026-08-09', '3era', 'Paciente toleró bien la dosis', 9, 54, 8, 7),
(24, '2025-11-24', '2026-05-23', '3era', 'Sin reacciones adversas', 6, 53, 1, 37),
(25, '2025-05-30', '2025-11-26', '3era', 'Paciente toleró bien la dosis', 9, 56, 10, 47),
(26, '2025-10-30', '2026-04-28', '2da', 'Sin reacciones adversas', 6, 50, 7, 37),
(27, '2026-05-27', '2027-05-27', '3era', 'Paciente toleró bien la dosis', 5, 54, 12, 42),
(28, '2026-04-17', '2026-10-14', '3era', 'Paciente toleró bien la dosis', 3, 49, 12, 35),
(29, '2025-07-08', '2026-07-08', '1era', '', 9, 2, 14, 55),
(30, '2025-02-19', '2025-08-18', 'Refuerzo', 'Sin reacciones adversas', 3, 50, 2, 46),
(31, '2025-01-10', '2025-07-09', 'Unica', 'Paciente toleró bien la dosis', 8, 52, 1, 40),
(32, '2026-05-15', '2026-11-11', '1era', '', 9, 55, 7, 1),
(33, '2026-06-27', '2027-06-27', '3era', 'Paciente toleró bien la dosis', 9, 51, 1, 33),
(34, '2025-07-03', '2025-12-30', '1era', 'Se aplico sin complicaciones', 9, 49, 4, 13),
(35, '2025-12-29', '2026-09-25', 'Refuerzo', 'Sin reacciones adversas', 2, 52, 7, 30),
(36, '2025-08-20', '2026-05-17', '1era', 'Paciente toleró bien la dosis', 11, 2, 2, 26),
(37, '2026-01-22', '2027-01-22', 'Refuerzo', '', 1, 2, 12, 11),
(38, '2026-05-27', '2027-02-21', '3era', '', 10, 48, 15, 34),
(39, '2025-08-24', '2026-02-20', 'Unica', '', 8, 51, 1, 60),
(40, '2025-10-15', '2026-10-15', 'Unica', 'Se aplico sin complicaciones', 11, 2, 3, 60),
(41, '2025-01-17', '2025-07-16', 'Unica', 'Se aplico sin complicaciones', 3, 53, 15, 5),
(42, '2025-06-16', '2026-06-16', '2da', 'Se aplico sin complicaciones', 10, 53, 11, 58),
(43, '2026-01-29', '2027-01-29', '2da', 'Se aplico sin complicaciones', 13, 48, 9, 48),
(44, '2025-02-24', '2025-08-23', 'Refuerzo', 'Paciente toleró bien la dosis', 4, 48, 6, 39),
(45, '2025-12-01', '2026-05-30', '3era', 'Se aplico sin complicaciones', 13, 54, 8, 15),
(46, '2026-07-21', '2027-04-17', 'Refuerzo', '', 3, 56, 11, 25),
(47, '2025-10-30', '2026-04-28', '1era', 'Sin reacciones adversas', 2, 51, 3, 25),
(48, '2026-02-03', '2026-10-31', '3era', 'Se aplico sin complicaciones', 2, 48, 1, 61),
(49, '2026-04-01', '2026-12-27', '3era', 'Paciente toleró bien la dosis', 2, 49, 2, 12),
(50, '2026-04-05', '2027-04-05', 'Unica', '', 9, 53, 2, 2),
(51, '2025-12-29', '2026-12-29', '1era', 'Paciente toleró bien la dosis', 10, 56, 13, 21),
(52, '2025-01-13', '2025-10-10', 'Refuerzo', 'Sin reacciones adversas', 7, 48, 12, 58),
(53, '2026-08-09', '2027-08-09', '2da', 'Se aplico sin complicaciones', 11, 53, 3, 9),
(54, '2025-11-05', '2026-08-02', 'Refuerzo', 'Se aplico sin complicaciones', 3, 48, 3, 28),
(55, '2026-03-08', '2026-12-03', 'Refuerzo', 'Sin reacciones adversas', 13, 48, 6, 17),
(56, '2026-05-23', '2027-05-23', 'Unica', 'Se aplico sin complicaciones', 4, 54, 2, 9),
(57, '2025-01-07', '2025-10-04', '3era', 'Se aplico sin complicaciones', 14, 56, 7, 52),
(58, '2026-03-27', '2026-12-22', 'Refuerzo', 'Se aplico sin complicaciones', 14, 56, 3, 20),
(59, '2026-09-10', '2027-09-10', '2da', 'Se aplico sin complicaciones', 8, 52, 3, 26),
(60, '2025-10-26', '2026-10-26', 'Refuerzo', 'Se aplico sin complicaciones', 10, 50, 6, 25),
(61, '2026-02-07', '2026-11-04', '1era', 'Sin reacciones adversas', 15, 56, 4, 38),
(62, '2026-07-18', '2027-01-14', 'Refuerzo', 'Se aplico sin complicaciones', 12, 50, 8, 52),
(63, '2025-03-12', '2025-12-07', 'Refuerzo', 'Sin reacciones adversas', 3, 51, 4, 17),
(64, '2026-03-10', '2026-12-05', '1era', 'Paciente toleró bien la dosis', 8, 56, 8, 38),
(65, '2026-07-02', '2027-07-02', 'Refuerzo', '', 9, 2, 13, 24),
(66, '2025-04-22', '2025-10-19', '3era', 'Paciente toleró bien la dosis', 3, 56, 1, 37),
(67, '2025-12-06', '2026-09-02', '1era', 'Sin reacciones adversas', 1, 48, 5, 15),
(68, '2026-04-02', '2026-12-28', 'Refuerzo', 'Sin reacciones adversas', 8, 56, 12, 33),
(69, '2025-12-20', '2026-06-18', 'Refuerzo', '', 2, 51, 7, 59),
(70, '2025-04-29', '2026-04-29', '2da', 'Sin reacciones adversas', 6, 51, 6, 30),
(71, '2026-06-18', '2027-03-15', '3era', 'Paciente toleró bien la dosis', 8, 48, 8, 47),
(72, '2025-11-23', '2026-05-22', '3era', 'Se aplico sin complicaciones', 1, 48, 1, 55),
(73, '2026-09-15', '2026-09-30', '1er dosis', '', 1, 2, 10, 10);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `cita`
--

CREATE TABLE `cita` (
  `idCita` int(11) NOT NULL,
  `fecha` date NOT NULL,
  `hora` time NOT NULL,
  `estado` enum('AGENDADA','ATENDIDA','CANCELADA','DISPONIBLE','PENDIENTE') NOT NULL,
  `Veterinario_idUsuario` int(11) NOT NULL,
  `Servicio_idServicio` int(11) NOT NULL,
  `Veterinaria_idVeterinaria` int(11) NOT NULL,
  `idMascota` int(10) UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `cita`
--

INSERT INTO `cita` (`idCita`, `fecha`, `hora`, `estado`, `Veterinario_idUsuario`, `Servicio_idServicio`, `Veterinaria_idVeterinaria`, `idMascota`) VALUES
(1, '2026-06-10', '09:00:00', 'AGENDADA', 2, 1, 1, 1),
(2, '2026-09-16', '08:00:00', 'CANCELADA', 2, 1, 1, 1),
(3, '2026-06-02', '19:45:00', 'CANCELADA', 51, 1, 6, 47),
(4, '2026-06-26', '11:45:00', 'PENDIENTE', 49, 15, 5, 31),
(5, '2026-05-03', '15:45:00', 'ATENDIDA', 54, 11, 1, 45),
(6, '2026-08-31', '17:45:00', 'AGENDADA', 50, 14, 10, 21),
(7, '2026-01-19', '08:30:00', 'ATENDIDA', 53, 22, 8, 32),
(8, '2026-08-22', '06:15:00', 'AGENDADA', 48, 15, 9, 34),
(9, '2026-08-29', '18:30:00', 'ATENDIDA', 53, 21, 8, 5),
(10, '2026-02-11', '19:30:00', 'DISPONIBLE', 52, 20, 5, NULL),
(11, '2026-09-07', '19:00:00', 'CANCELADA', 55, 13, 7, 41),
(12, '2026-05-28', '09:00:00', 'ATENDIDA', 56, 3, 5, 41),
(13, '2026-03-27', '17:30:00', 'AGENDADA', 53, 4, 8, 29),
(14, '2026-01-29', '10:30:00', 'CANCELADA', 2, 2, 1, 51),
(15, '2026-07-30', '15:15:00', 'ATENDIDA', 52, 14, 5, 16),
(16, '2026-07-15', '15:15:00', 'AGENDADA', 49, 6, 5, 40),
(17, '2026-08-25', '16:30:00', 'ATENDIDA', 54, 19, 1, 15),
(18, '2026-08-27', '10:15:00', 'AGENDADA', 54, 9, 1, 58),
(19, '2026-06-03', '16:45:00', 'CANCELADA', 48, 15, 9, 38),
(20, '2026-07-16', '19:45:00', 'CANCELADA', 51, 15, 6, 13),
(21, '2026-07-03', '15:30:00', 'DISPONIBLE', 48, 8, 9, NULL),
(22, '2026-05-21', '06:00:00', 'DISPONIBLE', 51, 1, 6, NULL),
(23, '2026-04-28', '15:30:00', 'CANCELADA', 56, 16, 5, 50),
(24, '2026-05-09', '16:15:00', 'ATENDIDA', 50, 21, 10, 40),
(25, '2026-08-14', '06:30:00', 'AGENDADA', 48, 21, 9, 20),
(26, '2026-08-01', '08:15:00', 'CANCELADA', 49, 3, 5, 21),
(27, '2026-09-14', '10:15:00', 'CANCELADA', 49, 18, 5, 34),
(28, '2026-06-23', '18:00:00', 'CANCELADA', 51, 16, 6, 48),
(29, '2026-04-26', '19:45:00', 'ATENDIDA', 54, 3, 1, 49),
(30, '2026-07-22', '06:30:00', 'AGENDADA', 55, 12, 7, 51),
(31, '2026-07-08', '16:30:00', 'DISPONIBLE', 55, 4, 7, NULL),
(32, '2026-04-30', '13:00:00', 'CANCELADA', 56, 13, 5, 7),
(33, '2026-04-24', '16:00:00', 'CANCELADA', 56, 18, 5, 59),
(34, '2026-07-29', '07:15:00', 'CANCELADA', 54, 23, 1, 42),
(35, '2026-03-01', '07:15:00', 'CANCELADA', 2, 2, 1, 32),
(36, '2026-08-21', '11:45:00', 'DISPONIBLE', 55, 5, 7, NULL),
(37, '2026-02-20', '19:45:00', 'DISPONIBLE', 56, 5, 5, NULL),
(38, '2026-07-09', '09:45:00', 'CANCELADA', 56, 14, 5, 3),
(39, '2026-07-08', '14:30:00', 'CANCELADA', 54, 8, 1, 7),
(40, '2026-03-04', '19:45:00', 'CANCELADA', 2, 13, 1, 13),
(41, '2026-01-11', '06:30:00', 'ATENDIDA', 48, 22, 9, 42),
(42, '2026-02-05', '19:15:00', 'PENDIENTE', 50, 5, 10, 14),
(43, '2026-03-17', '18:00:00', 'ATENDIDA', 56, 7, 5, 22),
(44, '2026-05-09', '18:15:00', 'ATENDIDA', 51, 5, 6, 35),
(45, '2026-01-08', '11:15:00', 'AGENDADA', 48, 22, 9, 9),
(46, '2026-05-16', '06:15:00', 'AGENDADA', 56, 11, 5, 12),
(47, '2026-02-02', '13:45:00', 'AGENDADA', 53, 17, 8, 48),
(48, '2026-08-20', '14:15:00', 'PENDIENTE', 52, 17, 5, 7),
(49, '2026-08-23', '16:00:00', 'PENDIENTE', 56, 2, 5, 20),
(50, '2026-08-07', '16:00:00', 'DISPONIBLE', 2, 16, 1, NULL),
(51, '2026-02-07', '07:30:00', 'DISPONIBLE', 54, 23, 1, NULL),
(52, '2026-05-21', '15:30:00', 'AGENDADA', 56, 5, 5, 9),
(53, '2026-08-21', '14:45:00', 'PENDIENTE', 53, 20, 8, 19),
(54, '2026-04-21', '12:45:00', 'AGENDADA', 48, 23, 9, 55),
(55, '2026-08-21', '12:45:00', 'CANCELADA', 50, 14, 10, 53),
(56, '2026-06-10', '16:30:00', 'DISPONIBLE', 48, 11, 9, NULL),
(57, '2026-02-04', '07:00:00', 'DISPONIBLE', 52, 5, 5, NULL),
(58, '2026-07-10', '18:15:00', 'AGENDADA', 48, 14, 9, 48),
(59, '2026-06-18', '16:00:00', 'PENDIENTE', 55, 2, 7, 36),
(60, '2026-01-27', '10:30:00', 'DISPONIBLE', 53, 12, 8, NULL),
(61, '2026-04-19', '08:45:00', 'PENDIENTE', 52, 4, 5, 33),
(62, '2026-07-08', '07:30:00', 'CANCELADA', 50, 4, 10, 55),
(63, '2026-01-14', '15:30:00', 'DISPONIBLE', 56, 8, 5, NULL),
(64, '2026-06-08', '11:30:00', 'CANCELADA', 2, 6, 1, 45),
(65, '2026-07-25', '07:15:00', 'ATENDIDA', 2, 6, 1, 37),
(66, '2026-07-12', '12:45:00', 'PENDIENTE', 2, 3, 1, 14),
(67, '2026-06-16', '18:00:00', 'CANCELADA', 52, 6, 5, 20),
(68, '2026-01-26', '16:00:00', 'ATENDIDA', 2, 5, 1, 49),
(69, '2026-09-06', '15:45:00', 'DISPONIBLE', 51, 15, 6, NULL),
(70, '2026-02-28', '11:45:00', 'ATENDIDA', 53, 9, 8, 49),
(71, '2026-06-07', '06:15:00', 'PENDIENTE', 48, 10, 9, 32),
(72, '2026-04-15', '10:15:00', 'AGENDADA', 53, 20, 8, 1),
(73, '2026-03-03', '06:45:00', 'CANCELADA', 49, 9, 5, 21),
(74, '2026-04-28', '14:30:00', 'ATENDIDA', 53, 6, 8, 25),
(75, '2026-01-10', '13:00:00', 'AGENDADA', 48, 13, 9, 28),
(76, '2026-07-27', '17:45:00', 'DISPONIBLE', 52, 19, 5, NULL),
(77, '2026-01-11', '11:15:00', 'DISPONIBLE', 51, 4, 6, NULL),
(78, '2026-08-12', '19:00:00', 'CANCELADA', 56, 15, 5, 6),
(79, '2026-02-10', '12:30:00', 'PENDIENTE', 50, 14, 10, 26),
(80, '2026-03-28', '07:00:00', 'CANCELADA', 52, 8, 5, 50),
(81, '2026-06-28', '11:15:00', 'ATENDIDA', 55, 17, 7, 58),
(82, '2026-04-12', '08:15:00', 'ATENDIDA', 50, 4, 10, 17),
(83, '2026-08-26', '18:45:00', 'DISPONIBLE', 48, 6, 9, NULL),
(84, '2026-06-11', '08:45:00', 'PENDIENTE', 56, 21, 5, 21),
(85, '2026-06-05', '18:30:00', 'DISPONIBLE', 48, 16, 9, NULL),
(86, '2026-02-07', '10:45:00', 'CANCELADA', 56, 2, 5, 33),
(87, '2026-05-27', '07:00:00', 'AGENDADA', 54, 2, 1, 24),
(88, '2026-08-25', '15:00:00', 'PENDIENTE', 56, 20, 5, 25),
(89, '2026-09-01', '14:15:00', 'ATENDIDA', 54, 19, 1, 21),
(90, '2026-06-25', '17:00:00', 'AGENDADA', 2, 15, 1, 52),
(91, '2026-05-07', '17:45:00', 'ATENDIDA', 55, 21, 7, 3),
(92, '2026-03-23', '11:30:00', 'PENDIENTE', 54, 17, 1, 40),
(93, '2026-10-07', '17:30:00', 'DISPONIBLE', 54, 23, 1, NULL),
(94, '2026-11-21', '10:30:00', 'DISPONIBLE', 50, 1, 10, NULL),
(95, '2026-12-29', '08:30:00', 'DISPONIBLE', 52, 1, 3, NULL),
(96, '2026-09-26', '15:30:00', 'DISPONIBLE', 54, 1, 1, NULL),
(97, '2026-09-23', '14:30:00', 'DISPONIBLE', 50, 12, 2, NULL),
(98, '2026-11-24', '07:00:00', 'DISPONIBLE', 54, 5, 1, NULL),
(99, '2026-09-21', '15:30:00', 'DISPONIBLE', 53, 17, 8, NULL),
(100, '2026-11-01', '14:00:00', 'DISPONIBLE', 48, 5, 9, NULL),
(101, '2026-09-28', '17:00:00', 'DISPONIBLE', 56, 6, 3, NULL),
(102, '2026-11-16', '10:00:00', 'DISPONIBLE', 48, 1, 9, NULL),
(103, '2026-12-28', '17:30:00', 'DISPONIBLE', 50, 21, 10, NULL),
(104, '2026-11-22', '16:30:00', 'DISPONIBLE', 50, 1, 4, NULL),
(105, '2026-10-18', '09:30:00', 'DISPONIBLE', 50, 14, 3, NULL),
(106, '2026-12-18', '14:30:00', 'DISPONIBLE', 48, 1, 9, NULL),
(107, '2026-10-30', '11:00:00', 'DISPONIBLE', 48, 7, 9, NULL),
(108, '2026-11-05', '08:30:00', 'DISPONIBLE', 52, 20, 3, NULL),
(109, '2026-10-01', '11:30:00', 'DISPONIBLE', 49, 20, 3, NULL),
(110, '2026-11-29', '16:00:00', 'DISPONIBLE', 55, 13, 7, NULL),
(111, '2026-12-03', '14:30:00', 'DISPONIBLE', 56, 23, 3, NULL),
(112, '2026-12-13', '11:30:00', 'DISPONIBLE', 49, 5, 3, NULL),
(113, '2026-12-17', '10:30:00', 'DISPONIBLE', 50, 4, 10, NULL),
(114, '2026-10-28', '10:00:00', 'DISPONIBLE', 54, 14, 3, NULL),
(115, '2026-11-18', '07:00:00', 'DISPONIBLE', 54, 10, 4, NULL),
(116, '2026-10-21', '16:30:00', 'DISPONIBLE', 56, 15, 4, NULL),
(117, '2026-12-10', '17:00:00', 'DISPONIBLE', 50, 14, 10, NULL),
(118, '2026-10-14', '08:00:00', 'DISPONIBLE', 55, 1, 2, NULL),
(119, '2026-09-19', '11:30:00', 'DISPONIBLE', 49, 12, 2, NULL),
(120, '2026-12-20', '08:00:00', 'DISPONIBLE', 50, 21, 10, NULL),
(121, '2026-09-29', '17:00:00', 'DISPONIBLE', 2, 1, 1, NULL),
(122, '2026-10-15', '15:30:00', 'DISPONIBLE', 51, 23, 6, NULL),
(123, '2026-09-24', '14:30:00', 'DISPONIBLE', 48, 8, 9, NULL),
(124, '2026-12-11', '07:30:00', 'DISPONIBLE', 50, 1, 3, NULL),
(125, '2026-10-05', '15:00:00', 'DISPONIBLE', 50, 1, 10, NULL),
(126, '2026-12-24', '09:30:00', 'DISPONIBLE', 52, 7, 4, NULL),
(127, '2026-12-02', '09:00:00', 'DISPONIBLE', 52, 4, 5, NULL),
(128, '2026-10-27', '10:00:00', 'DISPONIBLE', 48, 7, 2, NULL),
(129, '2026-12-21', '14:30:00', 'DISPONIBLE', 48, 23, 9, NULL),
(130, '2026-10-20', '07:00:00', 'DISPONIBLE', 53, 2, 2, NULL),
(131, '2026-12-27', '15:00:00', 'DISPONIBLE', 48, 8, 9, NULL),
(132, '2026-11-23', '11:30:00', 'DISPONIBLE', 51, 1, 6, NULL),
(133, '2026-12-04', '14:30:00', 'DISPONIBLE', 48, 7, 9, NULL),
(134, '2026-11-30', '08:30:00', 'DISPONIBLE', 53, 15, 8, NULL),
(135, '2026-12-24', '15:00:00', 'DISPONIBLE', 49, 5, 3, NULL),
(136, '2026-12-30', '11:30:00', 'DISPONIBLE', 49, 4, 5, NULL),
(137, '2026-10-03', '09:30:00', 'DISPONIBLE', 56, 7, 4, NULL),
(138, '2026-10-03', '10:30:00', 'DISPONIBLE', 48, 5, 9, NULL),
(139, '2026-11-18', '17:00:00', 'DISPONIBLE', 48, 8, 9, NULL),
(140, '2026-12-04', '10:00:00', 'DISPONIBLE', 52, 7, 2, NULL),
(141, '2026-12-30', '17:00:00', 'DISPONIBLE', 52, 8, 4, NULL),
(142, '2026-09-24', '15:00:00', 'DISPONIBLE', 52, 1, 5, NULL),
(143, '2026-09-29', '14:00:00', 'DISPONIBLE', 51, 14, 3, NULL),
(144, '2026-11-18', '10:30:00', 'DISPONIBLE', 49, 2, 2, NULL),
(145, '2026-12-01', '16:30:00', 'DISPONIBLE', 55, 5, 7, NULL),
(146, '2026-09-18', '07:30:00', 'DISPONIBLE', 2, 6, 3, NULL),
(147, '2026-10-25', '07:00:00', 'DISPONIBLE', 49, 23, 3, NULL),
(148, '2026-10-09', '15:00:00', 'DISPONIBLE', 50, 21, 10, NULL),
(149, '2026-11-13', '14:30:00', 'DISPONIBLE', 48, 5, 9, NULL),
(150, '2026-10-07', '10:30:00', 'DISPONIBLE', 50, 1, 10, NULL),
(151, '2026-11-03', '09:30:00', 'DISPONIBLE', 56, 13, 5, NULL),
(152, '2026-10-10', '16:30:00', 'DISPONIBLE', 51, 2, 2, NULL),
(153, '2026-10-19', '15:30:00', 'DISPONIBLE', 54, 1, 1, NULL),
(154, '2026-12-30', '17:30:00', 'DISPONIBLE', 48, 23, 9, NULL),
(155, '2026-12-17', '11:30:00', 'DISPONIBLE', 48, 5, 9, NULL),
(156, '2026-12-04', '14:00:00', 'DISPONIBLE', 55, 13, 7, NULL),
(157, '2026-12-18', '09:30:00', 'DISPONIBLE', 2, 2, 2, NULL),
(158, '2026-10-23', '15:00:00', 'DISPONIBLE', 54, 6, 3, NULL),
(159, '2026-11-05', '07:30:00', 'DISPONIBLE', 50, 23, 10, NULL),
(160, '2026-09-24', '11:00:00', 'DISPONIBLE', 48, 8, 9, NULL),
(161, '2026-11-09', '07:00:00', 'DISPONIBLE', 55, 7, 7, NULL),
(162, '2026-10-04', '10:30:00', 'DISPONIBLE', 49, 1, 4, NULL),
(163, '2026-11-17', '14:00:00', 'DISPONIBLE', 55, 7, 4, NULL),
(164, '2026-11-28', '07:00:00', 'DISPONIBLE', 55, 14, 7, NULL),
(165, '2026-10-19', '10:00:00', 'DISPONIBLE', 49, 13, 5, NULL),
(166, '2026-10-22', '09:00:00', 'DISPONIBLE', 49, 12, 2, NULL),
(167, '2026-12-05', '14:00:00', 'DISPONIBLE', 56, 6, 3, NULL),
(168, '2026-12-14', '16:00:00', 'DISPONIBLE', 49, 7, 2, NULL),
(169, '2026-11-06', '07:30:00', 'DISPONIBLE', 49, 6, 5, NULL),
(170, '2026-10-21', '08:30:00', 'DISPONIBLE', 53, 15, 8, NULL),
(171, '2026-11-11', '07:30:00', 'DISPONIBLE', 50, 5, 3, NULL),
(172, '2026-11-11', '08:00:00', 'DISPONIBLE', 50, 21, 10, NULL),
(173, '2026-11-06', '16:00:00', 'DISPONIBLE', 53, 7, 4, NULL),
(174, '2026-10-12', '14:30:00', 'DISPONIBLE', 51, 17, 6, NULL),
(175, '2026-10-05', '14:30:00', 'DISPONIBLE', 52, 22, 5, NULL),
(176, '2026-12-04', '07:30:00', 'DISPONIBLE', 56, 18, 5, NULL),
(177, '2026-09-23', '11:00:00', 'DISPONIBLE', 52, 5, 3, NULL),
(178, '2026-11-21', '17:00:00', 'DISPONIBLE', 54, 15, 4, NULL),
(179, '2026-12-09', '10:30:00', 'DISPONIBLE', 54, 1, 3, NULL),
(180, '2026-11-05', '11:00:00', 'DISPONIBLE', 55, 5, 7, NULL),
(181, '2026-10-28', '15:00:00', 'DISPONIBLE', 53, 23, 8, NULL),
(182, '2026-12-11', '17:00:00', 'DISPONIBLE', 49, 6, 5, NULL),
(183, '2026-10-07', '14:30:00', 'DISPONIBLE', 48, 2, 9, NULL),
(184, '2026-10-02', '14:00:00', 'DISPONIBLE', 50, 21, 10, NULL),
(185, '2026-11-25', '15:30:00', 'DISPONIBLE', 50, 21, 10, NULL),
(186, '2026-10-09', '16:00:00', 'DISPONIBLE', 51, 2, 6, NULL),
(187, '2026-10-20', '07:30:00', 'DISPONIBLE', 51, 14, 6, NULL),
(188, '2026-12-16', '10:00:00', 'DISPONIBLE', 54, 7, 2, NULL),
(189, '2026-11-05', '08:00:00', 'DISPONIBLE', 50, 23, 10, NULL),
(190, '2026-12-27', '11:30:00', 'DISPONIBLE', 53, 15, 8, NULL),
(191, '2026-12-05', '16:00:00', 'DISPONIBLE', 53, 15, 8, NULL),
(192, '2026-09-29', '10:00:00', 'DISPONIBLE', 48, 2, 2, NULL),
(193, '2026-11-13', '07:00:00', 'DISPONIBLE', 55, 18, 7, NULL),
(194, '2026-10-27', '09:30:00', 'DISPONIBLE', 56, 22, 5, NULL),
(195, '2026-10-22', '14:00:00', 'DISPONIBLE', 48, 2, 9, NULL),
(196, '2026-09-29', '15:30:00', 'DISPONIBLE', 48, 7, 9, NULL),
(197, '2026-09-30', '15:00:00', 'DISPONIBLE', 49, 15, 4, NULL),
(198, '2026-09-21', '07:00:00', 'DISPONIBLE', 55, 5, 7, NULL),
(199, '2026-12-28', '14:00:00', 'DISPONIBLE', 55, 1, 3, NULL),
(200, '2026-12-08', '15:30:00', 'DISPONIBLE', 49, 6, 5, NULL),
(201, '2026-10-02', '17:30:00', 'DISPONIBLE', 51, 17, 6, NULL),
(202, '2026-11-10', '07:30:00', 'DISPONIBLE', 52, 18, 5, NULL),
(203, '2026-09-15', '16:00:00', 'DISPONIBLE', 55, 5, 7, NULL),
(204, '2026-12-09', '11:00:00', 'DISPONIBLE', 48, 5, 9, NULL),
(205, '2026-10-21', '16:30:00', 'DISPONIBLE', 55, 15, 4, NULL),
(206, '2026-10-26', '07:30:00', 'DISPONIBLE', 51, 9, 6, NULL),
(207, '2026-11-02', '15:00:00', 'DISPONIBLE', 54, 1, 4, NULL),
(208, '2026-11-24', '08:30:00', 'DISPONIBLE', 51, 23, 6, NULL),
(209, '2026-12-26', '15:00:00', 'DISPONIBLE', 2, 23, 1, NULL),
(210, '2026-11-07', '07:30:00', 'DISPONIBLE', 2, 1, 1, NULL),
(211, '2026-10-03', '09:00:00', 'DISPONIBLE', 48, 1, 9, NULL),
(212, '2026-10-08', '10:00:00', 'DISPONIBLE', 55, 1, 7, NULL),
(213, '2026-10-23', '07:00:00', 'DISPONIBLE', 53, 7, 8, NULL),
(214, '2026-10-12', '08:30:00', 'DISPONIBLE', 50, 23, 10, NULL),
(215, '2026-10-27', '08:00:00', 'DISPONIBLE', 51, 23, 6, NULL),
(216, '2026-12-11', '10:30:00', 'DISPONIBLE', 2, 23, 1, NULL),
(217, '2026-10-23', '11:30:00', 'DISPONIBLE', 55, 12, 2, NULL),
(218, '2026-09-17', '14:00:00', 'DISPONIBLE', 2, 5, 1, NULL),
(219, '2026-11-06', '16:00:00', 'DISPONIBLE', 53, 2, 2, NULL),
(220, '2026-12-18', '10:30:00', 'DISPONIBLE', 48, 5, 9, NULL),
(221, '2026-12-06', '09:30:00', 'DISPONIBLE', 54, 6, 3, NULL),
(222, '2026-11-10', '09:00:00', 'DISPONIBLE', 48, 5, 9, NULL),
(223, '2026-11-01', '14:30:00', 'DISPONIBLE', 48, 2, 9, NULL),
(224, '2026-09-19', '11:30:00', 'DISPONIBLE', 55, 18, 7, NULL),
(225, '2026-12-29', '09:00:00', 'DISPONIBLE', 49, 12, 2, NULL),
(226, '2026-12-20', '08:30:00', 'DISPONIBLE', 48, 1, 9, NULL),
(227, '2026-11-15', '07:30:00', 'DISPONIBLE', 51, 14, 6, NULL),
(228, '2026-11-06', '09:00:00', 'DISPONIBLE', 50, 23, 10, NULL),
(229, '2026-12-29', '14:30:00', 'DISPONIBLE', 52, 13, 5, NULL),
(230, '2026-09-26', '16:00:00', 'DISPONIBLE', 52, 1, 2, NULL),
(231, '2026-10-20', '17:30:00', 'DISPONIBLE', 50, 1, 10, NULL),
(232, '2026-12-12', '11:30:00', 'DISPONIBLE', 53, 1, 8, NULL),
(233, '2026-10-02', '07:30:00', 'AGENDADA', 50, 23, 10, 47),
(234, '2026-09-20', '08:30:00', 'AGENDADA', 50, 4, 10, 35),
(235, '2026-10-03', '10:30:00', 'AGENDADA', 49, 4, 5, 4),
(236, '2026-09-24', '16:30:00', 'PENDIENTE', 55, 7, 2, 57),
(237, '2026-12-24', '09:30:00', 'AGENDADA', 2, 5, 1, 10),
(238, '2026-09-15', '14:00:00', 'PENDIENTE', 51, 14, 6, 3),
(239, '2026-10-11', '07:00:00', 'PENDIENTE', 48, 2, 9, 45),
(240, '2026-11-15', '09:00:00', 'PENDIENTE', 49, 22, 5, 61),
(241, '2026-11-25', '07:30:00', 'AGENDADA', 55, 18, 7, 8),
(242, '2026-12-02', '17:30:00', 'PENDIENTE', 56, 2, 2, 4),
(243, '2026-10-18', '10:30:00', 'PENDIENTE', 53, 23, 8, 2),
(244, '2026-12-09', '08:30:00', 'AGENDADA', 50, 12, 2, 5),
(245, '2026-09-26', '14:00:00', 'AGENDADA', 53, 17, 8, 6),
(246, '2026-11-15', '17:00:00', 'AGENDADA', 48, 5, 9, 21),
(247, '2026-11-12', '15:30:00', 'PENDIENTE', 49, 1, 3, 13),
(248, '2026-10-02', '14:30:00', 'AGENDADA', 51, 1, 6, 41),
(249, '2026-11-08', '15:30:00', 'AGENDADA', 50, 4, 10, 45),
(250, '2026-12-10', '15:00:00', 'PENDIENTE', 50, 7, 4, 25),
(251, '2026-10-18', '11:30:00', 'AGENDADA', 54, 12, 2, 8),
(252, '2026-12-22', '14:00:00', 'PENDIENTE', 51, 1, 6, 35),
(253, '2026-12-15', '15:30:00', 'AGENDADA', 55, 18, 7, 49),
(254, '2026-11-30', '15:30:00', 'AGENDADA', 55, 13, 7, 60),
(255, '2026-09-19', '15:30:00', 'AGENDADA', 55, 1, 7, 11),
(256, '2026-09-28', '14:00:00', 'AGENDADA', 55, 13, 7, 30),
(257, '2026-09-18', '09:30:00', 'PENDIENTE', 53, 16, 8, 50),
(258, '2026-10-15', '08:00:00', 'AGENDADA', 53, 2, 2, 16),
(259, '2026-11-09', '17:30:00', 'PENDIENTE', 51, 23, 6, 54),
(260, '2026-12-14', '08:00:00', 'AGENDADA', 51, 17, 6, 24),
(261, '2026-09-28', '11:00:00', 'PENDIENTE', 50, 23, 3, 11),
(262, '2026-10-05', '09:00:00', 'AGENDADA', 55, 5, 3, 34),
(263, '2026-12-19', '10:30:00', 'PENDIENTE', 2, 18, 1, 35),
(264, '2026-11-23', '09:30:00', 'PENDIENTE', 55, 7, 7, 37),
(265, '2026-10-10', '09:00:00', 'AGENDADA', 53, 12, 4, 50),
(266, '2026-10-03', '09:00:00', 'AGENDADA', 51, 2, 2, 53),
(267, '2026-10-05', '16:30:00', 'AGENDADA', 50, 21, 10, 26),
(268, '2026-12-04', '09:00:00', 'AGENDADA', 56, 1, 5, 29),
(269, '2026-12-31', '17:30:00', 'AGENDADA', 50, 7, 2, 33),
(270, '2026-11-13', '07:30:00', 'AGENDADA', 51, 7, 6, 20),
(271, '2026-12-24', '15:00:00', 'AGENDADA', 50, 10, 4, 37),
(272, '2026-10-31', '09:00:00', 'AGENDADA', 55, 6, 3, 10),
(273, '2026-10-11', '15:30:00', 'AGENDADA', 51, 17, 6, 3),
(274, '2026-12-25', '08:00:00', 'PENDIENTE', 49, 13, 5, 8),
(275, '2026-10-29', '14:00:00', 'AGENDADA', 51, 12, 6, 49),
(276, '2026-10-21', '11:00:00', 'PENDIENTE', 48, 2, 9, 48),
(277, '2026-11-27', '17:00:00', 'PENDIENTE', 51, 7, 6, 1),
(278, '2026-12-09', '15:30:00', 'AGENDADA', 54, 12, 2, 51),
(279, '2026-12-06', '14:00:00', 'AGENDADA', 55, 18, 7, 13),
(280, '2026-10-20', '15:00:00', 'AGENDADA', 52, 1, 5, 5),
(281, '2026-12-18', '14:30:00', 'AGENDADA', 51, 2, 6, 59),
(282, '2026-12-28', '11:30:00', 'AGENDADA', 51, 7, 6, 21),
(283, '2026-10-28', '10:00:00', 'AGENDADA', 54, 5, 1, 24),
(284, '2026-10-07', '15:30:00', 'PENDIENTE', 48, 23, 9, 57),
(285, '2026-10-01', '07:30:00', 'AGENDADA', 50, 4, 10, 22),
(286, '2026-11-25', '15:30:00', 'PENDIENTE', 52, 9, 5, 37),
(287, '2026-10-03', '16:30:00', 'AGENDADA', 53, 15, 8, 8),
(288, '2026-10-25', '09:30:00', 'AGENDADA', 51, 7, 6, 28),
(289, '2026-10-07', '16:00:00', 'AGENDADA', 50, 23, 10, 55),
(290, '2026-11-03', '08:30:00', 'AGENDADA', 53, 22, 8, 53),
(291, '2026-10-28', '08:00:00', 'AGENDADA', 50, 23, 10, 51),
(292, '2026-10-11', '14:30:00', 'PENDIENTE', 54, 1, 4, 48),
(293, '2026-12-16', '11:30:00', 'AGENDADA', 50, 4, 10, 10),
(294, '2026-11-01', '17:30:00', 'AGENDADA', 53, 1, 8, 49),
(295, '2026-12-15', '07:00:00', 'PENDIENTE', 53, 8, 3, 26),
(296, '2026-12-01', '16:00:00', 'PENDIENTE', 55, 7, 7, 26),
(297, '2026-11-22', '17:30:00', 'PENDIENTE', 55, 7, 7, 43),
(298, '2026-09-19', '15:00:00', 'AGENDADA', 56, 6, 5, 52),
(299, '2026-10-08', '15:00:00', 'AGENDADA', 51, 14, 6, 13),
(300, '2026-11-07', '07:30:00', 'AGENDADA', 51, 12, 6, 43),
(301, '2026-12-19', '09:00:00', 'PENDIENTE', 50, 4, 10, 45),
(302, '2026-11-13', '10:00:00', 'AGENDADA', 53, 1, 4, 7),
(303, '2026-06-16', '16:30:00', 'ATENDIDA', 50, 7, 4, 29),
(304, '2026-03-06', '10:00:00', 'ATENDIDA', 49, 1, 5, 58),
(305, '2026-06-27', '17:30:00', 'CANCELADA', 2, 1, 1, 27),
(306, '2026-03-09', '15:30:00', 'ATENDIDA', 53, 22, 8, 45),
(307, '2026-08-06', '08:00:00', 'ATENDIDA', 53, 16, 8, 29),
(308, '2026-06-19', '07:30:00', 'ATENDIDA', 55, 5, 7, 55),
(309, '2026-08-10', '11:30:00', 'ATENDIDA', 51, 1, 6, 28),
(310, '2026-08-23', '07:00:00', 'CANCELADA', 52, 2, 2, 15),
(311, '2026-06-20', '08:30:00', 'ATENDIDA', 50, 23, 10, 60),
(312, '2026-05-21', '14:30:00', 'CANCELADA', 53, 17, 8, 23),
(313, '2026-02-20', '16:30:00', 'ATENDIDA', 48, 5, 9, 40),
(314, '2026-01-17', '08:00:00', 'ATENDIDA', 55, 2, 2, 47),
(315, '2026-03-18', '14:00:00', 'ATENDIDA', 49, 14, 3, 58),
(316, '2026-05-09', '10:00:00', 'ATENDIDA', 2, 18, 1, 32),
(317, '2026-02-28', '15:30:00', 'ATENDIDA', 51, 9, 6, 56),
(318, '2026-06-04', '15:00:00', 'ATENDIDA', 53, 16, 8, 29),
(319, '2026-02-12', '07:30:00', 'ATENDIDA', 50, 14, 10, 46),
(320, '2026-06-06', '07:00:00', 'CANCELADA', 52, 8, 3, 8),
(321, '2026-03-24', '14:00:00', 'ATENDIDA', 51, 12, 2, 30),
(322, '2026-01-13', '17:00:00', 'ATENDIDA', 51, 1, 6, 9),
(323, '2026-05-13', '11:00:00', 'ATENDIDA', 49, 22, 5, 47),
(324, '2026-06-07', '09:00:00', 'ATENDIDA', 48, 5, 9, 42),
(325, '2026-03-08', '11:30:00', 'CANCELADA', 50, 4, 10, 6),
(326, '2026-05-26', '10:00:00', 'ATENDIDA', 48, 2, 2, 41),
(327, '2026-04-18', '14:00:00', 'CANCELADA', 48, 5, 9, 10),
(328, '2026-07-04', '10:00:00', 'ATENDIDA', 52, 7, 2, 31),
(329, '2026-02-09', '14:00:00', 'ATENDIDA', 55, 1, 7, 11),
(330, '2026-09-14', '14:00:00', 'ATENDIDA', 55, 18, 7, 24),
(331, '2026-09-07', '08:30:00', 'CANCELADA', 54, 23, 1, 51),
(332, '2026-07-05', '16:30:00', 'ATENDIDA', 51, 14, 3, 33),
(333, '2026-07-20', '09:00:00', 'CANCELADA', 2, 5, 1, 17),
(334, '2026-06-09', '10:00:00', 'ATENDIDA', 48, 7, 9, 5),
(335, '2026-04-25', '09:30:00', 'ATENDIDA', 52, 4, 5, 41),
(336, '2026-09-09', '10:00:00', 'ATENDIDA', 53, 22, 8, 50),
(337, '2026-03-23', '08:00:00', 'ATENDIDA', 52, 13, 5, 40),
(338, '2026-08-09', '08:30:00', 'CANCELADA', 52, 6, 5, 28),
(339, '2026-01-01', '16:00:00', 'ATENDIDA', 51, 9, 6, 56),
(340, '2026-05-15', '11:30:00', 'ATENDIDA', 52, 18, 5, 55),
(341, '2026-07-12', '08:00:00', 'ATENDIDA', 53, 16, 8, 4),
(342, '2026-02-23', '10:00:00', 'ATENDIDA', 49, 4, 5, 1),
(343, '2026-01-14', '09:00:00', 'CANCELADA', 51, 9, 6, 39),
(344, '2026-07-24', '14:30:00', 'ATENDIDA', 48, 6, 3, 31),
(345, '2026-06-24', '07:00:00', 'ATENDIDA', 48, 2, 2, 56),
(346, '2026-06-24', '17:00:00', 'ATENDIDA', 48, 10, 4, 39),
(347, '2026-08-08', '14:30:00', 'ATENDIDA', 50, 23, 10, 34),
(348, '2026-06-05', '10:30:00', 'ATENDIDA', 53, 7, 8, 36),
(349, '2026-01-20', '08:00:00', 'ATENDIDA', 2, 18, 1, 7),
(350, '2026-04-10', '08:00:00', 'ATENDIDA', 55, 18, 7, 38),
(351, '2026-01-09', '15:30:00', 'CANCELADA', 49, 4, 5, 54),
(352, '2026-02-17', '07:00:00', 'ATENDIDA', 48, 7, 9, 56),
(353, '2026-09-10', '10:30:00', 'ATENDIDA', 2, 5, 1, 41),
(354, '2026-08-31', '08:00:00', 'ATENDIDA', 51, 12, 6, 58),
(355, '2026-02-11', '10:30:00', 'ATENDIDA', 51, 12, 6, 53),
(356, '2026-05-18', '09:30:00', 'ATENDIDA', 49, 7, 2, 39),
(357, '2026-03-10', '16:00:00', 'ATENDIDA', 52, 22, 5, 47),
(358, '2026-01-06', '15:00:00', 'ATENDIDA', 55, 1, 7, 32),
(359, '2026-03-05', '16:00:00', 'ATENDIDA', 2, 18, 1, 51),
(360, '2026-03-07', '15:30:00', 'CANCELADA', 53, 8, 3, 5),
(361, '2026-06-16', '11:00:00', 'ATENDIDA', 51, 9, 6, 9),
(362, '2026-02-21', '14:30:00', 'ATENDIDA', 54, 23, 1, 49),
(363, '2026-09-16', '08:00:00', 'DISPONIBLE', 2, 1, 1, NULL),
(364, '2026-09-16', '09:00:00', 'AGENDADA', 54, 1, 1, 2),
(365, '2026-09-16', '10:00:00', 'DISPONIBLE', 2, 1, 1, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ciudad`
--

CREATE TABLE `ciudad` (
  `idCiudad` int(11) NOT NULL,
  `nombre` varchar(45) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `ciudad`
--

INSERT INTO `ciudad` (`idCiudad`, `nombre`) VALUES
(4, 'Barranquilla'),
(1, 'Bogota'),
(3, 'Cali'),
(5, 'Cartagena'),
(2, 'Medellín');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `conversacion`
--

CREATE TABLE `conversacion` (
  `idConversacion` int(11) NOT NULL,
  `creadaEn` datetime(6) NOT NULL,
  `usuarioA` int(11) NOT NULL,
  `usuarioB` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `conversacion`
--

INSERT INTO `conversacion` (`idConversacion`, `creadaEn`, `usuarioA`, `usuarioB`) VALUES
(1, '2026-09-15 01:53:01.000000', 1, 2),
(2, '2026-02-06 19:14:03.000000', 41, 39),
(3, '2026-07-04 11:31:20.000000', 57, 1),
(4, '2026-08-04 13:18:15.000000', 9, 25),
(5, '2026-03-30 17:08:56.000000', 35, 44),
(6, '2026-01-09 21:23:06.000000', 50, 5),
(7, '2026-05-15 07:51:17.000000', 5, 1),
(8, '2026-01-31 14:29:23.000000', 3, 60),
(9, '2026-09-14 15:07:31.000000', 33, 19),
(10, '2026-07-25 03:00:03.000000', 28, 45),
(11, '2026-03-20 10:06:08.000000', 41, 35),
(12, '2026-02-10 18:36:57.000000', 6, 20),
(13, '2026-03-21 19:31:45.000000', 59, 14),
(14, '2026-02-03 11:17:34.000000', 21, 26),
(15, '2026-05-03 02:06:29.000000', 47, 55),
(16, '2026-08-11 04:13:22.000000', 16, 6),
(17, '2026-01-29 11:15:39.000000', 30, 40),
(18, '2026-03-31 04:20:55.000000', 43, 48),
(19, '2026-10-19 16:20:10.000000', 3, 7);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `correo`
--

CREATE TABLE `correo` (
  `idCorreo` int(11) NOT NULL,
  `idUsuario` int(11) NOT NULL,
  `correo` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `correo`
--

INSERT INTO `correo` (`idCorreo`, `idUsuario`, `correo`) VALUES
(1, 1, 'maria@gmail.com'),
(2, 2, 'juan@gmail.com'),
(3, 3, 'laura@gmail.com'),
(4, 4, 'carlos@gmail.com'),
(5, 5, 'mafesdy_12@outlook.com'),
(6, 6, 'rT@gmail.com'),
(7, 7, 'm@gmail.com'),
(8, 8, 'A@gmail.com'),
(9, 9, 'armando420@gmail.com'),
(10, 10, 'SL@gmail.com'),
(11, 11, 'sara@gmail.com'),
(12, 12, 'juan123@gmail.com'),
(13, 13, 'jose123@gmail.com'),
(14, 14, 'puyol@gmail.com'),
(15, 15, 'lola@gmail.com'),
(16, 16, 'jorge.rodríguez16@hotmail.com'),
(17, 17, 'esperanza.torres17@hotmail.com'),
(18, 18, 'felipe.buitrago18@yahoo.com'),
(19, 19, 'stiven.garcía19@outlook.com'),
(20, 20, 'rubiela.gómez20@hotmail.com'),
(21, 21, 'marina.mora21@outlook.com'),
(22, 22, 'maría.ramírez22@hotmail.com'),
(23, 23, 'jesús.vásquez23@gmail.com'),
(24, 24, 'marina.fernández24@gmail.com'),
(25, 25, 'edwin.rodríguez25@outlook.com'),
(26, 26, 'mario.benavides26@yahoo.com'),
(27, 27, 'ruth.sánchez27@yahoo.com'),
(28, 28, 'rené.quintero28@outlook.com'),
(29, 29, 'andrea.lópez29@hotmail.com'),
(30, 30, 'edgar.agudelo30@gmail.com'),
(31, 31, 'mario.ariza31@outlook.com'),
(32, 32, 'andrés.cardozo32@gmail.com'),
(33, 33, 'myriam.caicedo33@yahoo.com'),
(34, 34, 'álvaro.beltrán34@hotmail.com'),
(35, 35, 'nelson.escobar35@hotmail.com'),
(36, 36, 'diego.gómez36@gmail.com'),
(37, 37, 'ángela.gutiérrez37@hotmail.com'),
(38, 38, 'ana.moreno38@yahoo.com'),
(39, 39, 'diana.castro39@outlook.com'),
(40, 40, 'fernando.morales40@hotmail.com'),
(41, 41, 'eliana.rojas41@outlook.com'),
(42, 42, 'mónica.cuesta42@gmail.com'),
(43, 43, 'jesús.león43@hotmail.com'),
(44, 44, 'carolina.padilla44@yahoo.com'),
(45, 45, 'claudia.rivera45@hotmail.com'),
(46, 46, 'edisson.bedoya46@hotmail.com'),
(47, 47, 'luis.duque47@outlook.com'),
(48, 48, 'héctor.guevara48@yahoo.com'),
(49, 49, 'ángela.gómez49@hotmail.com'),
(50, 50, 'valentina.ortiz50@gmail.com'),
(51, 51, 'laura.valderrama51@gmail.com'),
(52, 52, 'germán.álvarez52@yahoo.com'),
(53, 53, 'jesús.medina53@yahoo.com'),
(54, 54, 'néstor.cruz54@outlook.com'),
(55, 55, 'mónica.vargas55@gmail.com'),
(56, 56, 'luis.muñoz56@outlook.com'),
(57, 57, 'juan.burbano57@gmail.com'),
(58, 58, 'pilar.torres58@hotmail.com'),
(59, 59, 'leider.palacios59@outlook.com'),
(60, 60, 'andrea.gómez60@gmail.com'),
(61, 61, 'sandra.pineda61@hotmail.com');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `correo_veterinaria`
--

CREATE TABLE `correo_veterinaria` (
  `idCorreo_Veterinaria` int(11) NOT NULL,
  `idVeterinaria` int(11) NOT NULL,
  `correo` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `correo_veterinaria`
--

INSERT INTO `correo_veterinaria` (`idCorreo_Veterinaria`, `idVeterinaria`, `correo`) VALUES
(1, 1, 'contacto@vetlink.com'),
(2, 2, 'vet@gmail.com'),
(3, 4, 'vetplus@gmail.com'),
(4, 5, 'jose@jose.com'),
(5, 6, 'lola@lolita.com'),
(6, 3, 'contacto@puppy.com'),
(7, 7, 'contacto@huellitasfelices.com'),
(8, 8, 'contacto@petcarebogota.com'),
(9, 9, 'contacto@vetsanfrancisco.com'),
(10, 10, 'contacto@animalvetexpress.com');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `dia_semana`
--

CREATE TABLE `dia_semana` (
  `idDiaSemana` int(11) NOT NULL,
  `diaSemana` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `dia_semana`
--

INSERT INTO `dia_semana` (`idDiaSemana`, `diaSemana`) VALUES
(4, 'Jueves'),
(1, 'Lunes'),
(2, 'Martes'),
(3, 'Miercoles'),
(6, 'Sabado'),
(5, 'Viernes');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `emergencia`
--

CREATE TABLE `emergencia` (
  `idEmergencia` int(11) NOT NULL,
  `creadaEn` datetime(6) NOT NULL,
  `detalle` text DEFAULT NULL,
  `estado` enum('ASIGNADA','ATENDIDA','CANCELADA','PENDIENTE') NOT NULL,
  `motivo` varchar(255) NOT NULL,
  `idMascota` int(11) DEFAULT NULL,
  `idVeterinaria` int(11) DEFAULT NULL,
  `idVeterinario` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `emergencia`
--

INSERT INTO `emergencia` (`idEmergencia`, `creadaEn`, `detalle`, `estado`, `motivo`, `idMascota`, `idVeterinaria`, `idVeterinario`) VALUES
(1, '2026-09-15 01:53:35.000000', 'no rspira', 'ATENDIDA', 'morir', 1, 1, 2),
(2, '2026-02-25 05:59:02.000000', 'Requirio atencion inmediata', 'ATENDIDA', 'Parto complicado', 11, 6, 55),
(3, '2026-08-07 16:49:22.000000', 'Requirio atencion inmediata', 'PENDIENTE', 'Convulsiones', 12, 7, 53),
(4, '2026-01-15 22:21:32.000000', 'Requirio atencion inmediata', 'ASIGNADA', 'Golpe de calor', 25, 1, 2),
(5, '2026-04-16 10:08:03.000000', 'Llego en estado critico', 'ASIGNADA', 'Parto complicado', 12, 6, 52),
(6, '2026-04-11 16:39:32.000000', 'Llego en estado critico', 'CANCELADA', 'Reaccion alergica grave', 34, 6, 56),
(7, '2026-07-18 22:15:25.000000', 'Se traslado a la sala de urgencias', 'ASIGNADA', 'Vomito severo', 23, 8, 56),
(8, '2026-03-30 10:49:46.000000', 'Necesito hospitalizacion', 'ASIGNADA', 'Convulsiones', 21, 2, 50),
(9, '2026-06-06 04:15:28.000000', 'Requirio atencion inmediata', 'CANCELADA', 'Convulsiones', 33, 7, 53),
(10, '2026-03-12 20:55:40.000000', 'Se traslado a la sala de urgencias', 'PENDIENTE', 'Convulsiones', 32, 9, 49),
(11, '2026-03-27 17:49:38.000000', 'Estable tras primeros auxilios', 'ATENDIDA', 'Convulsiones', 21, 8, 56),
(12, '2026-01-28 13:04:19.000000', 'Llego en estado critico', 'PENDIENTE', 'Convulsiones', 13, 7, 55),
(13, '2026-09-11 14:53:46.000000', 'Se traslado a la sala de urgencias', 'PENDIENTE', 'Reaccion alergica grave', 32, 3, 48),
(14, '2026-01-16 08:01:45.000000', 'Estable tras primeros auxilios', 'ASIGNADA', 'Hemorragia', 15, 9, 51),
(15, '2026-03-28 16:39:02.000000', 'Necesito hospitalizacion', 'PENDIENTE', 'Parto complicado', 33, 2, 56),
(16, '2026-02-28 09:45:12.000000', 'Necesito hospitalizacion', 'CANCELADA', 'Parto complicado', 53, 1, 54),
(17, '2026-04-22 15:28:45.000000', 'Llego en estado critico', 'ATENDIDA', 'Hemorragia', 56, 1, 54),
(18, '2026-05-15 12:41:28.000000', 'Llego en estado critico', 'PENDIENTE', 'Dificultad respiratoria', 50, 4, 50),
(19, '2026-06-29 22:13:08.000000', 'Se traslado a la sala de urgencias', 'ATENDIDA', 'Convulsiones', 51, 4, 50),
(20, '2026-09-15 18:28:39.000000', 'tienen vomito', 'PENDIENTE', 'dificultad respiratoria', 1, 1, NULL);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `especialidad_veterinario`
--

CREATE TABLE `especialidad_veterinario` (
  `idEspecialidadVeterinario` int(11) NOT NULL,
  `nombreEspecialidad` varchar(45) NOT NULL,
  `descripcion` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `especialidad_veterinario`
--

INSERT INTO `especialidad_veterinario` (`idEspecialidadVeterinario`, `nombreEspecialidad`, `descripcion`) VALUES
(1, 'Medicina General', 'Consulta general'),
(2, 'Cirugia', 'Procedimientos quirurgicos'),
(3, 'Dermatologia', 'Diagnostico y tratamiento de piel'),
(4, 'Cardiologia', 'Enfermedades del corazon'),
(5, 'Odontologia', 'Salud dental animal'),
(6, 'Neurologia', 'Trastornos del sistema nervioso'),
(7, 'Oftalmologia', 'Salud visual animal');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `especie`
--

CREATE TABLE `especie` (
  `idEspecie` int(11) NOT NULL,
  `nombreEspecie` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `especie`
--

INSERT INTO `especie` (`idEspecie`, `nombreEspecie`) VALUES
(1, 'Perro'),
(2, 'Gato'),
(3, 'Ave'),
(4, 'Conejo'),
(5, 'Hámster'),
(6, 'Cobayo'),
(7, 'Pez'),
(8, 'Tortuga'),
(9, 'Iguana'),
(10, 'Hurón'),
(11, 'Serpiente'),
(12, 'Erizo');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `fabricante_vacuna`
--

CREATE TABLE `fabricante_vacuna` (
  `idFabricante_Vacuna` int(11) NOT NULL,
  `nombre` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `fabricante_vacuna`
--

INSERT INTO `fabricante_vacuna` (`idFabricante_Vacuna`, `nombre`) VALUES
(1, 'Zoetis'),
(2, 'MSD'),
(3, 'Boehringer Ingelheim'),
(4, 'Virbac'),
(5, 'Hipra');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_clinico`
--

CREATE TABLE `historial_clinico` (
  `numeroHistorial` int(11) NOT NULL,
  `fechaConsulta` int(11) NOT NULL,
  `motivoConsulta` longtext NOT NULL,
  `diagnostico` longtext NOT NULL,
  `observaciones` longtext DEFAULT NULL,
  `proximaCita` date NOT NULL,
  `Historial_Clinicocol` varchar(45) DEFAULT NULL,
  `Veterinario_idUsuario` int(11) NOT NULL,
  `idMascota` int(10) UNSIGNED NOT NULL,
  `fechaRegistro` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `historial_clinico`
--

INSERT INTO `historial_clinico` (`numeroHistorial`, `fechaConsulta`, `motivoConsulta`, `diagnostico`, `observaciones`, `proximaCita`, `Historial_Clinicocol`, `Veterinario_idUsuario`, `idMascota`, `fechaRegistro`) VALUES
(2, 2026, 'diarrea', 'diarrea', 'no coma basura', '2026-09-25', NULL, 2, 1, NULL),
(3, 2026, 'Control de peso', 'Parasitosis intestinal', '', '2026-03-25', NULL, 51, 25, '2026-06-21 02:23:59'),
(4, 2026, 'Perdida de apetito', 'Infeccion respiratoria', '', '2026-04-15', NULL, 52, 7, '2026-02-11 21:13:00'),
(5, 2026, 'Perdida de apetito', 'Obesidad', '', '2026-08-04', NULL, 49, 59, '2026-02-13 11:16:17'),
(6, 2026, 'Alergia alimentaria', 'Dermatitis alergica', '', '2026-04-06', NULL, 55, 25, '2026-08-05 18:30:48'),
(7, 2026, 'Perdida de apetito', 'Dermatitis alergica', 'Paciente estable', '2026-08-19', NULL, 52, 2, '2026-09-06 06:59:26'),
(8, 2025, 'Vomito', 'Infeccion respiratoria', 'Se recomienda seguimiento en dos semanas', '2026-03-10', NULL, 50, 9, '2026-06-22 22:30:06'),
(9, 2026, 'Diarrea', 'Dermatitis alergica', 'Paciente estable', '2026-05-18', NULL, 49, 39, '2026-08-13 01:35:26'),
(10, 2025, 'Tos persistente', 'Artritis leve', 'Continuar tratamiento indicado', '2026-09-08', NULL, 53, 24, '2026-05-03 11:16:02'),
(11, 2025, 'Control de rutina', 'Obesidad', 'Requiere control nutricional', '2026-09-19', NULL, 54, 58, '2026-05-25 13:54:12'),
(12, 2026, 'Diarrea', 'Fractura menor', 'Requiere control nutricional', '2026-04-04', NULL, 55, 34, '2026-01-27 23:20:40'),
(13, 2026, 'Picazon en la piel', 'Otitis externa', '', '2026-03-30', NULL, 54, 56, '2026-02-12 05:50:12'),
(14, 2025, 'Alergia alimentaria', 'Insuficiencia renal leve', 'Se recomienda seguimiento en dos semanas', '2026-06-22', NULL, 51, 29, '2026-08-19 12:43:40'),
(15, 2025, 'Herida en pata', 'Conjuntivitis', 'Continuar tratamiento indicado', '2026-06-01', NULL, 2, 27, '2026-02-11 13:39:00'),
(16, 2026, 'Infeccion de oido', 'Otitis externa', '', '2026-08-17', NULL, 50, 2, '2026-03-16 05:00:35'),
(17, 2026, 'Dolor abdominal', 'Dermatitis alergica', '', '2026-10-03', NULL, 2, 19, '2026-01-03 02:53:04'),
(18, 2025, 'Alergia alimentaria', 'Dermatitis alergica', 'Requiere control nutricional', '2026-08-19', NULL, 2, 17, '2026-05-28 08:39:07'),
(19, 2025, 'Cojera', 'Insuficiencia renal leve', '', '2026-09-03', NULL, 51, 8, '2026-02-26 18:11:53'),
(20, 2025, 'Revision post quirurgica', 'Conjuntivitis', 'Requiere control nutricional', '2026-06-25', NULL, 56, 35, '2026-03-15 10:36:17'),
(21, 2026, 'Cojera', 'Artritis leve', 'Sin observaciones adicionales', '2026-05-25', NULL, 53, 1, '2026-02-13 19:03:34'),
(22, 2025, 'Herida en pata', 'Sano, sin hallazgos relevantes', 'Se recomienda seguimiento en dos semanas', '2026-08-17', NULL, 52, 5, '2026-05-10 00:35:28'),
(23, 2025, 'Control de rutina', 'Fractura menor', '', '2026-04-20', NULL, 52, 51, '2026-06-20 04:51:26'),
(24, 2026, 'Alergia alimentaria', 'Obesidad', 'Paciente estable', '2026-10-16', NULL, 2, 23, '2026-09-03 23:00:02'),
(25, 2025, 'Vomito', 'Infeccion urinaria', 'Sin observaciones adicionales', '2026-02-13', NULL, 49, 52, '2026-04-14 01:35:41'),
(26, 2025, 'Control de rutina', 'Infeccion respiratoria', 'Requiere control nutricional', '2026-09-20', NULL, 50, 57, '2026-08-31 09:13:22'),
(27, 2025, 'Perdida de apetito', 'Parasitosis intestinal', 'Se recomienda seguimiento en dos semanas', '2026-07-13', NULL, 2, 49, '2026-03-05 13:23:57'),
(28, 2026, 'Dolor abdominal', 'Sano, sin hallazgos relevantes', 'Sin observaciones adicionales', '2026-07-11', NULL, 53, 57, '2026-09-12 18:06:51'),
(29, 2026, 'Control de peso', 'Otitis externa', '', '2026-08-18', NULL, 52, 60, '2026-08-09 21:56:04'),
(30, 2026, 'Control de rutina', 'Otitis externa', 'Se recomienda seguimiento en dos semanas', '2026-08-05', NULL, 49, 35, '2026-06-01 11:09:33'),
(31, 2026, 'Diarrea', 'Infeccion urinaria', 'Sin observaciones adicionales', '2026-08-03', NULL, 54, 39, '2026-01-22 12:50:10'),
(32, 2026, 'Revision post quirurgica', 'Estres digestivo', 'Se recomienda seguimiento en dos semanas', '2026-03-29', NULL, 56, 28, '2026-07-27 13:13:39'),
(33, 2025, 'Consulta general', 'Dermatitis alergica', 'Requiere control nutricional', '2026-08-22', NULL, 55, 52, '2026-03-06 02:26:52'),
(34, 2026, 'Control de rutina', 'Insuficiencia renal leve', 'Requiere control nutricional', '2026-03-03', NULL, 50, 50, '2026-03-20 16:22:52'),
(35, 2026, 'Control de peso', 'Otitis externa', 'Requiere control nutricional', '2026-04-21', NULL, 52, 24, '2026-07-07 14:29:31'),
(36, 2026, 'Cojera', 'Otitis externa', 'Se recomienda seguimiento en dos semanas', '2026-05-14', NULL, 52, 42, '2026-07-18 20:26:59'),
(37, 2026, 'Dolor abdominal', 'Sano, sin hallazgos relevantes', 'Paciente estable', '2026-07-17', NULL, 48, 17, '2026-09-09 07:17:17'),
(38, 2025, 'Tos persistente', 'Dermatitis alergica', '', '2026-08-28', NULL, 48, 9, '2026-06-13 12:38:57'),
(39, 2026, 'Infeccion de oido', 'Infeccion respiratoria', 'Paciente estable', '2026-05-27', NULL, 48, 36, '2026-07-05 18:31:23'),
(40, 2025, 'Control de peso', 'Dermatitis alergica', 'Paciente estable', '2026-09-18', NULL, 50, 8, '2026-07-04 20:10:16'),
(41, 2026, 'Control de peso', 'Conjuntivitis', 'Paciente estable', '2026-02-25', NULL, 56, 24, '2026-06-08 14:29:47'),
(42, 2026, 'Control de peso', 'Insuficiencia renal leve', 'Requiere control nutricional', '2026-02-22', NULL, 54, 34, '2026-06-12 23:23:12'),
(43, 2026, 'Revision post quirurgica', 'Conjuntivitis', 'Continuar tratamiento indicado', '2026-05-06', NULL, 48, 29, '2026-04-04 04:41:52'),
(44, 2025, 'Alergia alimentaria', 'Otitis externa', 'Requiere control nutricional', '2026-04-15', NULL, 53, 33, '2026-03-26 05:53:49'),
(45, 2025, 'Control de rutina', 'Conjuntivitis', 'Continuar tratamiento indicado', '2026-03-05', NULL, 52, 60, '2026-04-11 23:07:18'),
(46, 2025, 'Revision post quirurgica', 'Infeccion respiratoria', 'Requiere control nutricional', '2026-03-28', NULL, 55, 18, '2026-03-28 21:32:49'),
(47, 2025, 'Revision post quirurgica', 'Artritis leve', '', '2026-08-21', NULL, 56, 10, '2026-01-22 01:01:58'),
(48, 2026, 'Revision post quirurgica', 'Fractura menor', 'Requiere control nutricional', '2026-09-17', NULL, 48, 3, '2026-01-16 18:07:58'),
(49, 2026, 'Tos persistente', 'Fractura menor', '', '2026-08-22', NULL, 55, 19, '2026-08-21 02:39:24'),
(50, 2026, 'Herida en pata', 'Sano, sin hallazgos relevantes', 'Sin observaciones adicionales', '2026-05-13', NULL, 2, 11, '2026-03-15 11:22:50'),
(51, 2026, 'Herida en pata', 'Dermatitis alergica', 'Continuar tratamiento indicado', '2026-03-23', NULL, 52, 47, '2026-05-04 16:59:01'),
(52, 2026, 'Cojera', 'Dermatitis alergica', 'Se recomienda seguimiento en dos semanas', '2026-02-20', NULL, 48, 18, '2026-07-16 22:24:18'),
(53, 2025, 'Alergia alimentaria', 'Parasitosis intestinal', 'Requiere control nutricional', '2026-06-29', NULL, 53, 16, '2026-03-23 18:08:28'),
(54, 2026, 'Infeccion de oido', 'Infeccion respiratoria', 'Sin observaciones adicionales', '2026-01-24', NULL, 55, 30, '2026-07-21 18:24:09'),
(55, 2025, 'Letargo', 'Parasitosis intestinal', 'Se recomienda seguimiento en dos semanas', '2026-02-20', NULL, 54, 16, '2026-09-11 21:45:17'),
(56, 2025, 'Alergia alimentaria', 'Otitis externa', 'Sin observaciones adicionales', '2026-04-02', NULL, 54, 19, '2026-07-07 14:40:55'),
(57, 2026, 'Consulta general', 'Artritis leve', 'Requiere control nutricional', '2026-05-10', NULL, 52, 3, '2026-05-28 02:40:37'),
(58, 2025, 'Control geriatrico', 'Sarna', 'Requiere control nutricional', '2026-03-08', NULL, 2, 18, '2026-01-24 19:31:21'),
(59, 2026, 'Deshidratacion', 'Otitis externa', 'Continuar tratamiento indicado', '2026-04-30', NULL, 55, 22, '2026-07-29 12:09:11'),
(60, 2025, 'Mordedura de otro animal', 'Sarna', 'Paciente estable', '2026-07-28', NULL, 50, 11, '2025-01-07 11:29:07'),
(61, 2025, 'Diarrea', 'Reaccion alergica a picadura', 'Continuar tratamiento indicado', '2026-03-15', NULL, 54, 7, '2025-10-26 15:40:44'),
(62, 2026, 'Chequeo prequirurgico', 'Otitis externa', 'Requiere control nutricional', '2026-09-06', NULL, 56, 31, '2026-05-10 07:31:53'),
(63, 2026, 'Consulta general', 'Estres digestivo', 'Tutor debe vigilar cambios en el comportamiento', '2026-10-25', NULL, 56, 41, '2026-09-03 17:41:58'),
(64, 2026, 'Dolor abdominal', 'Cistitis', 'Se recomienda seguimiento en dos semanas', '2026-04-20', NULL, 55, 43, '2026-05-14 23:57:35'),
(65, 2026, 'Revision post quirurgica', 'Tendinitis', 'Se sugiere realizar examenes de laboratorio de control', '2026-04-13', NULL, 51, 11, '2026-02-01 20:52:06'),
(66, 2025, 'Estornudos frecuentes', 'Dermatitis alergica', '', '2026-02-28', NULL, 52, 2, '2025-08-23 14:46:18'),
(67, 2025, 'Convulsiones leves', 'Artritis leve', 'Mejoria notable respecto a consulta anterior', '2026-07-15', NULL, 55, 33, '2026-05-11 06:27:57'),
(68, 2026, 'Control de peso', 'Cataratas incipientes', 'Mejoria notable respecto a consulta anterior', '2026-01-31', NULL, 49, 35, '2025-05-18 16:36:53'),
(69, 2026, 'Seguimiento de tratamiento', 'Infeccion urinaria', 'Requiere control nutricional', '2026-11-07', NULL, 50, 2, '2026-03-07 00:00:46'),
(70, 2025, 'Control de vacunacion', 'Infeccion urinaria', 'Paciente estable', '2026-04-15', NULL, 48, 40, '2025-01-09 19:30:28'),
(71, 2026, 'Consulta general', 'Obesidad', 'Requiere control nutricional', '2026-09-10', NULL, 53, 5, '2026-07-30 12:11:26'),
(72, 2025, 'Convulsiones leves', 'Anemia leve', 'Continuar tratamiento indicado', '2026-08-18', NULL, 53, 23, '2025-05-28 13:32:53'),
(73, 2025, 'Fiebre', 'Anemia leve', 'Se recomienda seguimiento en dos semanas', '2026-10-01', NULL, 50, 48, '2025-11-10 05:19:14'),
(74, 2026, 'Dolor abdominal', 'Infeccion respiratoria', 'Tutor debe vigilar cambios en el comportamiento', '2026-09-03', NULL, 51, 50, '2025-05-03 22:55:25'),
(75, 2026, 'Alergia alimentaria', 'Otitis externa', 'Continuar tratamiento indicado', '2026-08-30', NULL, 49, 52, '2025-12-24 16:22:26'),
(76, 2026, 'Seguimiento de tratamiento', 'Cataratas incipientes', 'Se recomienda seguimiento en dos semanas', '2026-06-04', NULL, 54, 20, '2025-11-29 18:49:03'),
(77, 2025, 'Revision post quirurgica', 'Cistitis', 'Paciente estable', '2026-08-12', NULL, 55, 36, '2026-08-07 12:22:55'),
(78, 2025, 'Herida en pata', 'Obesidad', 'Requiere control nutricional', '2026-08-04', NULL, 48, 18, '2026-05-28 02:55:33'),
(79, 2025, 'Control geriatrico', 'Gastroenteritis leve', 'Sin observaciones adicionales', '2026-08-05', NULL, 50, 33, '2025-08-25 03:05:54'),
(80, 2025, 'Revision post quirurgica', 'Tendinitis', 'Tutor debe vigilar cambios en el comportamiento', '2026-05-24', NULL, 56, 21, '2025-05-19 17:37:13'),
(81, 2026, 'Control geriatrico', 'Gastroenteritis leve', 'Requiere control nutricional', '2026-10-26', NULL, 54, 47, '2026-02-10 08:09:39'),
(82, 2025, 'Caida de pelo', 'Anemia leve', 'Se recomienda seguimiento en dos semanas', '2026-10-31', NULL, 55, 60, '2026-08-22 04:27:12'),
(83, 2026, 'Dolor abdominal', 'Reaccion alergica a picadura', 'Se sugiere realizar examenes de laboratorio de control', '2026-08-02', NULL, 2, 24, '2025-08-10 16:20:57'),
(84, 2025, 'Fiebre', 'Parasitosis intestinal', 'Se sugiere realizar examenes de laboratorio de control', '2026-04-02', NULL, 52, 52, '2026-01-20 18:13:18'),
(85, 2025, 'Deshidratacion', 'Tendinitis', 'Se sugiere realizar examenes de laboratorio de control', '2026-07-23', NULL, 51, 54, '2025-10-31 04:27:13'),
(86, 2026, 'Vomito', 'Estres digestivo', 'Se recomienda seguimiento en dos semanas', '2026-09-01', NULL, 51, 33, '2025-04-21 16:57:00'),
(87, 2026, 'Tos persistente', 'Anemia leve', 'Mejoria notable respecto a consulta anterior', '2026-07-19', NULL, 56, 49, '2026-07-31 20:48:30'),
(88, 2026, 'Control de vacunacion', 'Enfermedad periodontal', 'Requiere control nutricional', '2026-07-05', NULL, 52, 16, '2025-07-06 04:29:26'),
(89, 2026, 'Revision dental', 'Infeccion respiratoria', 'Mejoria notable respecto a consulta anterior', '2026-03-30', NULL, 2, 28, '2026-07-09 05:09:13'),
(90, 2026, 'Mordedura de otro animal', 'Sano, sin hallazgos relevantes', 'Se indica reposo relativo', '2026-03-24', NULL, 52, 56, '2026-06-01 21:53:45'),
(91, 2025, 'Revision post quirurgica', 'Displasia de cadera leve', 'Se sugiere realizar examenes de laboratorio de control', '2026-05-12', NULL, 51, 46, '2025-09-17 10:22:47'),
(92, 2026, 'Alergia alimentaria', 'Otitis externa', 'Se indica reposo relativo', '2026-03-18', NULL, 49, 27, '2025-06-18 07:13:03'),
(93, 2025, 'Control geriatrico', 'Displasia de cadera leve', 'Tutor debe vigilar cambios en el comportamiento', '2026-05-25', NULL, 2, 18, '2026-05-27 17:01:01'),
(94, 2025, 'Cojera', 'Gingivitis', 'Se indica reposo relativo', '2026-06-24', NULL, 50, 19, '2026-06-18 02:28:03'),
(95, 2026, 'Cojera', 'Otitis externa', 'Se indica reposo relativo', '2026-11-08', NULL, 53, 27, '2026-03-23 18:50:02'),
(96, 2026, 'Perdida de apetito', 'Insuficiencia renal leve', 'Sin observaciones adicionales', '2026-05-15', NULL, 52, 10, '2025-09-08 07:34:45'),
(97, 2025, 'Consulta general', 'Sarna', '', '2026-01-28', NULL, 48, 26, '2025-09-10 15:51:43'),
(98, 2025, 'Letargo', 'Fractura menor', 'Requiere control nutricional', '2026-07-20', NULL, 51, 60, '2026-06-12 00:56:43'),
(99, 2025, 'Infeccion de oido', 'Sano, sin hallazgos relevantes', 'Se sugiere realizar examenes de laboratorio de control', '2026-07-05', NULL, 52, 51, '2026-03-22 14:13:26'),
(100, 2026, 'Diarrea', 'Sindrome vestibular leve', 'Se recomienda seguimiento en dos semanas', '2026-06-20', NULL, 55, 13, '2026-05-03 03:53:23'),
(101, 2026, 'Reaccion alergica leve', 'Infeccion urinaria', 'Tutor debe vigilar cambios en el comportamiento', '2026-03-13', NULL, 49, 8, '2026-06-18 12:51:09'),
(102, 2025, 'Control de peso', 'Otitis externa', 'Paciente estable', '2026-05-11', NULL, 51, 24, '2025-11-29 03:00:48'),
(103, 2025, 'Alergia alimentaria', 'Infeccion respiratoria', 'Mejoria notable respecto a consulta anterior', '2026-03-20', NULL, 50, 25, '2025-06-16 03:08:32'),
(104, 2025, 'Perdida de apetito', 'Sano, sin hallazgos relevantes', 'Mejoria notable respecto a consulta anterior', '2026-09-10', NULL, 52, 3, '2026-02-19 02:10:15'),
(105, 2025, 'Infeccion de oido', 'Rinitis', 'Paciente estable', '2026-03-22', NULL, 2, 10, '2026-01-03 16:00:02'),
(106, 2025, 'Tos persistente', 'Sarna', '', '2026-04-02', NULL, 50, 53, '2025-01-10 10:37:11'),
(107, 2026, 'Dolor abdominal', 'Sano, sin hallazgos relevantes', 'Se recomienda seguimiento en dos semanas', '2026-04-17', NULL, 52, 43, '2025-03-08 04:01:00'),
(108, 2025, 'Perdida de apetito', 'Tendinitis', 'Se recomienda seguimiento en dos semanas', '2026-07-14', NULL, 50, 19, '2026-07-23 13:23:21'),
(109, 2025, 'Deshidratacion', 'Sarna', 'Se recomienda seguimiento en dos semanas', '2026-07-19', NULL, 52, 41, '2025-11-17 00:07:10'),
(110, 2025, 'Convulsiones leves', 'Artritis leve', 'Paciente estable', '2026-08-12', NULL, 52, 33, '2025-11-30 16:11:30'),
(111, 2025, 'Control de peso', 'Anemia leve', 'Paciente estable', '2026-05-12', NULL, 48, 47, '2026-01-02 15:33:36'),
(112, 2025, 'Control de peso', 'Gastroenteritis leve', 'Requiere control nutricional', '2026-04-13', NULL, 54, 19, '2025-06-15 07:00:54'),
(113, 2025, 'Perdida de apetito', 'Rinitis', 'Se indica reposo relativo', '2026-04-03', NULL, 52, 12, '2025-05-05 02:40:45'),
(114, 2026, 'Dolor abdominal', 'Infeccion respiratoria', 'Requiere control nutricional', '2026-08-17', NULL, 52, 4, '2025-09-24 18:54:41'),
(115, 2025, 'Dificultad para orinar', 'Tendinitis', 'Paciente estable', '2026-07-31', NULL, 55, 56, '2025-11-12 16:00:07'),
(116, 2025, 'Fiebre', 'Fractura menor', 'Se indica reposo relativo', '2026-04-25', NULL, 50, 1, '2026-02-02 13:03:44'),
(117, 2025, 'Control de peso', 'Gastroenteritis leve', 'Se sugiere realizar examenes de laboratorio de control', '2026-05-25', NULL, 49, 53, '2025-01-05 15:11:24'),
(118, 2025, 'Control de rutina', 'Tendinitis', 'Requiere control nutricional', '2026-09-01', NULL, 49, 52, '2026-02-04 11:07:32'),
(119, 2025, 'Cojera', 'Enfermedad periodontal', 'Se sugiere realizar examenes de laboratorio de control', '2026-08-04', NULL, 48, 56, '2025-02-24 23:13:58'),
(120, 2025, 'Mordedura de otro animal', 'Artritis leve', 'Requiere control nutricional', '2026-07-02', NULL, 55, 46, '2025-05-29 11:13:05'),
(121, 2025, 'Alergia alimentaria', 'Insuficiencia renal leve', 'Mejoria notable respecto a consulta anterior', '2026-03-26', NULL, 51, 57, '2025-03-09 05:36:19'),
(122, 2025, 'Sangrado en encias', 'Sano, sin hallazgos relevantes', 'Paciente estable', '2026-11-07', NULL, 49, 43, '2025-07-14 14:35:23'),
(123, 2026, 'Inflamacion en pata', 'Tendinitis', '', '2026-03-14', NULL, 56, 46, '2025-04-02 15:59:42'),
(124, 2026, 'Inflamacion en pata', 'Gastroenteritis leve', 'Sin observaciones adicionales', '2026-02-14', NULL, 2, 61, '2026-06-22 03:35:25'),
(125, 2026, 'Seguimiento de tratamiento', 'Displasia de cadera leve', 'Mejoria notable respecto a consulta anterior', '2026-09-03', NULL, 2, 54, '2025-07-06 23:24:29'),
(126, 2025, 'Diarrea', 'Otitis externa', 'Sin observaciones adicionales', '2026-06-16', NULL, 53, 28, '2025-05-20 21:15:10'),
(127, 2026, 'Inflamacion en pata', 'Conjuntivitis', 'Requiere control nutricional', '2026-08-29', NULL, 51, 24, '2026-07-01 19:15:55'),
(128, 2025, 'Mordedura de otro animal', 'Enfermedad periodontal', 'Se indica reposo relativo', '2026-05-27', NULL, 55, 6, '2026-09-04 21:52:04'),
(129, 2025, 'Convulsiones leves', 'Tendinitis', 'Mejoria notable respecto a consulta anterior', '2026-03-11', NULL, 48, 30, '2025-10-31 16:56:40'),
(130, 2026, 'Fiebre', 'Tendinitis', 'Se indica reposo relativo', '2026-07-25', NULL, 52, 42, '2025-06-23 01:26:40'),
(131, 2025, 'Chequeo prequirurgico', 'Infeccion urinaria', 'Se sugiere realizar examenes de laboratorio de control', '2026-04-21', NULL, 52, 4, '2025-03-16 08:25:46'),
(132, 2025, 'Control de vacunacion', 'Reaccion alergica a picadura', 'Paciente estable', '2026-11-20', NULL, 56, 25, '2025-08-26 22:49:59'),
(133, 2026, 'Revision post quirurgica', 'Hipotiroidismo', 'Se sugiere realizar examenes de laboratorio de control', '2026-07-06', NULL, 55, 32, '2026-05-08 06:25:28'),
(134, 2025, 'Inflamacion en pata', 'Cistitis', 'Continuar tratamiento indicado', '2026-07-20', NULL, 50, 7, '2025-05-24 05:21:29'),
(135, 2026, 'Revision dental', 'Parasitosis intestinal', 'Tutor debe vigilar cambios en el comportamiento', '2026-07-19', NULL, 49, 44, '2025-07-07 17:41:28'),
(136, 2025, 'Chequeo prequirurgico', 'Infeccion respiratoria', 'Requiere control nutricional', '2026-09-08', NULL, 55, 43, '2026-03-26 08:11:05'),
(137, 2026, 'Control de peso', 'Obesidad', 'Se recomienda seguimiento en dos semanas', '2026-10-07', NULL, 48, 28, '2025-04-01 11:55:01'),
(138, 2025, 'Cojera', 'Otitis externa', '', '2026-08-23', NULL, 54, 30, '2025-09-02 19:57:39'),
(139, 2026, 'Dificultad para orinar', 'Sano, sin hallazgos relevantes', 'Se recomienda seguimiento en dos semanas', '2026-04-28', NULL, 55, 31, '2026-04-05 07:22:00'),
(140, 2025, 'Dolor abdominal', 'Anemia leve', 'Continuar tratamiento indicado', '2026-07-09', NULL, 54, 28, '2026-07-12 11:18:51'),
(141, 2025, 'Seguimiento de tratamiento', 'Otitis externa', 'Paciente estable', '2026-05-19', NULL, 48, 23, '2025-01-11 21:50:36'),
(142, 2026, 'Control de peso', 'Rinitis', 'Mejoria notable respecto a consulta anterior', '2026-03-05', NULL, 54, 33, '2025-07-07 12:42:35'),
(143, 2026, 'Revision dental', 'Sindrome vestibular leve', 'Paciente estable', '2026-08-07', NULL, 50, 22, '2026-01-03 18:33:23'),
(144, 2025, 'Picazon en la piel', 'Dermatitis alergica', 'Paciente estable', '2026-08-06', NULL, 53, 7, '2025-07-14 03:00:00'),
(145, 2026, 'Reaccion alergica leve', 'Displasia de cadera leve', 'Se sugiere realizar examenes de laboratorio de control', '2026-08-30', NULL, 48, 28, '2026-03-01 01:52:36'),
(146, 2026, 'Control de vacunacion', 'Estres digestivo', 'Se sugiere realizar examenes de laboratorio de control', '2026-08-13', NULL, 48, 60, '2025-12-24 02:17:51'),
(147, 2025, 'Cojera', 'Gastroenteritis leve', 'Mejoria notable respecto a consulta anterior', '2026-09-29', NULL, 52, 39, '2025-11-26 01:12:01'),
(148, 2026, 'Revision post quirurgica', 'Rinitis', 'Requiere control nutricional', '2026-05-05', NULL, 50, 52, '2026-09-12 17:10:25'),
(149, 2025, 'Caida de pelo', 'Sindrome vestibular leve', 'Continuar tratamiento indicado', '2026-06-05', NULL, 52, 15, '2025-09-04 17:19:32'),
(150, 2026, 'Reaccion alergica leve', 'Cataratas incipientes', 'Continuar tratamiento indicado', '2026-06-16', NULL, 55, 15, '2026-05-09 12:02:41'),
(151, 2026, 'Infeccion de oido', 'Conjuntivitis', 'Se sugiere realizar examenes de laboratorio de control', '2026-06-17', NULL, 54, 41, '2026-02-08 16:57:57'),
(152, 2026, 'Control de peso', 'Enfermedad periodontal', 'Se indica reposo relativo', '2026-04-13', NULL, 48, 59, '2025-02-28 05:51:44'),
(153, 2026, 'Dificultad para orinar', 'Hipotiroidismo', 'Requiere control nutricional', '2026-06-14', NULL, 53, 51, '2025-06-29 18:56:33'),
(154, 2026, 'Seguimiento de tratamiento', 'Displasia de cadera leve', 'Paciente estable', '2026-10-02', NULL, 50, 59, '2025-03-06 13:24:27'),
(155, 2025, 'Control de rutina', 'Infeccion urinaria', 'Se indica reposo relativo', '2026-03-31', NULL, 54, 45, '2025-11-08 15:08:39'),
(156, 2026, 'Picazon en la piel', 'Hipotiroidismo', 'Sin observaciones adicionales', '2026-05-25', NULL, 49, 11, '2025-07-15 21:24:40'),
(157, 2026, 'Reaccion alergica leve', 'Cataratas incipientes', 'Sin observaciones adicionales', '2026-08-28', NULL, 56, 4, '2025-07-05 05:46:09'),
(158, 2025, 'Revision post quirurgica', 'Otitis externa', 'Paciente estable', '2026-05-17', NULL, 52, 40, '2025-12-22 07:50:49'),
(159, 2026, 'Alergia alimentaria', 'Parasitosis intestinal', 'Paciente estable', '2026-04-20', NULL, 48, 12, '2025-08-31 23:49:41'),
(160, 2025, 'Tos persistente', 'Reaccion alergica a picadura', 'Requiere control nutricional', '2026-06-01', NULL, 48, 27, '2025-05-11 21:11:10'),
(161, 2026, 'Consulta general', 'Hipotiroidismo', 'Mejoria notable respecto a consulta anterior', '2026-08-10', NULL, 52, 48, '2026-03-19 21:52:56'),
(162, 2025, 'Herida en pata', 'Sano, sin hallazgos relevantes', 'Requiere control nutricional', '2026-10-24', NULL, 49, 59, '2025-05-30 21:23:30'),
(163, 2025, 'Tos persistente', 'Sindrome vestibular leve', 'Paciente estable', '2026-04-04', NULL, 50, 11, '2025-07-01 00:35:37'),
(164, 2026, 'Tos persistente', 'Tendinitis', 'Sin observaciones adicionales', '2026-08-26', NULL, 54, 47, '2025-10-02 09:09:24'),
(165, 2026, 'Tos persistente', 'Infeccion respiratoria', 'Se indica reposo relativo', '2026-05-16', NULL, 55, 20, '2025-01-10 03:17:45'),
(166, 2026, 'Chequeo prequirurgico', 'Dermatitis alergica', 'Continuar tratamiento indicado', '2026-10-14', NULL, 53, 33, '2026-06-01 02:37:22'),
(167, 2026, 'Cojera', 'Cataratas incipientes', 'Mejoria notable respecto a consulta anterior', '2026-02-07', NULL, 53, 35, '2025-09-29 12:09:56'),
(168, 2026, 'Deshidratacion', 'Artritis leve', 'Paciente estable', '2026-11-18', NULL, 49, 29, '2025-12-26 13:34:16'),
(169, 2026, 'Reaccion alergica leve', 'Rinitis', 'Requiere control nutricional', '2026-08-05', NULL, 48, 15, '2026-06-24 03:21:33'),
(170, 2026, 'Dolor abdominal', 'Gingivitis', 'Mejoria notable respecto a consulta anterior', '2026-08-13', NULL, 48, 1, '2025-07-02 08:44:53'),
(171, 2026, 'Herida en pata', 'Displasia de cadera leve', 'Mejoria notable respecto a consulta anterior', '2026-05-31', NULL, 49, 42, '2025-09-07 15:35:39'),
(172, 2025, 'Picazon en la piel', 'Enfermedad periodontal', '', '2026-10-05', NULL, 52, 25, '2026-03-20 23:44:59'),
(173, 2025, 'Chequeo prequirurgico', 'Sano, sin hallazgos relevantes', 'Se indica reposo relativo', '2026-05-24', NULL, 2, 46, '2026-09-06 12:31:14'),
(174, 2026, 'Inflamacion en pata', 'Gastroenteritis leve', 'Mejoria notable respecto a consulta anterior', '2026-08-26', NULL, 54, 52, '2025-02-25 08:38:40'),
(175, 2025, 'Seguimiento de tratamiento', 'Conjuntivitis', 'Continuar tratamiento indicado', '2026-10-09', NULL, 2, 17, '2025-06-13 08:48:33'),
(176, 2025, 'Convulsiones leves', 'Conjuntivitis', 'Se sugiere realizar examenes de laboratorio de control', '2026-09-14', NULL, 55, 9, '2026-01-26 20:30:30'),
(177, 2026, 'Seguimiento de tratamiento', 'Infeccion respiratoria', 'Paciente estable', '2026-07-01', NULL, 53, 17, '2025-11-06 03:21:35'),
(178, 2026, 'Revision dental', 'Enfermedad periodontal', 'Requiere control nutricional', '2026-09-05', NULL, 50, 29, '2025-02-10 16:13:37'),
(179, 2026, 'Estornudos frecuentes', 'Colitis', 'Paciente estable', '2026-03-16', NULL, 56, 36, '2025-12-08 03:46:16'),
(180, 2026, 'Estornudos frecuentes', 'Enfermedad periodontal', 'Requiere control nutricional', '2026-05-22', NULL, 2, 54, '2025-08-03 11:22:07'),
(181, 2026, 'Diarrea', 'Infeccion urinaria', 'Se indica reposo relativo', '2026-06-05', NULL, 55, 9, '2026-06-23 02:46:46'),
(182, 2026, 'Chequeo de rutina anual', 'Fractura menor', 'Tutor debe vigilar cambios en el comportamiento', '2026-09-16', NULL, 55, 35, '2025-01-12 15:59:21'),
(183, 2026, 'Control de rutina', 'Estres digestivo', 'Continuar tratamiento indicado', '2026-09-18', NULL, 51, 14, '2026-07-04 14:19:54'),
(184, 2026, 'Caida de pelo', 'Insuficiencia renal leve', 'Paciente estable', '2026-04-21', NULL, 55, 60, '2026-07-22 21:11:37'),
(185, 2026, 'Control de vacunacion', 'Anemia leve', 'Requiere control nutricional', '2026-02-03', NULL, 2, 1, '2025-05-03 23:23:03'),
(186, 2025, 'Chequeo de rutina anual', 'Dermatitis por pulgas', 'Se sugiere realizar examenes de laboratorio de control', '2026-10-02', NULL, 54, 25, '2026-04-01 12:29:39'),
(187, 2026, 'Cojera en pata trasera', 'Hipotiroidismo', 'Tutor debe vigilar cambios en el comportamiento', '2026-04-22', NULL, 56, 44, '2025-02-24 13:36:43'),
(188, 2026, 'Letargo', 'Reaccion alergica a picadura', 'Requiere control nutricional', '2026-11-04', NULL, 51, 58, '2025-01-14 10:14:05'),
(189, 2026, 'Revision post quirurgica', 'Infeccion respiratoria', '', '2026-03-26', NULL, 54, 23, '2025-10-01 10:58:07'),
(190, 2026, 'Revision post quirurgica', 'Dermatitis alergica', 'Mejoria notable respecto a consulta anterior', '2026-03-28', NULL, 53, 11, '2025-04-10 14:04:27'),
(191, 2025, 'Picazon en la piel', 'Anemia leve', 'Paciente estable', '2026-08-06', NULL, 56, 48, '2026-08-01 15:45:18'),
(192, 2026, 'Diarrea', 'Gastroenteritis leve', 'Sin observaciones adicionales', '2026-07-23', NULL, 55, 44, '2025-06-19 15:35:12'),
(193, 2026, 'Sangrado en encias', 'Reaccion alergica a picadura', 'Se recomienda seguimiento en dos semanas', '2026-10-07', NULL, 54, 48, '2025-09-14 02:19:51'),
(194, 2026, 'Secrecion nasal', 'Gastroenteritis leve', 'Continuar tratamiento indicado', '2026-02-23', NULL, 52, 4, '2026-08-16 06:31:55'),
(195, 2026, 'Caida de pelo', 'Insuficiencia renal leve', 'Requiere control nutricional', '2026-09-26', NULL, 56, 59, '2026-07-15 11:54:52'),
(196, 2026, 'Chequeo prequirurgico', 'Cataratas incipientes', 'Se indica reposo relativo', '2026-10-13', NULL, 50, 55, '2025-08-10 01:18:56'),
(197, 2026, 'Control postnatal', 'Estres digestivo', 'Requiere control nutricional', '2026-09-11', NULL, 51, 25, '2025-05-25 00:47:36'),
(198, 2025, 'Control postnatal', 'Estres digestivo', 'Tutor debe vigilar cambios en el comportamiento', '2026-08-06', NULL, 2, 16, '2026-03-31 17:26:27'),
(199, 2026, 'Control de peso', 'Tendinitis', 'Se sugiere realizar examenes de laboratorio de control', '2026-11-14', NULL, 52, 1, '2026-06-30 15:11:54'),
(200, 2025, 'Inapetencia', 'Hipotiroidismo', 'Continuar tratamiento indicado', '2026-07-02', NULL, 54, 49, '2026-02-08 12:19:09'),
(201, 2026, 'Revision post quirurgica', 'Parasitosis intestinal', 'Paciente estable', '2026-09-04', NULL, 52, 50, '2025-12-25 13:32:56'),
(202, 2026, 'Mordedura de otro animal', 'Infeccion urinaria', 'Se indica reposo relativo', '2026-06-16', NULL, 49, 12, '2026-05-20 02:10:52'),
(203, 2026, 'Picazon en la piel', 'Obesidad', 'Se solicita nueva cita en un mes', '2026-10-26', NULL, 56, 57, '2026-01-02 13:36:49'),
(204, 2026, 'Consulta general', 'Reaccion alergica a picadura', 'Continuar tratamiento indicado', '2026-10-06', NULL, 50, 33, '2025-08-24 08:40:50'),
(205, 2026, 'Chequeo de rutina anual', 'Tendinitis', 'Buena evolucion clinica', '2026-09-05', NULL, 48, 47, '2026-08-23 18:44:00'),
(206, 2025, 'Caida de pelo', 'Reaccion alergica a picadura', 'Sin observaciones adicionales', '2026-04-11', NULL, 48, 18, '2026-08-06 00:03:49'),
(207, 2026, 'Fiebre', 'Sarna', 'Buena evolucion clinica', '2026-03-04', NULL, 2, 30, '2026-04-28 06:24:07'),
(208, 2025, 'Caida de pelo', 'Sindrome vestibular leve', 'Se sugiere realizar examenes de laboratorio de control', '2026-05-17', NULL, 48, 54, '2025-02-07 14:32:00'),
(209, 2026, 'Letargo', 'Rinitis', 'Se indica reposo relativo', '2026-04-24', NULL, 56, 43, '2025-06-02 00:09:51'),
(210, 2026, 'Chequeo de rutina anual', 'Bronquitis leve', 'Tutor debe vigilar cambios en el comportamiento', '2026-05-08', NULL, 56, 1, '2025-07-04 06:20:52'),
(211, 2025, 'Dificultad para orinar', 'Dermatitis por pulgas', 'Buena evolucion clinica', '2026-03-28', NULL, 53, 30, '2026-07-10 13:23:19'),
(212, 2025, 'Cojera en pata trasera', 'Parasitosis intestinal', '', '2026-04-19', NULL, 51, 45, '2025-05-10 02:50:32'),
(213, 2026, 'Caida de pelo', 'Bronquitis leve', 'Sin observaciones adicionales', '2026-03-11', NULL, 49, 15, '2025-02-26 18:24:16'),
(214, 2026, 'Seguimiento de tratamiento', 'Cistitis', 'Tutor debe vigilar cambios en el comportamiento', '2026-03-19', NULL, 2, 27, '2025-05-17 02:21:42'),
(215, 2025, 'Mordedura de otro animal', 'Cistitis', 'Se indica reposo relativo', '2026-07-06', NULL, 50, 18, '2025-02-23 20:07:03'),
(216, 2025, 'Caida de pelo', 'Reaccion alergica a picadura', 'Se indica reposo relativo', '2026-09-02', NULL, 52, 47, '2025-05-31 23:11:07'),
(217, 2026, 'Deshidratacion', 'Otitis media', 'Se indica reposo relativo', '2026-07-27', NULL, 54, 24, '2026-01-18 20:27:22'),
(218, 2025, 'Letargo', 'Parasitosis intestinal', 'Se recomienda seguimiento en dos semanas', '2026-07-06', NULL, 54, 40, '2026-01-04 16:22:25'),
(219, 2026, 'Consulta general', 'Insuficiencia renal leve', '', '2026-07-05', NULL, 48, 1, '2025-11-21 07:15:23'),
(220, 2025, 'Revision dental', 'Rinitis', 'Se recomienda seguimiento en dos semanas', '2026-06-08', NULL, 49, 55, '2026-04-20 07:51:17'),
(221, 2026, 'Tos persistente', 'Insuficiencia renal leve', 'Mejoria notable respecto a consulta anterior', '2026-07-12', NULL, 50, 10, '2025-09-20 06:43:20'),
(222, 2025, 'Perdida de apetito', 'Infeccion urinaria', 'Se indica reposo relativo', '2026-08-17', NULL, 54, 54, '2025-09-01 05:58:00'),
(223, 2025, 'Control geriatrico', 'Hipotiroidismo', 'Requiere control nutricional', '2026-03-02', NULL, 50, 10, '2025-10-20 06:09:10'),
(224, 2026, 'Infeccion de oido', 'Rinitis', 'Requiere control nutricional', '2026-01-15', NULL, 56, 34, '2026-06-30 01:12:13'),
(225, 2025, 'Deshidratacion', 'Parasitosis intestinal', 'Se sugiere realizar examenes de laboratorio de control', '2026-09-20', NULL, 52, 3, '2026-02-02 22:43:58'),
(226, 2026, 'Reaccion alergica leve', 'Otitis externa', '', '2026-08-18', NULL, 56, 19, '2025-05-07 14:16:17'),
(227, 2026, 'Control de rutina', 'Sindrome vestibular leve', 'Continuar tratamiento indicado', '2026-08-31', NULL, 55, 15, '2026-03-17 12:48:14'),
(228, 2026, 'Perdida de apetito', 'Otitis externa', 'Continuar tratamiento indicado', '2026-06-25', NULL, 48, 29, '2026-01-29 16:07:51'),
(229, 2025, 'Chequeo prequirurgico', 'Rinitis', 'Se sugiere realizar examenes de laboratorio de control', '2026-02-22', NULL, 52, 12, '2025-07-27 17:42:22'),
(230, 2026, 'Control de peso', 'Infeccion urinaria', 'Mejoria notable respecto a consulta anterior', '2026-08-19', NULL, 54, 60, '2025-08-26 21:35:14'),
(231, 2025, 'Cojera en pata trasera', 'Obesidad', 'Mejoria notable respecto a consulta anterior', '2026-03-26', NULL, 48, 14, '2025-09-13 18:53:40'),
(232, 2026, 'Caida de pelo', 'Fractura menor', 'Tutor debe vigilar cambios en el comportamiento', '2026-11-30', NULL, 51, 36, '2025-09-04 15:06:38'),
(233, 2025, 'Caida de pelo', 'Gastroenteritis leve', 'Se sugiere realizar examenes de laboratorio de control', '2026-08-11', NULL, 51, 50, '2026-04-12 14:01:41'),
(234, 2026, 'Chequeo de rutina anual', 'Artritis leve', 'Tutor debe vigilar cambios en el comportamiento', '2026-03-08', NULL, 53, 59, '2026-09-13 05:06:14'),
(235, 2025, 'Seguimiento de tratamiento', 'Rinitis', 'Paciente estable', '2026-05-03', NULL, 54, 8, '2025-12-23 10:00:16'),
(236, 2026, 'Infeccion de oido', 'Sindrome vestibular leve', 'Se solicita nueva cita en un mes', '2026-05-18', NULL, 53, 5, '2025-06-01 12:22:19'),
(237, 2025, 'Alergia alimentaria', 'Conjuntivitis', '', '2026-07-19', NULL, 53, 26, '2025-03-12 19:37:04'),
(238, 2026, 'Diarrea', 'Tendinitis', 'Paciente estable', '2026-11-04', NULL, 52, 34, '2025-06-19 15:19:12'),
(239, 2026, 'Convulsiones leves', 'Cataratas incipientes', 'Buena evolucion clinica', '2026-06-21', NULL, 52, 29, '2025-02-16 18:24:08'),
(240, 2026, 'Secrecion nasal', 'Anemia leve', 'Se indica reposo relativo', '2026-04-09', NULL, 48, 37, '2026-05-03 03:19:21'),
(241, 2026, 'Picazon en la piel', 'Parasitosis intestinal', 'Continuar tratamiento indicado', '2026-05-29', NULL, 49, 15, '2026-07-04 19:06:32'),
(242, 2026, 'Control de vacunacion', 'Rinitis', 'Se solicita nueva cita en un mes', '2026-06-26', NULL, 55, 48, '2026-06-12 14:52:13'),
(243, 2026, 'Perdida de apetito', 'Sano, sin hallazgos relevantes', 'Buena evolucion clinica', '2026-05-09', NULL, 55, 13, '2025-09-12 18:48:43'),
(244, 2025, 'Revision dental', 'Otitis externa', 'Paciente estable', '2026-09-21', NULL, 48, 61, '2025-03-28 23:24:04'),
(245, 2026, 'Picazon en la piel', 'Tendinitis', 'Se solicita nueva cita en un mes', '2026-04-03', NULL, 55, 5, '2025-07-31 14:35:42'),
(246, 2026, 'Control postnatal', 'Rinitis', 'Se recomienda seguimiento en dos semanas', '2026-10-19', NULL, 48, 51, '2025-02-11 20:46:52'),
(247, 2026, 'Tos persistente', 'Obesidad', 'Se sugiere realizar examenes de laboratorio de control', '2026-04-22', NULL, 51, 19, '2026-08-09 16:27:04'),
(248, 2025, 'Chequeo prequirurgico', 'Sano, sin hallazgos relevantes', 'Buena evolucion clinica', '2026-04-27', NULL, 53, 5, '2025-12-26 08:56:05'),
(249, 2026, 'Dolor abdominal', 'Conjuntivitis', 'Mejoria notable respecto a consulta anterior', '2026-08-18', NULL, 50, 36, '2025-06-03 06:23:24'),
(250, 2025, 'Deshidratacion', 'Enfermedad periodontal', 'Buena evolucion clinica', '2026-04-22', NULL, 52, 16, '2025-01-31 07:54:45'),
(251, 2025, 'Dificultad para orinar', 'Tendinitis', 'Requiere control nutricional', '2026-08-01', NULL, 49, 51, '2025-07-29 23:28:02'),
(252, 2025, 'Deshidratacion', 'Anemia leve', '', '2026-10-31', NULL, 56, 25, '2025-07-16 19:42:25'),
(253, 2026, 'Chequeo prequirurgico', 'Fractura menor', 'Buena evolucion clinica', '2026-05-25', NULL, 2, 50, '2026-08-22 18:26:50'),
(254, 2025, 'Chequeo de rutina anual', 'Displasia de cadera leve', 'Tutor debe vigilar cambios en el comportamiento', '2026-04-18', NULL, 51, 60, '2025-12-04 15:41:04'),
(255, 2026, 'Diarrea', 'Artritis leve', 'Continuar tratamiento indicado', '2026-09-27', NULL, 2, 4, '2026-05-03 03:49:02'),
(256, 2026, 'Cojera', 'Sindrome vestibular leve', '', '2026-06-18', NULL, 56, 8, '2025-02-01 11:45:56'),
(257, 2026, 'Revision dental', 'Infeccion respiratoria', 'Se indica reposo relativo', '2026-03-01', NULL, 49, 61, '2026-02-12 03:13:45'),
(258, 2026, 'Estornudos frecuentes', 'Displasia de cadera leve', '', '2026-07-04', NULL, 49, 51, '2025-01-09 18:49:29'),
(259, 2026, 'Control de peso', 'Infeccion respiratoria', 'Paciente estable', '2026-03-01', NULL, 54, 55, '2025-05-29 13:01:02'),
(260, 2026, 'Convulsiones leves', 'Hipotiroidismo', 'Se indica reposo relativo', '2026-06-19', NULL, 49, 38, '2025-07-23 21:41:54'),
(261, 2025, 'Dolor abdominal', 'Conjuntivitis', 'Paciente estable', '2026-04-13', NULL, 56, 40, '2025-12-13 19:38:26'),
(262, 2026, 'Inapetencia', 'Rinitis', 'Sin observaciones adicionales', '2026-05-11', NULL, 52, 42, '2025-06-24 08:06:41'),
(263, 2026, 'Sangrado en encias', 'Insuficiencia renal leve', 'Requiere control nutricional', '2026-10-26', NULL, 48, 37, '2026-03-31 22:15:38'),
(264, 2026, 'Perdida de apetito', 'Fractura menor', 'Se recomienda seguimiento en dos semanas', '2026-07-17', NULL, 55, 30, '2025-04-06 07:47:44'),
(265, 2026, 'Secrecion nasal', 'Conjuntivitis', 'Se indica reposo relativo', '2026-03-30', NULL, 56, 22, '2026-03-19 23:22:24'),
(266, 2026, 'Perdida de apetito', 'Artritis leve', 'Se solicita nueva cita en un mes', '2026-07-13', NULL, 50, 54, '2025-11-03 15:06:38'),
(267, 2025, 'Secrecion nasal', 'Parasitosis intestinal', 'Se indica reposo relativo', '2026-02-18', NULL, 49, 36, '2026-06-18 17:33:26'),
(268, 2025, 'Herida en pata', 'Parasitosis intestinal', 'Paciente estable', '2026-03-13', NULL, 54, 6, '2025-05-05 16:46:41'),
(269, 2025, 'Cojera', 'Sarna', 'Requiere control nutricional', '2026-05-25', NULL, 2, 35, '2025-05-09 18:29:59'),
(270, 2026, 'Estornudos frecuentes', 'Infeccion urinaria', 'Mejoria notable respecto a consulta anterior', '2026-09-08', NULL, 56, 55, '2025-10-27 08:55:09'),
(271, 2025, 'Control de peso', 'Cataratas incipientes', '', '2026-08-28', NULL, 49, 32, '2026-07-05 20:13:56'),
(272, 2026, 'Deshidratacion', 'Rinitis', 'Buena evolucion clinica', '2026-09-18', NULL, 48, 57, '2025-01-18 12:45:35'),
(273, 2025, 'Chequeo de rutina anual', 'Cataratas incipientes', 'Mejoria notable respecto a consulta anterior', '2026-07-22', NULL, 56, 35, '2025-02-19 18:12:28'),
(274, 2025, 'Inapetencia', 'Colitis', 'Tutor debe vigilar cambios en el comportamiento', '2026-09-17', NULL, 51, 58, '2026-08-21 20:58:53'),
(275, 2026, 'Dolor abdominal', 'Cataratas incipientes', 'Mejoria notable respecto a consulta anterior', '2026-05-22', NULL, 51, 35, '2025-04-24 12:20:05'),
(276, 2025, 'Cojera', 'Insuficiencia renal leve', 'Requiere control nutricional', '2026-08-08', NULL, 48, 23, '2026-03-25 01:31:56'),
(277, 2026, 'Secrecion nasal', 'Estres digestivo', '', '2026-05-14', NULL, 51, 48, '2026-07-05 06:57:03');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_medicamento`
--

CREATE TABLE `historial_medicamento` (
  `Historial_Clinico_numeroHistorial` int(11) NOT NULL,
  `Medicamento_idMedicamento` int(11) NOT NULL,
  `cantidad` varchar(100) NOT NULL,
  `instrucciones` longtext NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `historial_medicamento`
--

INSERT INTO `historial_medicamento` (`Historial_Clinico_numeroHistorial`, `Medicamento_idMedicamento`, `cantidad`, `instrucciones`) VALUES
(4, 5, '5 ml', 'Aplicar via oral cada 8 horas'),
(5, 11, '3 ml', 'Una tableta al dia con alimento'),
(9, 10, '3 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(10, 2, '2 tabletas', 'Aplicar via oral cada 8 horas'),
(10, 6, '2 tabletas', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(11, 10, '3 ml', 'Una tableta al dia con alimento'),
(11, 11, '1 tableta', 'Administrar cada 12 horas por 7 dias'),
(12, 9, '3 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(14, 4, '10 ml', 'Aplicar via oral cada 8 horas'),
(15, 4, '3 ml', 'Aplicar via oral cada 8 horas'),
(16, 5, '10 ml', 'Una tableta al dia con alimento'),
(18, 11, '3 ml', 'Una tableta al dia con alimento'),
(20, 11, '5 ml', 'Administrar cada 12 horas por 7 dias'),
(22, 3, '2 tabletas', 'Aplicar via oral cada 8 horas'),
(22, 10, '10 ml', 'Una tableta al dia con alimento'),
(24, 9, '10 ml', 'Aplicar via oral cada 8 horas'),
(26, 9, '2 tabletas', 'Aplicar via oral cada 8 horas'),
(27, 6, '1 tableta', 'Aplicar via oral cada 8 horas'),
(27, 10, '10 ml', 'Aplicar via oral cada 8 horas'),
(28, 4, '2 tabletas', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(28, 6, '2 tabletas', 'Administrar cada 12 horas por 7 dias'),
(28, 11, '2 tabletas', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(30, 6, '2 tabletas', 'Una tableta al dia con alimento'),
(30, 11, '10 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(34, 1, '5 ml', 'Administrar cada 12 horas por 7 dias'),
(34, 9, '3 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(36, 6, '10 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(37, 5, '1 tableta', 'Administrar cada 12 horas por 7 dias'),
(37, 6, '2 tabletas', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(38, 2, '1 tableta', 'Administrar cada 12 horas por 7 dias'),
(41, 10, '1 tableta', 'Una tableta al dia con alimento'),
(42, 4, '5 ml', 'Administrar cada 12 horas por 7 dias'),
(43, 4, '10 ml', 'Aplicar via oral cada 8 horas'),
(43, 10, '1 tableta', 'Administrar cada 12 horas por 7 dias'),
(45, 2, '2 tabletas', 'Aplicar via oral cada 8 horas'),
(45, 3, '5 ml', 'Una tableta al dia con alimento'),
(46, 8, '10 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(47, 10, '10 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(48, 1, '3 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(50, 3, '1 tableta', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(52, 6, '3 ml', 'Aplicar via oral cada 8 horas'),
(52, 11, '5 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(54, 1, '10 ml', 'Aplicar via oral cada 8 horas'),
(54, 10, '3 ml', 'Administrar cada 12 horas por 7 dias'),
(57, 1, '1 tableta', 'Una tableta al dia con alimento'),
(58, 11, '10 ml', 'Aplicar via oral cada 8 horas'),
(59, 9, '1/2 tableta', 'Administrar en ayunas por 3 dias'),
(60, 2, '5 ml', 'Aplicar cada 24 horas durante 10 dias'),
(60, 6, '3 ml', 'Suministrar con abundante agua cada 12 horas'),
(62, 2, '3 ml', 'Una tableta al dia con alimento'),
(64, 11, '10 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(66, 5, '5 ml', 'Suministrar con abundante agua cada 12 horas'),
(67, 1, '15 ml', 'Administrar cada 12 horas por 7 dias'),
(70, 8, '2 tabletas', 'Una tableta al dia con alimento'),
(71, 8, '5 ml', 'Administrar en ayunas por 3 dias'),
(72, 1, '2 tabletas', 'Administrar cada 12 horas por 7 dias'),
(76, 11, '2 tabletas', 'Administrar en ayunas por 3 dias'),
(77, 2, '10 ml', 'Administrar en ayunas por 3 dias'),
(78, 2, '1 tableta', 'Aplicar via oral cada 8 horas'),
(79, 11, '1 tableta', 'Suministrar con abundante agua cada 12 horas'),
(82, 1, '1/2 tableta', 'Administrar en ayunas por 3 dias'),
(82, 4, '2 tabletas', 'Administrar cada 12 horas por 7 dias'),
(84, 6, '5 ml', 'Una tableta al dia con alimento'),
(86, 4, '1 tableta', 'Administrar cada 12 horas por 7 dias'),
(87, 6, '5 ml', 'Aplicar cada 24 horas durante 10 dias'),
(87, 9, '1 tableta', 'Una tableta al dia con alimento'),
(88, 3, '2 tabletas', 'Suministrar con abundante agua cada 12 horas'),
(89, 1, '15 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(89, 8, '3 ml', 'Administrar en ayunas por 3 dias'),
(93, 8, '3 ml', 'Administrar en ayunas por 3 dias'),
(94, 3, '3 ml', 'Suministrar con abundante agua cada 12 horas'),
(94, 8, '3 ml', 'Administrar en ayunas por 3 dias'),
(96, 5, '15 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(97, 2, '1 tableta', 'Una tableta al dia con alimento'),
(97, 4, '3 ml', 'Aplicar via oral cada 8 horas'),
(101, 6, '1 tableta', 'Administrar en ayunas por 3 dias'),
(103, 9, '1/2 tableta', 'Aplicar cada 24 horas durante 10 dias'),
(104, 3, '2 tabletas', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(106, 3, '15 ml', 'Aplicar cada 24 horas durante 10 dias'),
(106, 7, '1/2 tableta', 'Una tableta al dia con alimento'),
(108, 6, '1 tableta', 'Una tableta al dia con alimento'),
(108, 7, '3 ml', 'Administrar cada 12 horas por 7 dias'),
(108, 11, '1/2 tableta', 'Una tableta al dia con alimento'),
(112, 3, '10 ml', 'Una tableta al dia con alimento'),
(112, 4, '5 ml', 'Aplicar cada 24 horas durante 10 dias'),
(113, 3, '3 ml', 'Aplicar cada 24 horas durante 10 dias'),
(114, 1, '1/2 tableta', 'Suministrar con abundante agua cada 12 horas'),
(119, 6, '3 ml', 'Una tableta al dia con alimento'),
(119, 10, '5 ml', 'Administrar cada 12 horas por 7 dias'),
(120, 6, '3 ml', 'Aplicar cada 24 horas durante 10 dias'),
(121, 1, '15 ml', 'Aplicar cada 24 horas durante 10 dias'),
(123, 5, '5 ml', 'Suministrar con abundante agua cada 12 horas'),
(124, 4, '1/2 tableta', 'Suministrar con abundante agua cada 12 horas'),
(124, 7, '1 tableta', 'Aplicar via oral cada 8 horas'),
(124, 9, '1/2 tableta', 'Administrar en ayunas por 3 dias'),
(124, 10, '1 tableta', 'Suministrar con abundante agua cada 12 horas'),
(125, 2, '1/2 tableta', 'Una tableta al dia con alimento'),
(126, 5, '1/2 tableta', 'Suministrar con abundante agua cada 12 horas'),
(126, 9, '2 tabletas', 'Aplicar cada 24 horas durante 10 dias'),
(129, 7, '2 tabletas', 'Administrar cada 12 horas por 7 dias'),
(132, 11, '5 ml', 'Una tableta al dia con alimento'),
(136, 3, '10 ml', 'Una tableta al dia con alimento'),
(137, 1, '2 tabletas', 'Administrar cada 12 horas por 7 dias'),
(137, 4, '5 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(137, 7, '1/2 tableta', 'Suministrar con abundante agua cada 12 horas'),
(138, 8, '10 ml', 'Administrar en ayunas por 3 dias'),
(139, 10, '1/2 tableta', 'Aplicar cada 24 horas durante 10 dias'),
(140, 4, '10 ml', 'Una tableta al dia con alimento'),
(141, 4, '1 tableta', 'Suministrar con abundante agua cada 12 horas'),
(143, 9, '1/2 tableta', 'Suministrar con abundante agua cada 12 horas'),
(145, 8, '1/2 tableta', 'Una tableta al dia con alimento'),
(146, 3, '1/2 tableta', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(151, 3, '1 tableta', 'Administrar en ayunas por 3 dias'),
(152, 8, '3 ml', 'Administrar en ayunas por 3 dias'),
(153, 6, '1/2 tableta', 'Administrar cada 12 horas por 7 dias'),
(155, 7, '1/2 tableta', 'Aplicar via oral cada 8 horas'),
(156, 5, '1/2 tableta', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(156, 11, '10 ml', 'Administrar en ayunas por 3 dias'),
(157, 8, '15 ml', 'Administrar en ayunas por 3 dias'),
(165, 10, '15 ml', 'Aplicar via oral cada 8 horas'),
(166, 3, '1 tableta', 'Una tableta al dia con alimento'),
(166, 6, '2 tabletas', 'Suministrar con abundante agua cada 12 horas'),
(166, 10, '5 ml', 'Administrar en ayunas por 3 dias'),
(166, 11, '5 ml', 'Una tableta al dia con alimento'),
(167, 10, '1 tableta', 'Aplicar cada 24 horas durante 10 dias'),
(169, 5, '15 ml', 'Aplicar cada 24 horas durante 10 dias'),
(173, 4, '1/2 tableta', 'Una tableta al dia con alimento'),
(174, 6, '1 tableta', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(175, 1, '5 ml', 'Administrar en ayunas por 3 dias'),
(175, 6, '5 ml', 'Una tableta al dia con alimento'),
(175, 8, '3 ml', 'Aplicar cada 24 horas durante 10 dias'),
(177, 4, '10 ml', 'Suministrar con abundante agua cada 12 horas'),
(178, 1, '2 tabletas', 'Administrar en ayunas por 3 dias'),
(178, 9, '15 ml', 'Aplicar cada 24 horas durante 10 dias'),
(181, 11, '2 tabletas', 'Administrar en ayunas por 3 dias'),
(182, 2, '3 ml', 'Una tableta al dia con alimento'),
(182, 10, '1/2 tableta', 'Aplicar via oral cada 8 horas'),
(184, 9, '1/2 tableta', 'Administrar en ayunas por 3 dias'),
(189, 2, '2 tabletas', 'Administrar en ayunas por 3 dias'),
(190, 8, '1/2 tableta', 'Administrar en ayunas por 3 dias'),
(191, 1, '10 ml', 'Aplicar via oral cada 8 horas'),
(191, 4, '1/2 tableta', 'Administrar en ayunas por 3 dias'),
(191, 9, '2 tabletas', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(192, 2, '1/2 tableta', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(194, 10, '1/2 tableta', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(198, 8, '5 ml', 'Suministrar con abundante agua cada 12 horas'),
(200, 4, '1 tableta', 'Administrar cada 12 horas por 7 dias'),
(200, 9, '5 ml', 'Suministrar con abundante agua cada 12 horas'),
(201, 1, '1 tableta', 'Administrar cada 12 horas por 7 dias'),
(204, 5, '5 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(205, 4, '5 ml', 'Aplicar cada 24 horas durante 10 dias'),
(206, 2, '10 ml', 'Suministrar con abundante agua cada 12 horas'),
(208, 1, '10 ml', 'Aplicar cada 24 horas durante 10 dias'),
(209, 5, '3 ml', 'Una tableta al dia con alimento'),
(209, 10, '1/2 tableta', 'Aplicar via oral cada 8 horas'),
(213, 4, '1 tableta', 'Una tableta al dia con alimento'),
(215, 3, '2 tabletas', 'Administrar cada 12 horas por 7 dias'),
(215, 5, '1 tableta', 'Aplicar via oral cada 8 horas'),
(218, 11, '5 ml', 'Aplicar via oral cada 8 horas'),
(220, 11, '10 ml', 'Administrar en ayunas por 3 dias'),
(221, 1, '1 tableta', 'Administrar cada 12 horas por 7 dias'),
(221, 4, '10 ml', 'Administrar cada 12 horas por 7 dias'),
(223, 2, '2 tabletas', 'Suministrar con abundante agua cada 12 horas'),
(224, 2, '2 tabletas', 'Administrar cada 12 horas por 7 dias'),
(224, 5, '10 ml', 'Aplicar cada 24 horas durante 10 dias'),
(226, 8, '10 ml', 'Aplicar via oral cada 8 horas'),
(228, 6, '15 ml', 'Una tableta al dia con alimento'),
(229, 3, '10 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(232, 4, '5 ml', 'Suministrar con abundante agua cada 12 horas'),
(232, 7, '10 ml', 'Aplicar via oral cada 8 horas'),
(233, 3, '2 tabletas', 'Una tableta al dia con alimento'),
(234, 11, '10 ml', 'Una tableta al dia con alimento'),
(235, 3, '3 ml', 'Una tableta al dia con alimento'),
(235, 5, '3 ml', 'Aplicar via oral cada 8 horas'),
(235, 7, '1 tableta', 'Una tableta al dia con alimento'),
(236, 2, '1 tableta', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(238, 6, '1/2 tableta', 'Administrar en ayunas por 3 dias'),
(238, 7, '2 tabletas', 'Administrar cada 12 horas por 7 dias'),
(239, 4, '15 ml', 'Administrar cada 12 horas por 7 dias'),
(239, 10, '3 ml', 'Aplicar cada 24 horas durante 10 dias'),
(241, 7, '15 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(242, 4, '2 tabletas', 'Administrar en ayunas por 3 dias'),
(243, 1, '3 ml', 'Administrar cada 12 horas por 7 dias'),
(245, 11, '3 ml', 'Suministrar con abundante agua cada 12 horas'),
(247, 11, '15 ml', 'Una tableta al dia con alimento'),
(257, 6, '2 tabletas', 'Aplicar via oral cada 8 horas'),
(257, 11, '5 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(258, 4, '15 ml', 'Aplicar cada 24 horas durante 10 dias'),
(259, 6, '1/2 tableta', 'Una tableta al dia con alimento'),
(260, 10, '15 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(263, 7, '10 ml', 'Una tableta al dia con alimento'),
(264, 3, '5 ml', 'Suministrar con abundante agua cada 12 horas'),
(266, 2, '15 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(266, 5, '10 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(266, 11, '10 ml', 'Suministrar con abundante agua cada 12 horas'),
(267, 3, '10 ml', 'Una tableta al dia con alimento'),
(269, 5, '1/2 tableta', 'Suministrar segun peso, 1 vez al dia por 5 dias'),
(270, 5, '15 ml', 'Una tableta al dia con alimento'),
(271, 4, '5 ml', 'Aplicar via oral cada 8 horas'),
(273, 10, '2 tabletas', 'Administrar en ayunas por 3 dias'),
(273, 11, '1 tableta', 'Una tableta al dia con alimento'),
(275, 8, '10 ml', 'Una tableta al dia con alimento'),
(277, 8, '1/2 tableta', 'Administrar cada 12 horas por 7 dias'),
(277, 11, '15 ml', 'Suministrar segun peso, 1 vez al dia por 5 dias');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_tratamiento`
--

CREATE TABLE `historial_tratamiento` (
  `Tratamiento_idTratamiento` int(11) NOT NULL,
  `Historial_Clinico_numeroHistorial` int(11) NOT NULL,
  `duracionTratamiento` varchar(45) NOT NULL,
  `intrucciones` longtext NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `historial_tratamiento`
--

INSERT INTO `historial_tratamiento` (`Tratamiento_idTratamiento`, `Historial_Clinico_numeroHistorial`, `duracionTratamiento`, `intrucciones`) VALUES
(1, 18, '2 semanas', 'Reposo absoluto por 10 dias'),
(1, 44, '7 dias', 'Seguimiento semanal en clinica'),
(1, 91, '45 dias', 'Evitar actividad fisica intensa por 15 dias'),
(1, 126, '15 dias', 'Reposo absoluto por 10 dias'),
(1, 150, '10 dias', 'Sesiones dos veces por semana durante un mes'),
(1, 166, '21 dias', 'Aplicar compresas frias dos veces al dia'),
(1, 167, '7 dias', 'Control de dieta estricta por 30 dias'),
(1, 171, '10 dias', 'Seguimiento semanal en clinica'),
(1, 183, '7 dias', 'Control de dieta estricta por 30 dias'),
(1, 191, '21 dias', 'Sesiones dos veces por semana durante un mes'),
(1, 202, '21 dias', 'Seguimiento semanal en clinica'),
(1, 205, '15 dias', 'Aplicar compresas frias dos veces al dia'),
(1, 206, '10 dias', 'Aplicar compresas frias dos veces al dia'),
(1, 219, '21 dias', 'Sesiones dos veces por semana durante un mes'),
(1, 232, '45 dias', 'Control de dieta estricta por 30 dias'),
(1, 239, '10 dias', 'Evitar actividad fisica intensa por 15 dias'),
(1, 255, '21 dias', 'Control de dieta estricta por 30 dias'),
(1, 260, '45 dias', 'Sesiones dos veces por semana durante un mes'),
(1, 272, '1 mes', 'Seguimiento semanal en clinica'),
(1, 275, '2 semanas', 'Seguimiento semanal en clinica'),
(2, 4, '2 semanas', 'Control de dieta estricta por 30 dias'),
(2, 23, '1 mes', 'Seguimiento semanal en clinica'),
(2, 34, '2 semanas', 'Sesiones dos veces por semana durante un mes'),
(2, 35, '2 semanas', 'Control de dieta estricta por 30 dias'),
(2, 38, '7 dias', 'Sesiones dos veces por semana durante un mes'),
(2, 42, '1 mes', 'Control de dieta estricta por 30 dias'),
(2, 45, '15 dias', 'Seguimiento semanal en clinica'),
(2, 70, '10 dias', 'Evitar actividad fisica intensa por 15 dias'),
(2, 82, '45 dias', 'Seguimiento semanal en clinica'),
(2, 210, '15 dias', 'Reposo absoluto por 10 dias'),
(2, 256, '15 dias', 'Ejercicio controlado y progresivo'),
(3, 2, '1 mes', 'Control de dieta estricta por 30 dias'),
(3, 22, '10 dias', 'Seguimiento semanal en clinica'),
(3, 28, '15 dias', 'Sesiones dos veces por semana durante un mes'),
(3, 31, '10 dias', 'Control de dieta estricta por 30 dias'),
(3, 32, '1 mes', 'Reposo absoluto por 10 dias'),
(3, 48, '2 semanas', 'Control de dieta estricta por 30 dias'),
(3, 60, '45 dias', 'Aplicar compresas frias dos veces al dia'),
(3, 73, '15 dias', 'Reposo absoluto por 10 dias'),
(3, 98, '10 dias', 'Seguimiento semanal en clinica'),
(3, 121, '7 dias', 'Seguimiento semanal en clinica'),
(3, 123, '45 dias', 'Ejercicio controlado y progresivo'),
(3, 160, '7 dias', 'Evitar actividad fisica intensa por 15 dias'),
(3, 192, '15 dias', 'Evitar actividad fisica intensa por 15 dias'),
(3, 198, '21 dias', 'Evitar actividad fisica intensa por 15 dias'),
(3, 210, '10 dias', 'Sesiones dos veces por semana durante un mes'),
(3, 227, '10 dias', 'Evitar actividad fisica intensa por 15 dias'),
(3, 245, '1 mes', 'Reposo absoluto por 10 dias'),
(3, 249, '21 dias', 'Sesiones dos veces por semana durante un mes'),
(3, 267, '2 semanas', 'Ejercicio controlado y progresivo'),
(4, 13, '10 dias', 'Seguimiento semanal en clinica'),
(4, 16, '15 dias', 'Seguimiento semanal en clinica'),
(4, 83, '7 dias', 'Reposo absoluto por 10 dias'),
(4, 85, '45 dias', 'Ejercicio controlado y progresivo'),
(4, 95, '2 semanas', 'Ejercicio controlado y progresivo'),
(4, 96, '2 semanas', 'Ejercicio controlado y progresivo'),
(4, 111, '21 dias', 'Evitar actividad fisica intensa por 15 dias'),
(4, 114, '2 semanas', 'Reposo absoluto por 10 dias'),
(4, 115, '2 semanas', 'Reposo absoluto por 10 dias'),
(4, 117, '2 semanas', 'Ejercicio controlado y progresivo'),
(4, 158, '21 dias', 'Seguimiento semanal en clinica'),
(4, 160, '10 dias', 'Seguimiento semanal en clinica'),
(4, 171, '2 semanas', 'Reposo absoluto por 10 dias'),
(4, 173, '1 mes', 'Evitar actividad fisica intensa por 15 dias'),
(4, 207, '10 dias', 'Sesiones dos veces por semana durante un mes'),
(4, 208, '10 dias', 'Seguimiento semanal en clinica'),
(4, 269, '1 mes', 'Reposo absoluto por 10 dias'),
(5, 4, '7 dias', 'Control de dieta estricta por 30 dias'),
(5, 12, '2 semanas', 'Control de dieta estricta por 30 dias'),
(5, 39, '10 dias', 'Sesiones dos veces por semana durante un mes'),
(5, 43, '1 mes', 'Reposo absoluto por 10 dias'),
(5, 48, '7 dias', 'Seguimiento semanal en clinica'),
(5, 54, '7 dias', 'Reposo absoluto por 10 dias'),
(5, 68, '2 semanas', 'Control de dieta estricta por 30 dias'),
(5, 74, '1 mes', 'Reposo absoluto por 10 dias'),
(5, 75, '45 dias', 'Reposo absoluto por 10 dias'),
(5, 79, '15 dias', 'Evitar actividad fisica intensa por 15 dias'),
(5, 85, '21 dias', 'Seguimiento semanal en clinica'),
(5, 97, '1 mes', 'Evitar actividad fisica intensa por 15 dias'),
(5, 101, '15 dias', 'Control de dieta estricta por 30 dias'),
(5, 110, '21 dias', 'Ejercicio controlado y progresivo'),
(5, 122, '10 dias', 'Sesiones dos veces por semana durante un mes'),
(5, 126, '21 dias', 'Aplicar compresas frias dos veces al dia'),
(5, 165, '15 dias', 'Seguimiento semanal en clinica'),
(5, 166, '10 dias', 'Sesiones dos veces por semana durante un mes'),
(5, 167, '1 mes', 'Sesiones dos veces por semana durante un mes'),
(5, 180, '45 dias', 'Reposo absoluto por 10 dias'),
(5, 189, '45 dias', 'Reposo absoluto por 10 dias'),
(5, 197, '21 dias', 'Control de dieta estricta por 30 dias'),
(5, 219, '15 dias', 'Ejercicio controlado y progresivo'),
(5, 233, '7 dias', 'Seguimiento semanal en clinica'),
(5, 234, '15 dias', 'Sesiones dos veces por semana durante un mes'),
(6, 67, '7 dias', 'Seguimiento semanal en clinica'),
(6, 112, '45 dias', 'Evitar actividad fisica intensa por 15 dias'),
(6, 143, '21 dias', 'Sesiones dos veces por semana durante un mes'),
(6, 153, '10 dias', 'Aplicar compresas frias dos veces al dia'),
(6, 170, '7 dias', 'Reposo absoluto por 10 dias'),
(6, 171, '2 semanas', 'Ejercicio controlado y progresivo'),
(6, 184, '21 dias', 'Evitar actividad fisica intensa por 15 dias'),
(6, 186, '21 dias', 'Seguimiento semanal en clinica'),
(6, 206, '21 dias', 'Ejercicio controlado y progresivo'),
(6, 217, '10 dias', 'Reposo absoluto por 10 dias'),
(6, 238, '21 dias', 'Ejercicio controlado y progresivo'),
(6, 242, '7 dias', 'Aplicar compresas frias dos veces al dia'),
(7, 34, '1 mes', 'Sesiones dos veces por semana durante un mes'),
(7, 44, '7 dias', 'Reposo absoluto por 10 dias'),
(7, 46, '10 dias', 'Seguimiento semanal en clinica'),
(7, 52, '2 semanas', 'Reposo absoluto por 10 dias'),
(7, 62, '45 dias', 'Evitar actividad fisica intensa por 15 dias'),
(7, 73, '10 dias', 'Aplicar compresas frias dos veces al dia'),
(7, 95, '7 dias', 'Reposo absoluto por 10 dias'),
(7, 97, '1 mes', 'Aplicar compresas frias dos veces al dia'),
(7, 102, '7 dias', 'Ejercicio controlado y progresivo'),
(7, 169, '2 semanas', 'Sesiones dos veces por semana durante un mes'),
(7, 179, '10 dias', 'Sesiones dos veces por semana durante un mes'),
(7, 223, '15 dias', 'Sesiones dos veces por semana durante un mes'),
(7, 246, '10 dias', 'Ejercicio controlado y progresivo'),
(7, 262, '10 dias', 'Evitar actividad fisica intensa por 15 dias'),
(8, 10, '10 dias', 'Seguimiento semanal en clinica'),
(8, 38, '7 dias', 'Seguimiento semanal en clinica'),
(8, 50, '1 mes', 'Control de dieta estricta por 30 dias'),
(8, 60, '45 dias', 'Sesiones dos veces por semana durante un mes'),
(8, 101, '7 dias', 'Reposo absoluto por 10 dias'),
(8, 116, '15 dias', 'Reposo absoluto por 10 dias'),
(8, 143, '15 dias', 'Evitar actividad fisica intensa por 15 dias'),
(8, 145, '15 dias', 'Reposo absoluto por 10 dias'),
(8, 150, '7 dias', 'Evitar actividad fisica intensa por 15 dias'),
(8, 154, '15 dias', 'Sesiones dos veces por semana durante un mes'),
(8, 171, '2 semanas', 'Reposo absoluto por 10 dias'),
(8, 187, '7 dias', 'Seguimiento semanal en clinica'),
(8, 204, '1 mes', 'Reposo absoluto por 10 dias'),
(8, 222, '45 dias', 'Aplicar compresas frias dos veces al dia'),
(8, 223, '7 dias', 'Sesiones dos veces por semana durante un mes'),
(8, 229, '45 dias', 'Seguimiento semanal en clinica'),
(8, 237, '21 dias', 'Sesiones dos veces por semana durante un mes'),
(8, 242, '15 dias', 'Ejercicio controlado y progresivo'),
(8, 273, '21 dias', 'Reposo absoluto por 10 dias'),
(9, 31, '1 mes', 'Reposo absoluto por 10 dias'),
(9, 34, '15 dias', 'Reposo absoluto por 10 dias'),
(9, 35, '2 semanas', 'Reposo absoluto por 10 dias'),
(9, 53, '15 dias', 'Reposo absoluto por 10 dias'),
(9, 58, '7 dias', 'Sesiones dos veces por semana durante un mes'),
(9, 85, '2 semanas', 'Sesiones dos veces por semana durante un mes'),
(9, 116, '21 dias', 'Control de dieta estricta por 30 dias'),
(9, 123, '1 mes', 'Reposo absoluto por 10 dias'),
(9, 133, '10 dias', 'Ejercicio controlado y progresivo'),
(9, 148, '1 mes', 'Ejercicio controlado y progresivo'),
(9, 157, '45 dias', 'Ejercicio controlado y progresivo'),
(9, 167, '10 dias', 'Aplicar compresas frias dos veces al dia'),
(9, 176, '10 dias', 'Aplicar compresas frias dos veces al dia'),
(9, 186, '10 dias', 'Aplicar compresas frias dos veces al dia'),
(9, 190, '45 dias', 'Evitar actividad fisica intensa por 15 dias'),
(9, 195, '10 dias', 'Ejercicio controlado y progresivo'),
(9, 197, '2 semanas', 'Aplicar compresas frias dos veces al dia'),
(9, 220, '45 dias', 'Sesiones dos veces por semana durante un mes');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `horario_veterinaria`
--

CREATE TABLE `horario_veterinaria` (
  `idHorario_Veterinaria` int(11) NOT NULL,
  `idDiaSemana` int(11) NOT NULL,
  `horaApertura` time NOT NULL,
  `horaCierre` time NOT NULL,
  `estado` tinyint(4) NOT NULL DEFAULT 1,
  `idVeterinaria` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `horario_veterinaria`
--

INSERT INTO `horario_veterinaria` (`idHorario_Veterinaria`, `idDiaSemana`, `horaApertura`, `horaCierre`, `estado`, `idVeterinaria`) VALUES
(1, 1, '08:00:00', '18:00:00', 1, 1),
(2, 2, '08:00:00', '18:00:00', 1, 1),
(3, 1, '06:00:00', '18:00:00', 1, 2),
(4, 1, '06:00:00', '19:00:00', 1, 4),
(5, 1, '20:34:00', '00:23:00', 1, 5),
(6, 3, '08:00:00', '18:00:00', 1, 6),
(7, 6, '06:00:00', '18:00:00', 1, 1),
(8, 3, '06:00:00', '19:00:00', 1, 1),
(9, 4, '06:00:00', '18:00:00', 1, 1),
(10, 5, '08:00:00', '18:00:00', 1, 1),
(11, 2, '06:00:00', '17:00:00', 1, 1),
(12, 1, '08:00:00', '17:00:00', 1, 1),
(13, 1, '08:00:00', '18:00:00', 1, 2),
(14, 6, '07:00:00', '20:00:00', 1, 2),
(15, 3, '06:00:00', '18:00:00', 1, 2),
(16, 5, '08:00:00', '20:00:00', 1, 2),
(17, 4, '06:00:00', '20:00:00', 1, 3),
(18, 6, '07:00:00', '20:00:00', 1, 3),
(19, 2, '07:00:00', '20:00:00', 1, 3),
(20, 1, '08:00:00', '17:00:00', 1, 3),
(21, 6, '06:00:00', '18:00:00', 1, 4),
(22, 1, '06:00:00', '20:00:00', 1, 4),
(23, 5, '06:00:00', '20:00:00', 1, 4),
(24, 2, '06:00:00', '19:00:00', 1, 4),
(25, 3, '07:00:00', '18:00:00', 1, 4),
(26, 4, '06:00:00', '20:00:00', 1, 4),
(27, 1, '07:00:00', '20:00:00', 1, 5),
(28, 6, '07:00:00', '18:00:00', 1, 5),
(29, 5, '07:00:00', '17:00:00', 1, 5),
(30, 4, '06:00:00', '20:00:00', 1, 5),
(31, 3, '06:00:00', '20:00:00', 1, 5),
(32, 2, '07:00:00', '20:00:00', 1, 5),
(33, 4, '07:00:00', '18:00:00', 1, 6),
(34, 5, '06:00:00', '17:00:00', 1, 6),
(35, 6, '08:00:00', '19:00:00', 1, 6),
(36, 1, '06:00:00', '17:00:00', 1, 6),
(37, 3, '08:00:00', '20:00:00', 1, 6),
(38, 5, '06:00:00', '17:00:00', 1, 7),
(39, 2, '08:00:00', '18:00:00', 1, 7),
(40, 1, '07:00:00', '17:00:00', 1, 7),
(41, 3, '08:00:00', '18:00:00', 1, 7),
(42, 4, '08:00:00', '17:00:00', 1, 7),
(43, 6, '08:00:00', '17:00:00', 1, 7),
(44, 6, '08:00:00', '19:00:00', 1, 8),
(45, 5, '06:00:00', '19:00:00', 1, 8),
(46, 3, '07:00:00', '18:00:00', 1, 8),
(47, 2, '08:00:00', '19:00:00', 1, 8),
(48, 1, '07:00:00', '19:00:00', 1, 8),
(49, 1, '08:00:00', '18:00:00', 1, 9),
(50, 4, '08:00:00', '19:00:00', 1, 9),
(51, 6, '06:00:00', '19:00:00', 1, 9),
(52, 5, '06:00:00', '18:00:00', 1, 9),
(53, 3, '08:00:00', '17:00:00', 1, 10),
(54, 2, '08:00:00', '19:00:00', 1, 10),
(55, 4, '08:00:00', '17:00:00', 1, 10),
(56, 6, '06:00:00', '19:00:00', 1, 10),
(57, 5, '06:00:00', '17:00:00', 1, 10);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `localidad`
--

CREATE TABLE `localidad` (
  `idLocalidad` int(11) NOT NULL,
  `nombre` varchar(45) NOT NULL,
  `idCiudad` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `localidad`
--

INSERT INTO `localidad` (`idLocalidad`, `nombre`, `idCiudad`) VALUES
(1, 'Engativa', 1),
(2, 'Usaquén', 1),
(3, 'Chapinero', 1),
(4, 'Santa Fe', 1),
(5, 'San Cristóbal', 1),
(6, 'Usme', 1),
(7, 'Tunjuelito', 1),
(8, 'Bosa', 1),
(9, 'Kennedy', 1),
(10, 'Fontibón', 1),
(11, 'Suba', 1),
(12, 'Barrios Unidos', 1),
(13, 'Teusaquillo', 1),
(14, 'Los Mártires', 1),
(15, 'Antonio Nariño', 1),
(16, 'Puente Aranda', 1),
(17, 'La Candelaria', 1),
(18, 'Rafael Uribe Uribe', 1),
(19, 'Ciudad Bolívar', 1),
(20, 'Sin localidad especificada', 1),
(21, 'El Poblado', 2),
(22, 'Laureles', 2),
(23, 'Belén', 2),
(24, 'Envigado', 2),
(25, 'Granada', 3),
(26, 'San Fernando', 3),
(27, 'Chipichape', 3),
(28, 'El Prado', 4),
(29, 'Riomar', 4),
(30, 'Boston', 4),
(31, 'Bocagrande', 5),
(32, 'Getsemaní', 5),
(33, 'Manga', 5);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `lote_vacuna`
--

CREATE TABLE `lote_vacuna` (
  `idLote` int(11) NOT NULL,
  `numeroLote` varchar(45) NOT NULL,
  `fechaVencimiento` date NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `lote_vacuna`
--

INSERT INTO `lote_vacuna` (`idLote`, `numeroLote`, `fechaVencimiento`) VALUES
(1, 'L001', '2027-12-31'),
(2, 'L002', '2027-02-19'),
(3, 'L003', '2026-11-22'),
(4, 'L004', '2027-08-05'),
(5, 'L005', '2027-07-05'),
(6, 'L006', '2027-06-13'),
(7, 'L007', '2027-03-19'),
(8, 'L008', '2027-02-09'),
(9, 'L009', '2028-05-08'),
(10, 'L010', '2027-01-25'),
(11, 'L011', '2028-06-23'),
(12, 'L012', '2028-01-03'),
(13, 'L013', '2026-11-29'),
(14, 'L014', '2026-11-27'),
(15, 'L015', '2027-01-31');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `mascota`
--

CREATE TABLE `mascota` (
  `idMascota` int(10) UNSIGNED NOT NULL,
  `nombre` varchar(45) NOT NULL,
  `idEspecie` int(11) NOT NULL,
  `genero` enum('H','M') NOT NULL,
  `idRaza` int(11) NOT NULL,
  `fechaNacimiento` date NOT NULL,
  `estado` tinyint(4) NOT NULL,
  `foto` varchar(500) DEFAULT NULL,
  `peso` double NOT NULL,
  `Tutor_idUsuario` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `mascota`
--

INSERT INTO `mascota` (`idMascota`, `nombre`, `idEspecie`, `genero`, `idRaza`, `fechaNacimiento`, `estado`, `foto`, `peso`, `Tutor_idUsuario`) VALUES
(1, 'Max', 1, 'M', 1, '2020-12-12', 0, NULL, 18.5, 1),
(2, 'Lula', 1, 'H', 76, '2026-03-03', 1, NULL, 2, 8),
(3, 'Max', 2, 'M', 84, '2020-03-03', 1, NULL, 5, 10),
(4, 'pitu', 1, 'M', 67, '2025-06-22', 1, NULL, 14, 12),
(5, 'l', 2, 'H', 3, '2023-03-31', 1, NULL, 9, 12),
(6, 'lulu', 2, 'H', 3, '2026-02-12', 0, '', 6, 1),
(7, 'Rambo', 8, 'M', 112, '2026-06-21', 1, NULL, 27.8, 41),
(8, 'Chispa', 7, 'H', 109, '2026-05-05', 1, NULL, 20.5, 24),
(9, 'Rambo', 11, 'M', 117, '2025-01-11', 1, NULL, 20.1, 27),
(10, 'Firulais', 4, 'M', 94, '2024-01-22', 1, NULL, 7.2, 33),
(11, 'Perla', 12, 'H', 119, '2018-09-21', 1, NULL, 15.2, 38),
(12, 'Thor', 7, 'M', 105, '2022-05-15', 1, NULL, 26.5, 5),
(13, 'Oreo', 10, 'H', 115, '2021-12-11', 1, NULL, 17.9, 35),
(14, 'Kiwi', 9, 'H', 113, '2021-01-22', 1, NULL, 1.8, 30),
(15, 'Whitey', 11, 'H', 118, '2023-03-30', 1, NULL, 44.1, 43),
(16, 'Nube', 1, 'H', 73, '2018-12-10', 1, NULL, 6.5, 38),
(17, 'Nina', 3, 'M', 85, '2021-09-02', 1, NULL, 15, 33),
(18, 'Nina', 5, 'H', 100, '2023-04-11', 0, NULL, 24.5, 31),
(19, 'Princesa', 4, 'H', 92, '2026-06-17', 0, NULL, 11.5, 5),
(20, 'Bruno', 10, 'H', 115, '2023-04-24', 1, NULL, 25.6, 22),
(21, 'Nina', 8, 'M', 112, '2019-11-19', 1, NULL, 43.4, 16),
(22, 'Coco', 3, 'H', 87, '2021-07-01', 1, NULL, 40.9, 33),
(23, 'Simba', 7, 'H', 106, '2019-02-22', 1, NULL, 13.9, 47),
(24, 'Pelusa', 2, 'M', 3, '2025-06-02', 1, NULL, 23, 30),
(25, 'Kiwi', 1, 'H', 74, '2022-11-11', 1, NULL, 28.8, 38),
(26, 'Pipo', 12, 'H', 119, '2026-03-25', 1, NULL, 29.4, 43),
(27, 'Pipo', 8, 'M', 111, '2021-08-13', 1, NULL, 37.5, 12),
(28, 'Bruno', 5, 'M', 100, '2024-05-23', 1, NULL, 17.4, 5),
(29, 'Zeus', 8, 'M', 111, '2020-05-18', 1, NULL, 12, 26),
(30, 'Bimbo', 10, 'H', 115, '2023-10-18', 1, NULL, 4.3, 35),
(31, 'Bruno', 8, 'M', 112, '2025-03-30', 1, NULL, 20.4, 5),
(32, 'Lola', 2, 'M', 80, '2025-10-04', 1, NULL, 30, 32),
(33, 'Rambo', 8, 'M', 112, '2022-10-09', 1, NULL, 25, 31),
(34, 'Leo', 12, 'M', 119, '2020-10-26', 1, NULL, 32.6, 29),
(35, 'Lucky', 2, 'H', 84, '2020-02-24', 1, NULL, 22, 46),
(36, 'Coco', 9, 'H', 114, '2021-04-28', 0, NULL, 8.5, 1),
(37, 'Leo', 12, 'H', 119, '2018-08-12', 1, NULL, 31.5, 17),
(38, 'Coco', 11, 'H', 117, '2024-06-09', 1, NULL, 21.8, 30),
(39, 'Nina', 3, 'M', 85, '2019-04-13', 1, NULL, 18.3, 10),
(40, 'Duke', 10, 'H', 115, '2024-04-24', 1, NULL, 4.3, 24),
(41, 'Copito', 2, 'H', 84, '2026-09-13', 1, NULL, 20.5, 37),
(42, 'Boby', 5, 'H', 100, '2024-11-02', 1, NULL, 4.9, 22),
(43, 'Nina', 11, 'H', 116, '2019-10-06', 1, NULL, 25.1, 19),
(44, 'Chispa', 1, 'M', 74, '2021-04-07', 0, NULL, 13.3, 27),
(45, 'Simba', 12, 'H', 119, '2020-12-19', 1, NULL, 28.3, 21),
(46, 'Frida', 7, 'H', 105, '2025-04-06', 1, NULL, 12.3, 18),
(47, 'Kira', 2, 'M', 3, '2024-09-03', 1, NULL, 25.8, 27),
(48, 'Suki', 8, 'M', 110, '2025-11-06', 1, NULL, 12.6, 43),
(49, 'Milo', 8, 'H', 111, '2022-11-05', 1, NULL, 27.4, 5),
(50, 'Rufo', 2, 'H', 79, '2026-07-30', 1, NULL, 12.5, 8),
(51, 'Rambo', 3, 'M', 88, '2021-02-13', 1, NULL, 26.5, 40),
(52, 'Pelusa', 2, 'M', 83, '2021-09-27', 1, NULL, 5.2, 19),
(53, 'Lucky', 6, 'M', 104, '2021-03-26', 1, NULL, 18.3, 44),
(54, 'Milo', 1, 'M', 75, '2020-10-30', 1, NULL, 43.7, 34),
(55, 'Chester', 9, 'M', 113, '2022-08-20', 1, NULL, 23.6, 40),
(56, 'Nala', 11, 'H', 117, '2020-12-29', 1, NULL, 41.8, 37),
(57, 'Bella', 12, 'H', 119, '2025-01-24', 1, NULL, 11.1, 19),
(58, 'Bimbo', 5, 'M', 97, '2019-01-17', 1, NULL, 37.9, 16),
(59, 'Princesa', 8, 'H', 110, '2023-08-04', 1, NULL, 13.5, 26),
(60, 'Oreo', 7, 'H', 108, '2023-02-14', 1, NULL, 17.6, 47),
(61, 'Simba', 9, 'M', 113, '2026-08-31', 1, NULL, 19, 41),
(62, 'lala', 2, 'M', 78, '2020-12-12', 0, '', 8, 1),
(63, 'Lula', 1, 'H', 76, '2026-01-01', 1, '', 10, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `medicamento`
--

CREATE TABLE `medicamento` (
  `idMedicamento` int(11) NOT NULL,
  `nombreMedicamento` varchar(45) NOT NULL,
  `dosis` varchar(45) NOT NULL,
  `fabricante` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `medicamento`
--

INSERT INTO `medicamento` (`idMedicamento`, `nombreMedicamento`, `dosis`, `fabricante`) VALUES
(1, 'Ivermectina', '1 ml', 'VetPharma'),
(2, 'Amoxicilina', '250 mg', 'VetPharma'),
(3, 'Meloxicam', '0.5 mg/kg', 'Zoetis'),
(4, 'Prednisolona', '5 mg', 'MSD'),
(5, 'Enrofloxacina', '50 mg', 'Bayer'),
(6, 'Omeprazol', '10 mg', 'VetPharma'),
(7, 'Furosemida', '20 mg', 'MSD'),
(8, 'Tramadol', '50 mg', 'Zoetis'),
(9, 'Cefalexina', '500 mg', 'VetPharma'),
(10, 'Metronidazol', '250 mg', 'Bayer'),
(11, 'Dexametasona', '4 mg', 'MSD');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `mensaje_chat`
--

CREATE TABLE `mensaje_chat` (
  `idMensaje` int(11) NOT NULL,
  `contenido` text NOT NULL,
  `enviadoEn` datetime(6) NOT NULL,
  `idConversacion` int(11) NOT NULL,
  `idRemitente` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Volcado de datos para la tabla `mensaje_chat`
--

INSERT INTO `mensaje_chat` (`idMensaje`, `contenido`, `enviadoEn`, `idConversacion`, `idRemitente`) VALUES
(1, 'Hola', '2026-09-15 01:53:08.000000', 1, 1),
(2, 'Hola', '2026-09-15 01:55:16.000000', 1, 2),
(3, 'El valor es de 60000 pesos', '2026-01-06 05:57:53.000000', 1, 1),
(4, 'Perfecto, queda agendada', '2026-05-13 06:04:44.000000', 1, 2),
(5, 'El valor es de 60000 pesos', '2026-05-18 04:17:53.000000', 2, 41),
(6, 'Perfecto, queda agendada', '2026-08-08 09:54:09.000000', 2, 41),
(7, 'El sabado en la manana', '2026-02-09 18:03:51.000000', 2, 41),
(8, 'Mi mascota no ha querido comer', '2026-07-04 02:01:06.000000', 2, 39),
(9, 'Perfecto, queda agendada', '2026-03-22 09:17:18.000000', 3, 57),
(10, 'Buenos dias, necesito informacion sobre vacunas', '2026-03-01 08:18:57.000000', 3, 57),
(11, 'Hola, buenas tardes', '2026-01-07 08:54:13.000000', 3, 57),
(12, 'Con gusto, cualquier cosa me escribe', '2026-07-06 13:56:37.000000', 3, 57),
(13, 'Claro, con gusto le ayudo', '2026-01-19 20:07:45.000000', 3, 57),
(14, 'Perfecto, queda agendada', '2026-05-08 15:09:19.000000', 4, 9),
(15, 'El sabado en la manana', '2026-02-09 20:48:32.000000', 4, 25),
(16, 'Quisiera agendar una cita para mi mascota', '2026-09-15 01:55:03.000000', 4, 25),
(17, 'Que dia le queda bien?', '2026-01-24 14:35:50.000000', 4, 9),
(18, 'Buenos dias, necesito informacion sobre vacunas', '2026-08-27 10:16:42.000000', 4, 9),
(19, 'Quisiera agendar una cita para mi mascota', '2026-08-22 00:17:24.000000', 5, 44),
(20, 'Que dia le queda bien?', '2026-05-30 20:31:39.000000', 5, 44),
(21, 'El valor es de 60000 pesos', '2026-05-05 16:53:27.000000', 5, 44),
(22, 'Le recomiendo traerla a revision', '2026-09-10 07:06:54.000000', 5, 44),
(23, 'Claro, con gusto le ayudo', '2026-01-04 14:55:44.000000', 6, 50),
(24, 'Que dia le queda bien?', '2026-06-27 23:09:14.000000', 6, 50),
(25, 'Perfecto, queda agendada', '2026-01-01 23:55:54.000000', 6, 50),
(26, 'Hasta luego', '2026-06-07 03:47:04.000000', 6, 5),
(27, 'Mi mascota no ha querido comer', '2026-05-02 15:17:06.000000', 6, 50),
(28, 'Hola, buenas tardes', '2026-03-14 10:11:17.000000', 6, 5),
(29, 'Buenos dias, necesito informacion sobre vacunas', '2026-04-29 18:55:25.000000', 7, 5),
(30, 'El valor es de 60000 pesos', '2026-07-10 16:41:31.000000', 7, 1),
(31, 'Mi mascota no ha querido comer', '2026-03-10 23:12:01.000000', 8, 3),
(32, 'Quisiera agendar una cita para mi mascota', '2026-02-01 15:50:14.000000', 8, 60),
(33, 'El sabado en la manana', '2026-05-14 11:28:08.000000', 9, 33),
(34, 'El valor es de 60000 pesos', '2026-06-17 11:34:48.000000', 9, 19),
(35, 'El sabado en la manana', '2026-04-30 02:43:09.000000', 9, 19),
(36, 'Listo, muchas gracias', '2026-02-25 05:39:29.000000', 10, 28),
(37, 'Claro, con gusto le ayudo', '2026-08-19 16:58:06.000000', 10, 28),
(38, 'Muchas gracias', '2026-03-03 19:30:44.000000', 10, 45),
(39, 'Con gusto, cualquier cosa me escribe', '2026-06-07 16:52:11.000000', 11, 41),
(40, 'Hasta luego', '2026-03-03 03:17:31.000000', 11, 35),
(41, 'Listo, muchas gracias', '2026-06-05 21:57:56.000000', 11, 41),
(42, 'Que dia le queda bien?', '2026-06-17 05:24:01.000000', 11, 41),
(43, 'Perfecto, queda agendada', '2026-07-22 18:41:40.000000', 12, 6),
(44, 'Cuanto cuesta la consulta?', '2026-06-03 09:28:20.000000', 12, 6),
(45, 'Muchas gracias', '2026-08-20 20:14:29.000000', 13, 59),
(46, 'Quisiera agendar una cita para mi mascota', '2026-08-25 03:42:37.000000', 13, 59),
(47, 'Quisiera agendar una cita para mi mascota', '2026-01-07 02:14:43.000000', 13, 59),
(48, 'Que dia le queda bien?', '2026-07-25 13:33:10.000000', 14, 21),
(49, 'Le recomiendo traerla a revision', '2026-05-13 02:43:48.000000', 14, 21),
(50, 'Quisiera agendar una cita para mi mascota', '2026-04-12 23:31:10.000000', 14, 21),
(51, 'Quisiera agendar una cita para mi mascota', '2026-06-19 04:27:39.000000', 15, 47),
(52, 'Con gusto, cualquier cosa me escribe', '2026-01-26 06:06:58.000000', 15, 47),
(53, 'Hasta luego', '2026-07-22 18:03:48.000000', 15, 55),
(54, 'Muchas gracias', '2026-08-07 06:17:30.000000', 15, 47),
(55, 'Que dia le queda bien?', '2026-04-06 10:00:42.000000', 15, 55),
(56, 'Buenos dias, necesito informacion sobre vacunas', '2026-03-18 01:15:41.000000', 16, 6),
(57, 'Cuanto cuesta la consulta?', '2026-06-01 18:09:50.000000', 16, 16),
(58, 'Buenos dias, necesito informacion sobre vacunas', '2026-09-03 02:13:12.000000', 16, 6),
(59, 'El sabado en la manana', '2026-07-16 04:59:47.000000', 16, 16),
(60, 'Cuanto cuesta la consulta?', '2026-05-08 22:57:54.000000', 17, 30),
(61, 'Muchas gracias', '2026-07-03 17:27:53.000000', 17, 30),
(62, 'Perfecto, queda agendada', '2026-09-15 11:42:33.000000', 17, 40),
(63, 'El sabado en la manana', '2026-04-03 00:56:56.000000', 17, 40),
(64, 'Le recomiendo traerla a revision', '2026-04-23 01:04:00.000000', 17, 40),
(65, 'Listo, muchas gracias', '2026-08-30 19:16:33.000000', 18, 43),
(66, 'Listo, muchas gracias', '2026-04-30 05:48:42.000000', 18, 48),
(67, 'Que dia le queda bien?', '2026-05-04 09:42:14.000000', 18, 43),
(68, 'Listo, muchas gracias', '2026-04-23 20:57:37.000000', 18, 43),
(69, 'ok', '2026-09-15 16:05:14.000000', 3, 1),
(70, 'hola', '2026-09-15 18:28:11.000000', 1, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `raza`
--

CREATE TABLE `raza` (
  `idRaza` int(11) NOT NULL,
  `nombreRaza` varchar(45) NOT NULL,
  `idEspecie` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `raza`
--

INSERT INTO `raza` (`idRaza`, `nombreRaza`, `idEspecie`) VALUES
(1, 'Labrador', 1),
(2, 'Golden Retriever', 1),
(3, 'Persa', 2),
(63, 'Pastor Alemán', 1),
(64, 'Bulldog Francés', 1),
(65, 'Bulldog Inglés', 1),
(66, 'Beagle', 1),
(67, 'Pug', 1),
(68, 'Shih Tzu', 1),
(69, 'Rottweiler', 1),
(70, 'Doberman', 1),
(71, 'Boxer', 1),
(72, 'Husky Siberiano', 1),
(73, 'Border Collie', 1),
(74, 'Chihuahua', 1),
(75, 'Dálmata', 1),
(76, 'Mestizo Canino', 1),
(77, 'Maine Coon', 2),
(78, 'Angora', 2),
(79, 'Bengalí', 2),
(80, 'Ragdoll', 2),
(81, 'British Shorthair', 2),
(82, 'Sphynx', 2),
(83, 'Azul Ruso', 2),
(84, 'Mestizo Felino', 2),
(85, 'Periquito', 3),
(86, 'Canario', 3),
(87, 'Cacatúa', 3),
(88, 'Guacamaya', 3),
(89, 'Agapornis', 3),
(90, 'Ninfa', 3),
(91, 'Loro Amazónico', 3),
(92, 'Mini Lop', 4),
(93, 'Belier', 4),
(94, 'Cabeza de León', 4),
(95, 'Holandés', 4),
(96, 'Gigante de Flandes', 4),
(97, 'Sirio', 5),
(98, 'Ruso', 5),
(99, 'Roborovski', 5),
(100, 'Chino', 5),
(101, 'Americano', 6),
(102, 'Peruano', 6),
(103, 'Abisinio', 6),
(104, 'Silkie', 6),
(105, 'Betta', 7),
(106, 'Goldfish', 7),
(107, 'Guppy', 7),
(108, 'Molly', 7),
(109, 'Tetra Neón', 7),
(110, 'Tortuga de Orejas Rojas', 8),
(111, 'Tortuga Mora', 8),
(112, 'Tortuga Leopardo', 8),
(113, 'Iguana Verde', 9),
(114, 'Iguana Azul', 9),
(115, 'Hurón Doméstico', 10),
(116, 'Pitón Bola', 11),
(117, 'Boa Constrictor', 11),
(118, 'Serpiente del Maíz', 11),
(119, 'Erizo Africano', 12);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `recepcionista`
--

CREATE TABLE `recepcionista` (
  `idUsuario` int(11) NOT NULL,
  `fechaContratacion` date NOT NULL,
  `turno` varchar(10) NOT NULL,
  `idVeterinaria` int(11) NOT NULL,
  `esPrincipal` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `recepcionista`
--

INSERT INTO `recepcionista` (`idUsuario`, `fechaContratacion`, `turno`, `idVeterinaria`, `esPrincipal`) VALUES
(3, '2026-01-15', 'MAÑANA', 1, 1),
(6, '2026-06-16', 'MAÑANA', 2, 1),
(11, '2026-06-17', 'MAÑANA', 4, 1),
(13, '2026-06-20', 'MAÑANA', 5, 1),
(15, '2026-09-04', 'MAÑANA', 6, 1),
(57, '2025-12-08', 'MANANA', 3, 1),
(58, '2025-09-26', 'MANANA', 7, 1),
(59, '2026-03-29', 'MANANA', 8, 1),
(60, '2026-07-29', 'TARDE', 9, 1),
(61, '2025-04-25', 'MANANA', 10, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `reseña`
--

CREATE TABLE `reseña` (
  `idReseña` int(11) NOT NULL,
  `fechaPublicacion` date NOT NULL,
  `comentario` longtext DEFAULT NULL,
  `idCalificacion` int(11) NOT NULL,
  `idVeterinaria` int(11) NOT NULL,
  `Tutor_idUsuario` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `reseña`
--

INSERT INTO `reseña` (`idReseña`, `fechaPublicacion`, `comentario`, `idCalificacion`, `idVeterinaria`, `Tutor_idUsuario`) VALUES
(1, '2026-06-11', 'Muy buena atencion y servicio', 1, 1, 1),
(2, '2026-06-11', 'Muy buena atencion y servicio', 1, 1, 1),
(3, '2026-01-31', 'Precios un poco altos pero vale la pena', 2, 9, 26),
(4, '2026-03-28', 'Instalaciones limpias y personal capacitado', 3, 5, 27),
(5, '2026-05-17', 'El veterinario fue muy claro con el diagnostico', 3, 2, 17),
(6, '2026-07-30', 'Excelente atencion, el personal es muy amable', 2, 3, 17),
(7, '2026-05-08', 'Muy profesionales, mi mascota salio muy bien atendida', 3, 6, 24),
(8, '2026-07-21', 'Atencion rapida en emergencia, muy agradecido', 1, 10, 26),
(9, '2026-08-01', 'Volveria a llevar a mi mascota sin duda', 2, 2, 12),
(10, '2026-07-26', 'El veterinario fue muy claro con el diagnostico', 3, 5, 32),
(11, '2026-08-21', 'Atencion rapida en emergencia, muy agradecido', 2, 10, 1),
(12, '2026-02-17', 'Atencion rapida en emergencia, muy agradecido', 3, 6, 10),
(13, '2026-07-23', 'El servicio es regular, se puede mejorar', 2, 4, 40),
(14, '2026-05-18', 'Instalaciones limpias y personal capacitado', 2, 6, 44),
(15, '2026-03-08', 'Precios un poco altos pero vale la pena', 2, 6, 9),
(16, '2026-01-24', 'Buena veterinaria, aunque la espera fue larga', 3, 8, 5),
(17, '2026-03-05', 'Muy profesionales, mi mascota salio muy bien atendida', 2, 8, 1),
(18, '2026-08-08', 'El servicio es regular, se puede mejorar', 3, 3, 28),
(19, '2026-03-23', 'Los mejores precios de la zona y muy buena atencion', 1, 6, 25),
(20, '2026-02-12', 'Instalaciones limpias y personal capacitado', 3, 3, 9),
(21, '2026-07-22', 'Los mejores precios de la zona y muy buena atencion', 3, 4, 36),
(22, '2026-02-16', 'Buena veterinaria, aunque la espera fue larga', 1, 4, 39),
(23, '2026-02-09', 'Muy profesionales, mi mascota salio muy bien atendida', 3, 4, 42),
(24, '2026-08-16', 'Excelente atencion, el personal es muy amable', 1, 6, 16),
(25, '2026-08-05', 'Muy profesionales, mi mascota salio muy bien atendida', 2, 2, 23),
(26, '2026-07-16', 'Buena veterinaria, aunque la espera fue larga', 3, 2, 14),
(27, '2026-06-10', 'Instalaciones limpias y personal capacitado', 2, 3, 33),
(28, '2026-03-10', 'Muy profesionales, mi mascota salio muy bien atendida', 1, 9, 45),
(29, '2026-01-05', 'Precios un poco altos pero vale la pena', 3, 3, 37),
(30, '2026-06-29', 'El servicio es regular, se puede mejorar', 3, 3, 35),
(31, '2026-08-15', 'El servicio es regular, se puede mejorar', 1, 2, 17),
(32, '2026-03-05', 'Precios un poco altos pero vale la pena', 3, 7, 31),
(33, '2026-08-08', 'Instalaciones limpias y personal capacitado', 1, 4, 26),
(34, '2026-02-13', 'El servicio es regular, se puede mejorar', 3, 10, 47),
(35, '2026-05-26', 'Excelente atencion, el personal es muy amable', 3, 5, 22),
(36, '2026-04-07', 'Volveria a llevar a mi mascota sin duda', 2, 1, 24),
(37, '2026-09-11', 'Volveria a llevar a mi mascota sin duda', 1, 4, 40);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `servicio`
--

CREATE TABLE `servicio` (
  `idServicio` int(11) NOT NULL,
  `nombreServicio` varchar(45) NOT NULL,
  `descripcion` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `servicio`
--

INSERT INTO `servicio` (`idServicio`, `nombreServicio`, `descripcion`) VALUES
(1, 'Consulta General', 'Consulta veterinaria'),
(2, 'Vacunacion', 'Aplicacion de vacunas'),
(3, 'Desparasitación', 'Control de parásitos internos y externos'),
(4, 'Urgencias 24 Horas', 'Atención de emergencias veterinarias'),
(5, 'Cirugía General', 'Procedimientos quirúrgicos de rutina'),
(6, 'Esterilización', 'Control reproductivo de mascotas'),
(7, 'Hospitalización', 'Internación y monitoreo clínico'),
(8, 'Laboratorio Clínico', 'Exámenes de sangre y laboratorio'),
(9, 'Radiografía', 'Estudios diagnósticos por imágenes'),
(10, 'Ecografía', 'Diagnóstico por ultrasonido'),
(11, 'Odontología Veterinaria', 'Limpieza y tratamientos dentales'),
(12, 'Dermatología', 'Diagnóstico y tratamiento de piel'),
(13, 'Cardiología', 'Diagnóstico cardiovascular'),
(14, 'Neurología', 'Tratamientos neurológicos'),
(15, 'Fisioterapia', 'Rehabilitación física'),
(16, 'Peluquería Canina', 'Baño y estética'),
(17, 'Hotel para Mascotas', 'Alojamiento temporal'),
(18, 'Guardería', 'Cuidado por horas'),
(19, 'Venta de Medicamentos', 'Farmacia veterinaria'),
(20, 'Venta de Alimentos', 'Nutrición y concentrados'),
(21, 'Microchip', 'Identificación electrónica'),
(22, 'Certificado de Viaje', 'Documentación para transporte'),
(23, 'Eutanasia', 'Procedimiento humanitario');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `servicio_veterinaria`
--

CREATE TABLE `servicio_veterinaria` (
  `idServicio` int(11) NOT NULL,
  `idVeterinaria` int(11) NOT NULL,
  `precio` double NOT NULL,
  `duracionAprox` int(10) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `servicio_veterinaria`
--

INSERT INTO `servicio_veterinaria` (`idServicio`, `idVeterinaria`, `precio`, `duracionAprox`) VALUES
(1, 1, 60000, 30),
(1, 3, 35000, 30),
(1, 5, 80000, 60),
(1, 6, 70000, 45),
(1, 9, 150000, 20),
(1, 10, 35000, 45),
(2, 1, 45000, 20),
(2, 2, 200000, 15),
(2, 6, 150000, 25),
(2, 9, 90000, 30),
(4, 5, 80000, 30),
(4, 10, 60000, 15),
(5, 1, 120000, 20),
(5, 3, 80000, 30),
(5, 7, 70000, 30),
(5, 9, 90000, 60),
(6, 3, 60000, 25),
(6, 5, 45000, 30),
(6, 6, 80000, 30),
(6, 7, 150000, 45),
(7, 2, 80000, 45),
(7, 4, 60000, 15),
(7, 5, 50000, 25),
(7, 6, 90000, 15),
(7, 7, 120000, 15),
(7, 8, 200000, 45),
(7, 9, 45000, 30),
(8, 3, 200000, 60),
(8, 4, 70000, 25),
(8, 9, 100000, 45),
(9, 2, 60000, 20),
(9, 5, 150000, 45),
(9, 6, 200000, 25),
(10, 2, 50000, 20),
(10, 4, 70000, 15),
(11, 5, 100000, 60),
(12, 2, 200000, 20),
(12, 4, 90000, 25),
(12, 6, 90000, 60),
(13, 4, 500000, 25),
(13, 5, 150000, 25),
(13, 7, 60000, 30),
(14, 3, 45000, 30),
(14, 6, 200000, 15),
(14, 7, 70000, 25),
(14, 10, 100000, 20),
(15, 4, 200000, 20),
(15, 8, 50000, 60),
(16, 8, 60000, 60),
(17, 6, 70000, 60),
(17, 8, 80000, 15),
(18, 1, 90000, 20),
(18, 5, 150000, 30),
(18, 7, 150000, 45),
(20, 3, 50000, 60),
(21, 10, 60000, 20),
(22, 5, 35000, 15),
(22, 8, 120000, 60),
(23, 1, 80000, 45),
(23, 3, 50000, 60),
(23, 6, 90000, 25),
(23, 8, 45000, 25),
(23, 9, 90000, 20),
(23, 10, 120000, 30);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `telefono`
--

CREATE TABLE `telefono` (
  `idTelefono` int(11) NOT NULL,
  `idUsuario` int(11) NOT NULL,
  `numeroTelefono` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `telefono`
--

INSERT INTO `telefono` (`idTelefono`, `idUsuario`, `numeroTelefono`) VALUES
(1, 1, '3001111111'),
(2, 2, '3002222223'),
(3, 3, '3003333333'),
(4, 4, '3004444444'),
(5, 5, '3238165475'),
(6, 6, '3228825475'),
(7, 7, '3005568575'),
(8, 8, '3002587541'),
(9, 9, '3305864971'),
(10, 10, '3235675475'),
(11, 11, '3001234566'),
(12, 12, '3000000000'),
(13, 13, '3000000025'),
(14, 14, '3000004456'),
(15, 15, '3225868007'),
(16, 16, '3542621108'),
(17, 17, '3768820204'),
(18, 18, '3236696312'),
(19, 19, '3869119330'),
(20, 20, '3749621470'),
(21, 21, '3298362082'),
(22, 22, '3819795579'),
(23, 23, '3099585092'),
(24, 24, '3385451171'),
(25, 25, '3866647391'),
(26, 26, '3575770529'),
(27, 27, '3084611066'),
(28, 28, '3890566476'),
(29, 29, '3756528252'),
(30, 30, '3710026086'),
(31, 31, '3085675980'),
(32, 32, '3408157429'),
(33, 33, '3682560971'),
(34, 34, '3397478786'),
(35, 35, '3719595113'),
(36, 36, '3654049436'),
(37, 37, '3175452091'),
(38, 38, '3289854268'),
(39, 39, '3905005353'),
(40, 40, '3882403818'),
(41, 41, '3430747414'),
(42, 42, '3226541099'),
(43, 43, '3703771909'),
(44, 44, '3949921299'),
(45, 45, '3284412919'),
(46, 46, '3799925830'),
(47, 47, '3802099969'),
(48, 48, '3388690725'),
(49, 49, '3547099690'),
(50, 50, '3811514914'),
(51, 51, '3164112119'),
(52, 52, '3640389325'),
(53, 53, '3409760584'),
(54, 54, '3594021782'),
(55, 55, '3731980933'),
(56, 56, '3825276600'),
(57, 57, '3315143362'),
(58, 58, '3487182120'),
(59, 59, '3537500247'),
(60, 60, '3934712197'),
(61, 61, '3164109919');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `telefono_veterinaria`
--

CREATE TABLE `telefono_veterinaria` (
  `idTelefonoVeterinaria` int(11) NOT NULL,
  `idVeterinaria` int(11) NOT NULL,
  `numeroTelefono` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `telefono_veterinaria`
--

INSERT INTO `telefono_veterinaria` (`idTelefonoVeterinaria`, `idVeterinaria`, `numeroTelefono`) VALUES
(1, 1, '6015551234'),
(2, 2, '3124361492'),
(3, 4, '3215647584'),
(4, 5, '3211111111'),
(5, 6, '3244354534'),
(6, 3, '3740739735'),
(7, 7, '3215984311'),
(8, 8, '3765523129'),
(9, 9, '3334702822'),
(10, 10, '3428414718');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tipo_documento`
--

CREATE TABLE `tipo_documento` (
  `idTipoDocumento` int(11) NOT NULL,
  `nombreTipo` varchar(45) NOT NULL,
  `descripcion` varchar(45) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `tipo_documento`
--

INSERT INTO `tipo_documento` (`idTipoDocumento`, `nombreTipo`, `descripcion`) VALUES
(1, 'CC', 'Cedula de ciudadania'),
(2, 'TI', 'Tarjeta de identidad'),
(3, 'CE', 'Cedula de extranjeria'),
(4, 'PA', 'Pasaporte');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tipo_vacuna`
--

CREATE TABLE `tipo_vacuna` (
  `idTipoVacuna` int(11) NOT NULL,
  `nombreTipo` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `tipo_vacuna`
--

INSERT INTO `tipo_vacuna` (`idTipoVacuna`, `nombreTipo`) VALUES
(1, 'Antirrabica'),
(2, 'Multiple'),
(3, 'Antiparasitaria'),
(4, 'Refuerzo');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tratamiento`
--

CREATE TABLE `tratamiento` (
  `idTratamiento` int(11) NOT NULL,
  `nombreTratamiento` varchar(45) NOT NULL,
  `descripcion` longtext NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `tratamiento`
--

INSERT INTO `tratamiento` (`idTratamiento`, `nombreTratamiento`, `descripcion`) VALUES
(1, 'Desparasitacion', 'Tratamiento preventivo'),
(2, 'Terapia Antibiotica', 'Tratamiento con antibioticos de amplio espectro'),
(3, 'Fisioterapia Canina', 'Rehabilitacion fisica post quirurgica'),
(4, 'Dieta Terapeutica', 'Manejo nutricional para enfermedad renal o digestiva'),
(5, 'Tratamiento Dermatologico', 'Manejo de afecciones cutaneas'),
(6, 'Quimioterapia', 'Tratamiento oncologico'),
(7, 'Terapia de Fluidos', 'Hidratacion intravenosa'),
(8, 'Manejo del Dolor', 'Terapia analgesica y antiinflamatoria'),
(9, 'Control de Peso', 'Plan de ejercicio y dieta para obesidad');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `tutor`
--

CREATE TABLE `tutor` (
  `idUsuario` int(11) NOT NULL,
  `direccion` varchar(45) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `tutor`
--

INSERT INTO `tutor` (`idUsuario`, `direccion`) VALUES
(1, 'Calle 80 # 90-15'),
(5, 'Cra 104#64C-22'),
(8, 'Calle 105 #64c -22'),
(9, 'calle san juan de señor'),
(10, 'cra 109 #65c-22'),
(12, 'calle #80'),
(14, 'calle 3'),
(16, 'Carrera 197C Bis M # 18-2 Este'),
(17, 'Avenida Subachoque # 55-71'),
(18, 'Tr. 148 # 58-91 Este'),
(19, 'Cl. 92 # 60Q-89'),
(20, 'Cl. 44 # 2-2 Este'),
(21, 'Calle 182 # 8-9'),
(22, 'Dg. 46 # 53Y-7 Este'),
(23, 'Carrera 27Q # 6-11 Este'),
(24, 'Carrera 110 Sur # 6-20 Este'),
(25, 'Cr. 93 # 8-4 Sur'),
(26, 'Avenida Ramírez # 7-6 Sur'),
(27, 'Dg. 101 # 6-2'),
(28, 'Carrera 72 # 8-4'),
(29, 'Dg. 181 # 9-1'),
(30, 'Av. calle 33 # 47-7'),
(31, 'Calle 56 # 57Z-7 Este'),
(32, 'Av. carrera 88 # 5-4'),
(33, 'Carrera 6X # 8-9 Este'),
(34, 'Tr. 31 # 2-4 Sur'),
(35, 'Dg. 13 # 96-69 Sur'),
(36, 'Cr. 53 # 8-64'),
(37, 'Cr. 2ª # 4-9 Sur'),
(38, 'Cl. 41 # 8-92 Este'),
(39, 'Tr. 18 # 28-34 Sur'),
(40, 'Calle 38 # 4-69'),
(41, 'Dg. 174Q # 71B-5'),
(42, 'Cr. 5ª # 8-9 Este'),
(43, 'Calle 112 # 95-3 Sur'),
(44, 'Cl. 60 # 2-63 Este'),
(45, 'Avenida Apía # 32X-33 Sur'),
(46, 'Cr. 6ª # 56-33 Este'),
(47, 'Dg. 47 # 8-6 Sur');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `ubicacion_veterinaria`
--

CREATE TABLE `ubicacion_veterinaria` (
  `idUbicacion_Veterinaria` int(11) NOT NULL,
  `calle` varchar(45) NOT NULL,
  `numero` varchar(45) NOT NULL,
  `estrato` int(11) NOT NULL,
  `latitud` double NOT NULL,
  `longitud` double NOT NULL,
  `idVeterinaria` int(11) NOT NULL,
  `idBarrio` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `ubicacion_veterinaria`
--

INSERT INTO `ubicacion_veterinaria` (`idUbicacion_Veterinaria`, `calle`, `numero`, `estrato`, `latitud`, `longitud`, `idVeterinaria`, `idBarrio`) VALUES
(1, 'Calle 80', '95-20', 4, 4.701, -74.112, 1, 1),
(2, 'Avenida Carrera 13', '65-42', 2, 4.6518466, -74.0629427, 4, 23),
(3, 'calle 80', '#15-23', 3, 4.6338355, -74.1521514, 5, 19),
(4, 'carrera 4 este # 48', 'S/N', 3, 4.5430214, -74.0955904, 6, 25),
(5, 'Avenida calle 43', '98-21', 5, 4.7860499, -74.0155132, 2, 24),
(6, 'Calle 6ª', '77-42', 4, 4.505843, -74.0141803, 3, 1),
(7, 'Calle 64', '31-8', 2, 4.7634029, -74.0106101, 7, 20),
(8, 'Cl. 15R', '94-63', 1, 4.7933953, -74.0934588, 8, 6),
(9, 'Tr. 52', '17-85', 4, 4.7840546, -74.1669746, 9, 9),
(10, 'Carrera 6ª', '78-55', 2, 4.778674, -74.048947, 10, 34);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuario`
--

CREATE TABLE `usuario` (
  `idUsuario` int(11) NOT NULL,
  `numeroDocumento` varchar(45) NOT NULL,
  `idTipoDocumento` int(11) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `apellido` varchar(50) NOT NULL,
  `password` varchar(120) NOT NULL,
  `estado` tinyint(4) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `usuario`
--

INSERT INTO `usuario` (`idUsuario`, `numeroDocumento`, `idTipoDocumento`, `nombre`, `apellido`, `password`, `estado`) VALUES
(1, '100000001', 1, 'Maria', 'Diaz', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(2, '100000002', 1, 'Juan', 'Perez', '$2b$10$7GnTAv5AtJYrf7NOKmaAvekpN.H/x7kHclDN0OPxrvLxkyENfG..y', 1),
(3, '100000003', 1, 'Laura', 'Gomez', '$2b$10$cEfDAPYOlpRT2tSotc.CY.wbNz4EZBCSSuWiD1o3yl3xHKP8QAdv2', 1),
(4, '100000004', 1, 'Carlos', 'Rodriguez', '123456', 1),
(5, '1016950095', 1, 'Mar', 'Diaz', '$2y$10$27uwivchKIAW2uovhaPIEusDcsWijNfqgO3hr4zj.5sVpcUGPv7DC', 1),
(6, '11111111', 1, 'Ramiro', 'Torres', '$2y$10$GqzwZcuvIkxDe5DykeLVI.LAN999FTaJMY6oqH6wNcjE2SaI3X4FO', 1),
(7, '12121212', 1, 'Mar', 'Fuentes', '$2y$10$EBCM7Q2fH98t6Kazb52R2uYkr32myfPSgoT4rgGc7rFG8GXpR67f6', 1),
(8, '11164587', 1, 'Alex', 'Celis', '$2y$10$ogZtUj2eo2DGgZ.ouZUL7uce6F9xEQO5L2G6.Ah.LRlNcS.3RIK8S', 1),
(9, '1021613789', 1, 'Armando', 'Maradroga', '$2y$10$fwT2rcFku8daLADgj8JSzuUtEHAYCjbL0anj/15Se/Iec8Bbda8Mu', 1),
(10, '88888888', 1, 'Sofia', 'Lopez', '$2y$10$cSZ7KlQsLX5rbnkYXb/Gu.W9R3odFmYCx5ePIPiBckcAbGu3NsbDa', 1),
(11, '79862250', 1, 'Sara', 'Hernandez', '$2y$10$MAHo7LyACIgUGnturHDRae732VUl1eXYf8PgZFku.53y0k2sCeGti', 1),
(12, '1010101010', 1, 'juan', 'juan', '$2y$10$6OnIyz1FoRYsNupCpuXlbOUGCXLDoQan7jDqmNJabc5L/3UKb2viO', 1),
(13, '11111111111', 1, 'jose', 'jose', '$2y$10$59ihPe3TmFJX887NH.EG5elyx5O0eJwEuPjN7co2QnZDwsll20xXG', 1),
(14, '22222222', 1, 'carles', 'puyol', '$2a$10$C7vg56EZlRn5msVn9RsEre0X7tfWi9g.HZvme32wjlGu2aefqyBtC', 1),
(15, '22222224', 1, 'lola', 'lolita', '$2a$10$qOcztrLOJ8fNlHmlBVLxZ.XwQIybOXobXVFBjOY/mJe6D1rO3D07e', 1),
(16, '479521477', 1, 'Jorge', 'Rodríguez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(17, '66985561', 1, 'Esperanza', 'Torres', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(18, '1180252923', 1, 'Felipe', 'Buitrago', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(19, '974669077', 1, 'Stiven', 'García', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(20, '23955983', 1, 'Rubiela', 'Gómez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(21, '917557512', 1, 'Marina', 'Mora', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(22, '343889688', 1, 'María', 'Ramírez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(23, '732831292', 1, 'Jesús', 'Vásquez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(24, '825887678', 1, 'Marina', 'Fernández', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(25, '748639288', 1, 'Edwin', 'Rodríguez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(26, '103309105', 1, 'Mario', 'Benavides', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(27, '278062140', 1, 'Ruth', 'Sánchez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(28, '1195498232', 1, 'René', 'Quintero', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(29, '786605304', 1, 'Andrea', 'López', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(30, '159368553', 1, 'Edgar', 'Agudelo', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(31, '499407815', 1, 'Mario', 'Ariza', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(32, '509914620', 1, 'Andrés', 'Cardozo', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(33, '606943772', 1, 'Myriam', 'Caicedo', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(34, '793471136', 1, 'Álvaro', 'Beltrán', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(35, '772938025', 1, 'Nelson', 'Escobar', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(36, '583330498', 1, 'Diego', 'Gómez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(37, '377517440', 1, 'Ángela', 'Gutiérrez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(38, '1002696249', 1, 'Ana', 'Moreno', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(39, '481619987', 1, 'Diana', 'Castro', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(40, '130125830', 1, 'Fernando', 'Morales', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(41, '78934737', 1, 'Eliana', 'Rojas', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(42, '584969167', 1, 'Mónica', 'Cuesta', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(43, '685765611', 1, 'Jesús', 'León', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(44, '1082090968', 1, 'Carolina', 'Padilla', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(45, '995376852', 1, 'Claudia', 'Rivera', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(46, '309853838', 1, 'Edisson', 'Bedoya', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(47, '1167444916', 1, 'Luis', 'Duque', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(48, '930054627', 1, 'Héctor', 'Guevara', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(49, '480987740', 1, 'Ángela', 'Gómez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(50, '1069817199', 1, 'Valentina', 'Ortiz', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(51, '111181642', 1, 'Laura', 'Valderrama', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(52, '353558720', 1, 'Germán', 'Álvarez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(53, '146424712', 1, 'Jesús', 'Medina', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(54, '1015129473', 1, 'Néstor', 'Cruz', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(55, '34655304', 1, 'Mónica', 'Vargas', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(56, '1163135002', 1, 'Luis', 'Muñoz', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(57, '740521271', 1, 'Juan', 'Burbano', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(58, '943651277', 1, 'Pilar', 'Torres', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(59, '16969260', 1, 'Leider', 'Palacios', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(60, '393651994', 1, 'Andrea', 'Gómez', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1),
(61, '650905300', 1, 'Sandra', 'Pineda', '$2b$10$vUaMe94AgdlZI0eGM6g.yuM8V86Sv9WO3SPLSPFICWU9Td5aC4el.', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `vacuna`
--

CREATE TABLE `vacuna` (
  `idVacuna` int(11) NOT NULL,
  `nombreVacuna` varchar(45) NOT NULL,
  `descripcion` text DEFAULT NULL,
  `idEspecie` int(11) NOT NULL,
  `idFabricanteVacuna` int(11) NOT NULL,
  `idTipoVacuna` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `vacuna`
--

INSERT INTO `vacuna` (`idVacuna`, `nombreVacuna`, `descripcion`, `idEspecie`, `idFabricanteVacuna`, `idTipoVacuna`) VALUES
(1, 'Rabia', 'Vacuna contra la rabia', 1, 1, 1),
(2, 'Sextuple Canina', 'Vacuna multiple contra 6 enfermedades caninas', 1, 1, 2),
(3, 'Parvovirus', 'Proteccion contra parvovirus canino', 1, 2, 2),
(4, 'Triple Felina', 'Vacuna multiple para gatos', 2, 1, 2),
(5, 'Leucemia Felina', 'Proteccion contra leucemia viral felina', 2, 2, 2),
(6, 'Rabia Felina', 'Vacuna antirrabica para gatos', 2, 1, 1),
(7, 'Moquillo', 'Proteccion contra el moquillo canino', 1, 3, 2),
(8, 'Bordetella', 'Proteccion respiratoria canina', 1, 4, 4),
(9, 'Polivalente Aviar', 'Vacuna multiple para aves', 3, 5, 2),
(10, 'Mixomatosis', 'Vacuna para conejos', 4, 4, 2),
(11, 'Antiparasitaria Canina', 'Desparasitacion interna', 1, 3, 3),
(12, 'Antiparasitaria Felina', 'Desparasitacion interna felina', 2, 3, 3),
(13, 'Refuerzo Antirrabico', 'Refuerzo anual antirrabico', 1, 1, 4),
(14, 'Coriza Felina', 'Proteccion respiratoria felina', 2, 5, 2),
(15, 'Hepatitis Canina', 'Proteccion contra hepatitis infecciosa canina', 1, 2, 2);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `veterinaria`
--

CREATE TABLE `veterinaria` (
  `idVeterinaria` int(11) NOT NULL,
  `nit` varchar(45) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `logo` varchar(100) NOT NULL,
  `descripcion` longtext DEFAULT NULL,
  `paginaWeb` varchar(50) DEFAULT NULL,
  `estado` tinyint(4) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `veterinaria`
--

INSERT INTO `veterinaria` (`idVeterinaria`, `nit`, `nombre`, `logo`, `descripcion`, `paginaWeb`, `estado`) VALUES
(1, '901123456', 'VetLink', 'logo.png', 'Centro veterinario integral', 'www.vetlink.com', 1),
(2, '900800457', 'Vetlink2', 'default-logo.png', 'La mejor veterinaria de Bogota', 'www.vetlink2.com', 1),
(3, '9007005475', 'Puppy', 'default-logo.png', 'Lo mejor para las mascotas', 'www.puppy.com', 1),
(4, '900000564', 'vetplus', 'default-logo.png', '---', 'www.vetplus.com', 1),
(5, '900000456', 'clinica don jose', 'default-logo.png', 'clinica de prueba de don jose', '', 1),
(6, '943824724', 'vetlolita', 'default-logo.png', 'vet de prueba ', '', 1),
(7, '901234567', 'Huellitas Felices', 'default-logo.png', 'Atencion veterinaria de confianza', 'www.huellitasfelices.com', 1),
(8, '900987654', 'Pet Care Bogota', 'default-logo.png', 'Clinica veterinaria integral 24 horas', 'www.petcarebogota.com', 1),
(9, '901555222', 'Veterinaria San Francisco', 'default-logo.png', 'Cuidado veterinario con mas de 10 anios de experiencia', 'www.vetsanfrancisco.com', 1),
(10, '900333111', 'AnimalVet Express', 'default-logo.png', 'Servicio veterinario rapido y confiable', 'www.animalvetexpress.com', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `veterinario`
--

CREATE TABLE `veterinario` (
  `idUsuario` int(11) NOT NULL,
  `licencia` varchar(45) NOT NULL,
  `idEspecialidad` int(11) DEFAULT NULL,
  `idVeterinaria` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

--
-- Volcado de datos para la tabla `veterinario`
--

INSERT INTO `veterinario` (`idUsuario`, `licencia`, `idEspecialidad`, `idVeterinaria`) VALUES
(2, 'VET001', 1, 1),
(48, 'VET002', 2, 9),
(49, 'VET003', 3, 5),
(50, 'VET004', 2, 10),
(51, 'VET005', 2, 6),
(52, 'VET006', 5, 5),
(53, 'VET007', 3, 8),
(54, 'VET008', 1, 1),
(55, 'VET009', 7, 7),
(56, 'VET010', 1, 5);

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `archivo_historial`
--
ALTER TABLE `archivo_historial`
  ADD PRIMARY KEY (`idArchivo`),
  ADD KEY `fk_Archivo_Historial_Historial_Clinico1_idx` (`numeroHistorial`);

--
-- Indices de la tabla `barrio`
--
ALTER TABLE `barrio`
  ADD PRIMARY KEY (`idBarrio`),
  ADD KEY `fk_Barrio_Localidad1_idx` (`idLocalidad`);

--
-- Indices de la tabla `calificacion`
--
ALTER TABLE `calificacion`
  ADD PRIMARY KEY (`idCalificacion`),
  ADD UNIQUE KEY `descripcion_UNIQUE` (`descripcion`);

--
-- Indices de la tabla `carnet_vacuna`
--
ALTER TABLE `carnet_vacuna`
  ADD PRIMARY KEY (`idCarnet`),
  ADD KEY `fk_Carnet_Vacuna_Lote_Vacuna1_idx` (`idLote`),
  ADD KEY `fk_Carnet_Vacuna_Veterinario1_idx` (`idUsuario`),
  ADD KEY `fk_Carnet_Vacuna_Vacuna1_idx` (`idVacuna`),
  ADD KEY `fk_Carnet_Vacuna_Mascota1_idx` (`idMascota`);

--
-- Indices de la tabla `cita`
--
ALTER TABLE `cita`
  ADD PRIMARY KEY (`idCita`),
  ADD KEY `fk_Cita_Veterinario1_idx` (`Veterinario_idUsuario`),
  ADD KEY `fk_Cita_Servicio1_idx` (`Servicio_idServicio`),
  ADD KEY `fk_Cita_Veterinaria1_idx` (`Veterinaria_idVeterinaria`),
  ADD KEY `fk_Cita_Mascota1_idx` (`idMascota`);

--
-- Indices de la tabla `ciudad`
--
ALTER TABLE `ciudad`
  ADD PRIMARY KEY (`idCiudad`),
  ADD UNIQUE KEY `nombre_UNIQUE` (`nombre`);

--
-- Indices de la tabla `conversacion`
--
ALTER TABLE `conversacion`
  ADD PRIMARY KEY (`idConversacion`),
  ADD KEY `FKowqauh3qmilfylhyfc61lbvsb` (`usuarioA`),
  ADD KEY `FKc5y9ufbuos7cw4sdtjf6c4y27` (`usuarioB`);

--
-- Indices de la tabla `correo`
--
ALTER TABLE `correo`
  ADD PRIMARY KEY (`idCorreo`),
  ADD UNIQUE KEY `correo_UNIQUE` (`correo`),
  ADD KEY `fk_Correo_Usuario1_idx` (`idUsuario`);

--
-- Indices de la tabla `correo_veterinaria`
--
ALTER TABLE `correo_veterinaria`
  ADD PRIMARY KEY (`idCorreo_Veterinaria`),
  ADD KEY `fk_Correo_Veterinaria_Veterinaria1_idx` (`idVeterinaria`);

--
-- Indices de la tabla `dia_semana`
--
ALTER TABLE `dia_semana`
  ADD PRIMARY KEY (`idDiaSemana`),
  ADD UNIQUE KEY `diaSemana_UNIQUE` (`diaSemana`);

--
-- Indices de la tabla `emergencia`
--
ALTER TABLE `emergencia`
  ADD PRIMARY KEY (`idEmergencia`),
  ADD KEY `FKlvpgbv2d0wtc94an3bkqi27po` (`idVeterinaria`),
  ADD KEY `FK3bdit2mor7eavvyyafiw7g6e4` (`idVeterinario`);

--
-- Indices de la tabla `especialidad_veterinario`
--
ALTER TABLE `especialidad_veterinario`
  ADD PRIMARY KEY (`idEspecialidadVeterinario`),
  ADD UNIQUE KEY `nombreEspecialidad_UNIQUE` (`nombreEspecialidad`);

--
-- Indices de la tabla `especie`
--
ALTER TABLE `especie`
  ADD PRIMARY KEY (`idEspecie`);

--
-- Indices de la tabla `fabricante_vacuna`
--
ALTER TABLE `fabricante_vacuna`
  ADD PRIMARY KEY (`idFabricante_Vacuna`);

--
-- Indices de la tabla `historial_clinico`
--
ALTER TABLE `historial_clinico`
  ADD PRIMARY KEY (`numeroHistorial`),
  ADD KEY `fk_Historial_Clinico_Veterinario1_idx` (`Veterinario_idUsuario`),
  ADD KEY `fk_Historial_Clinico_Mascota1_idx` (`idMascota`);

--
-- Indices de la tabla `historial_medicamento`
--
ALTER TABLE `historial_medicamento`
  ADD PRIMARY KEY (`Historial_Clinico_numeroHistorial`,`Medicamento_idMedicamento`),
  ADD KEY `fk_Historial_Clinico_has_Medicamento_Medicamento1_idx` (`Medicamento_idMedicamento`),
  ADD KEY `fk_Historial_Clinico_has_Medicamento_Historial_Clinico1_idx` (`Historial_Clinico_numeroHistorial`);

--
-- Indices de la tabla `historial_tratamiento`
--
ALTER TABLE `historial_tratamiento`
  ADD PRIMARY KEY (`Tratamiento_idTratamiento`,`Historial_Clinico_numeroHistorial`),
  ADD KEY `fk_Tratamiento_has_Historial_Clinico_Historial_Clinico1_idx` (`Historial_Clinico_numeroHistorial`),
  ADD KEY `fk_Tratamiento_has_Historial_Clinico_Tratamiento1_idx` (`Tratamiento_idTratamiento`);

--
-- Indices de la tabla `horario_veterinaria`
--
ALTER TABLE `horario_veterinaria`
  ADD PRIMARY KEY (`idHorario_Veterinaria`),
  ADD KEY `fk_Horario_Veterinaria_Dia_Semana1_idx` (`idDiaSemana`),
  ADD KEY `fk_Horario_Veterinaria_Veterinaria1_idx` (`idVeterinaria`);

--
-- Indices de la tabla `localidad`
--
ALTER TABLE `localidad`
  ADD PRIMARY KEY (`idLocalidad`),
  ADD KEY `fk_Localidad_Ciudad1_idx` (`idCiudad`);

--
-- Indices de la tabla `lote_vacuna`
--
ALTER TABLE `lote_vacuna`
  ADD PRIMARY KEY (`idLote`);

--
-- Indices de la tabla `mascota`
--
ALTER TABLE `mascota`
  ADD PRIMARY KEY (`idMascota`),
  ADD KEY `fk_Mascota_Especie1_idx` (`idEspecie`),
  ADD KEY `fk_Mascota_Raza1_idx` (`idRaza`),
  ADD KEY `fk_Mascota_Tutor1_idx` (`Tutor_idUsuario`);

--
-- Indices de la tabla `medicamento`
--
ALTER TABLE `medicamento`
  ADD PRIMARY KEY (`idMedicamento`);

--
-- Indices de la tabla `mensaje_chat`
--
ALTER TABLE `mensaje_chat`
  ADD PRIMARY KEY (`idMensaje`),
  ADD KEY `FKh6onu9cuw5kucuskr3mpuupfc` (`idConversacion`),
  ADD KEY `FK6lmsyngqhj9brc50yxofopvnb` (`idRemitente`);

--
-- Indices de la tabla `raza`
--
ALTER TABLE `raza`
  ADD PRIMARY KEY (`idRaza`),
  ADD UNIQUE KEY `nombreRaza_UNIQUE` (`nombreRaza`),
  ADD KEY `fk_raza_especie` (`idEspecie`);

--
-- Indices de la tabla `recepcionista`
--
ALTER TABLE `recepcionista`
  ADD PRIMARY KEY (`idUsuario`),
  ADD KEY `fk_Recepcionista_Veterinaria1_idx` (`idVeterinaria`);

--
-- Indices de la tabla `reseña`
--
ALTER TABLE `reseña`
  ADD PRIMARY KEY (`idReseña`),
  ADD KEY `fk_Reseña_Calificacion1_idx` (`idCalificacion`),
  ADD KEY `fk_Reseña_Veterinaria1_idx` (`idVeterinaria`),
  ADD KEY `fk_Reseña_Tutor1_idx` (`Tutor_idUsuario`);

--
-- Indices de la tabla `servicio`
--
ALTER TABLE `servicio`
  ADD PRIMARY KEY (`idServicio`),
  ADD UNIQUE KEY `nombreServicio_UNIQUE` (`nombreServicio`);

--
-- Indices de la tabla `servicio_veterinaria`
--
ALTER TABLE `servicio_veterinaria`
  ADD PRIMARY KEY (`idServicio`,`idVeterinaria`),
  ADD KEY `fk_Servicio_Veterinaria_Veterinaria1_idx` (`idVeterinaria`);

--
-- Indices de la tabla `telefono`
--
ALTER TABLE `telefono`
  ADD PRIMARY KEY (`idTelefono`),
  ADD UNIQUE KEY `telefono_UNIQUE` (`numeroTelefono`),
  ADD KEY `fk_Telefono_Usuario1_idx` (`idUsuario`);

--
-- Indices de la tabla `telefono_veterinaria`
--
ALTER TABLE `telefono_veterinaria`
  ADD PRIMARY KEY (`idTelefonoVeterinaria`),
  ADD KEY `fk_Telefono_Veterinaria_Veterinaria1_idx` (`idVeterinaria`);

--
-- Indices de la tabla `tipo_documento`
--
ALTER TABLE `tipo_documento`
  ADD PRIMARY KEY (`idTipoDocumento`),
  ADD UNIQUE KEY `nombreTipo_UNIQUE` (`nombreTipo`);

--
-- Indices de la tabla `tipo_vacuna`
--
ALTER TABLE `tipo_vacuna`
  ADD PRIMARY KEY (`idTipoVacuna`);

--
-- Indices de la tabla `tratamiento`
--
ALTER TABLE `tratamiento`
  ADD PRIMARY KEY (`idTratamiento`),
  ADD UNIQUE KEY `nombreTratamiento_UNIQUE` (`nombreTratamiento`);

--
-- Indices de la tabla `tutor`
--
ALTER TABLE `tutor`
  ADD PRIMARY KEY (`idUsuario`),
  ADD KEY `fk_Tutor_Usuario1_idx` (`idUsuario`);

--
-- Indices de la tabla `ubicacion_veterinaria`
--
ALTER TABLE `ubicacion_veterinaria`
  ADD PRIMARY KEY (`idUbicacion_Veterinaria`),
  ADD KEY `fk_Ubicacion_Veterinaria_Veterinaria1_idx` (`idVeterinaria`),
  ADD KEY `fk_Ubicacion_Veterinaria_Barrio1_idx` (`idBarrio`);

--
-- Indices de la tabla `usuario`
--
ALTER TABLE `usuario`
  ADD PRIMARY KEY (`idUsuario`),
  ADD UNIQUE KEY `numeroDocumento_UNIQUE` (`numeroDocumento`),
  ADD KEY `fk_Usuario_Tipo_Documento1_idx` (`idTipoDocumento`);

--
-- Indices de la tabla `vacuna`
--
ALTER TABLE `vacuna`
  ADD PRIMARY KEY (`idVacuna`),
  ADD KEY `fk_Vacuna_Especie1_idx` (`idEspecie`),
  ADD KEY `fk_Vacuna_Fabricante_Vacuna1_idx` (`idFabricanteVacuna`),
  ADD KEY `fk_Vacuna_Tipo_Vacuna1_idx` (`idTipoVacuna`);

--
-- Indices de la tabla `veterinaria`
--
ALTER TABLE `veterinaria`
  ADD PRIMARY KEY (`idVeterinaria`),
  ADD UNIQUE KEY `nit_UNIQUE` (`nit`),
  ADD UNIQUE KEY `nombre_UNIQUE` (`nombre`);

--
-- Indices de la tabla `veterinario`
--
ALTER TABLE `veterinario`
  ADD PRIMARY KEY (`idUsuario`),
  ADD UNIQUE KEY `licencia_UNIQUE` (`licencia`),
  ADD KEY `fk_Veterinario_Especialidad_Veterinario1_idx` (`idEspecialidad`),
  ADD KEY `fk_Veterinario_Veterinaria1_idx` (`idVeterinaria`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `archivo_historial`
--
ALTER TABLE `archivo_historial`
  MODIFY `idArchivo` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=88;

--
-- AUTO_INCREMENT de la tabla `barrio`
--
ALTER TABLE `barrio`
  MODIFY `idBarrio` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=43;

--
-- AUTO_INCREMENT de la tabla `calificacion`
--
ALTER TABLE `calificacion`
  MODIFY `idCalificacion` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `carnet_vacuna`
--
ALTER TABLE `carnet_vacuna`
  MODIFY `idCarnet` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=74;

--
-- AUTO_INCREMENT de la tabla `cita`
--
ALTER TABLE `cita`
  MODIFY `idCita` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=366;

--
-- AUTO_INCREMENT de la tabla `ciudad`
--
ALTER TABLE `ciudad`
  MODIFY `idCiudad` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT de la tabla `conversacion`
--
ALTER TABLE `conversacion`
  MODIFY `idConversacion` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT de la tabla `correo`
--
ALTER TABLE `correo`
  MODIFY `idCorreo` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

--
-- AUTO_INCREMENT de la tabla `correo_veterinaria`
--
ALTER TABLE `correo_veterinaria`
  MODIFY `idCorreo_Veterinaria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT de la tabla `dia_semana`
--
ALTER TABLE `dia_semana`
  MODIFY `idDiaSemana` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT de la tabla `emergencia`
--
ALTER TABLE `emergencia`
  MODIFY `idEmergencia` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT de la tabla `especialidad_veterinario`
--
ALTER TABLE `especialidad_veterinario`
  MODIFY `idEspecialidadVeterinario` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT de la tabla `especie`
--
ALTER TABLE `especie`
  MODIFY `idEspecie` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT de la tabla `fabricante_vacuna`
--
ALTER TABLE `fabricante_vacuna`
  MODIFY `idFabricante_Vacuna` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT de la tabla `historial_clinico`
--
ALTER TABLE `historial_clinico`
  MODIFY `numeroHistorial` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=278;

--
-- AUTO_INCREMENT de la tabla `horario_veterinaria`
--
ALTER TABLE `horario_veterinaria`
  MODIFY `idHorario_Veterinaria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=58;

--
-- AUTO_INCREMENT de la tabla `localidad`
--
ALTER TABLE `localidad`
  MODIFY `idLocalidad` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=34;

--
-- AUTO_INCREMENT de la tabla `lote_vacuna`
--
ALTER TABLE `lote_vacuna`
  MODIFY `idLote` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT de la tabla `mascota`
--
ALTER TABLE `mascota`
  MODIFY `idMascota` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=64;

--
-- AUTO_INCREMENT de la tabla `medicamento`
--
ALTER TABLE `medicamento`
  MODIFY `idMedicamento` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT de la tabla `mensaje_chat`
--
ALTER TABLE `mensaje_chat`
  MODIFY `idMensaje` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=71;

--
-- AUTO_INCREMENT de la tabla `raza`
--
ALTER TABLE `raza`
  MODIFY `idRaza` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=120;

--
-- AUTO_INCREMENT de la tabla `reseña`
--
ALTER TABLE `reseña`
  MODIFY `idReseña` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=38;

--
-- AUTO_INCREMENT de la tabla `servicio`
--
ALTER TABLE `servicio`
  MODIFY `idServicio` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=24;

--
-- AUTO_INCREMENT de la tabla `telefono`
--
ALTER TABLE `telefono`
  MODIFY `idTelefono` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

--
-- AUTO_INCREMENT de la tabla `telefono_veterinaria`
--
ALTER TABLE `telefono_veterinaria`
  MODIFY `idTelefonoVeterinaria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT de la tabla `tipo_documento`
--
ALTER TABLE `tipo_documento`
  MODIFY `idTipoDocumento` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `tipo_vacuna`
--
ALTER TABLE `tipo_vacuna`
  MODIFY `idTipoVacuna` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `tratamiento`
--
ALTER TABLE `tratamiento`
  MODIFY `idTratamiento` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT de la tabla `ubicacion_veterinaria`
--
ALTER TABLE `ubicacion_veterinaria`
  MODIFY `idUbicacion_Veterinaria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT de la tabla `usuario`
--
ALTER TABLE `usuario`
  MODIFY `idUsuario` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=62;

--
-- AUTO_INCREMENT de la tabla `vacuna`
--
ALTER TABLE `vacuna`
  MODIFY `idVacuna` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT de la tabla `veterinaria`
--
ALTER TABLE `veterinaria`
  MODIFY `idVeterinaria` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `archivo_historial`
--
ALTER TABLE `archivo_historial`
  ADD CONSTRAINT `fk_Archivo_Historial_Historial_Clinico1` FOREIGN KEY (`numeroHistorial`) REFERENCES `historial_clinico` (`numeroHistorial`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `barrio`
--
ALTER TABLE `barrio`
  ADD CONSTRAINT `fk_Barrio_Localidad1` FOREIGN KEY (`idLocalidad`) REFERENCES `localidad` (`idLocalidad`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `carnet_vacuna`
--
ALTER TABLE `carnet_vacuna`
  ADD CONSTRAINT `FK6m05f6jpc8ljkss4ymexcg48g` FOREIGN KEY (`idUsuario`) REFERENCES `usuario` (`idUsuario`),
  ADD CONSTRAINT `fk_Carnet_Vacuna_Lote_Vacuna1` FOREIGN KEY (`idLote`) REFERENCES `lote_vacuna` (`idLote`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Carnet_Vacuna_Mascota1` FOREIGN KEY (`idMascota`) REFERENCES `mascota` (`idMascota`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Carnet_Vacuna_Vacuna1` FOREIGN KEY (`idVacuna`) REFERENCES `vacuna` (`idVacuna`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Carnet_Vacuna_Veterinario1` FOREIGN KEY (`idUsuario`) REFERENCES `veterinario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `cita`
--
ALTER TABLE `cita`
  ADD CONSTRAINT `FK8s04qftngxf8wla3u6j89dl0i` FOREIGN KEY (`Veterinario_idUsuario`) REFERENCES `usuario` (`idUsuario`),
  ADD CONSTRAINT `fk_Cita_Mascota1` FOREIGN KEY (`idMascota`) REFERENCES `mascota` (`idMascota`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Cita_Servicio1` FOREIGN KEY (`Servicio_idServicio`) REFERENCES `servicio` (`idServicio`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Cita_Veterinaria1` FOREIGN KEY (`Veterinaria_idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Cita_Veterinario1` FOREIGN KEY (`Veterinario_idUsuario`) REFERENCES `veterinario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `conversacion`
--
ALTER TABLE `conversacion`
  ADD CONSTRAINT `FKc5y9ufbuos7cw4sdtjf6c4y27` FOREIGN KEY (`usuarioB`) REFERENCES `usuario` (`idUsuario`),
  ADD CONSTRAINT `FKowqauh3qmilfylhyfc61lbvsb` FOREIGN KEY (`usuarioA`) REFERENCES `usuario` (`idUsuario`);

--
-- Filtros para la tabla `correo`
--
ALTER TABLE `correo`
  ADD CONSTRAINT `fk_Correo_Usuario1` FOREIGN KEY (`idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `correo_veterinaria`
--
ALTER TABLE `correo_veterinaria`
  ADD CONSTRAINT `fk_Correo_Veterinaria_Veterinaria1` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `emergencia`
--
ALTER TABLE `emergencia`
  ADD CONSTRAINT `FK3bdit2mor7eavvyyafiw7g6e4` FOREIGN KEY (`idVeterinario`) REFERENCES `usuario` (`idUsuario`),
  ADD CONSTRAINT `FKlvpgbv2d0wtc94an3bkqi27po` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`);

--
-- Filtros para la tabla `historial_clinico`
--
ALTER TABLE `historial_clinico`
  ADD CONSTRAINT `FKt15709fysrooevts62ec4h3sx` FOREIGN KEY (`Veterinario_idUsuario`) REFERENCES `usuario` (`idUsuario`),
  ADD CONSTRAINT `fk_Historial_Clinico_Mascota1` FOREIGN KEY (`idMascota`) REFERENCES `mascota` (`idMascota`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Historial_Clinico_Veterinario1` FOREIGN KEY (`Veterinario_idUsuario`) REFERENCES `veterinario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `historial_medicamento`
--
ALTER TABLE `historial_medicamento`
  ADD CONSTRAINT `fk_Historial_Clinico_has_Medicamento_Historial_Clinico1` FOREIGN KEY (`Historial_Clinico_numeroHistorial`) REFERENCES `historial_clinico` (`numeroHistorial`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Historial_Clinico_has_Medicamento_Medicamento1` FOREIGN KEY (`Medicamento_idMedicamento`) REFERENCES `medicamento` (`idMedicamento`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `historial_tratamiento`
--
ALTER TABLE `historial_tratamiento`
  ADD CONSTRAINT `fk_Tratamiento_has_Historial_Clinico_Historial_Clinico1` FOREIGN KEY (`Historial_Clinico_numeroHistorial`) REFERENCES `historial_clinico` (`numeroHistorial`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Tratamiento_has_Historial_Clinico_Tratamiento1` FOREIGN KEY (`Tratamiento_idTratamiento`) REFERENCES `tratamiento` (`idTratamiento`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `horario_veterinaria`
--
ALTER TABLE `horario_veterinaria`
  ADD CONSTRAINT `fk_Horario_Veterinaria_Dia_Semana1` FOREIGN KEY (`idDiaSemana`) REFERENCES `dia_semana` (`idDiaSemana`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Horario_Veterinaria_Veterinaria1` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `localidad`
--
ALTER TABLE `localidad`
  ADD CONSTRAINT `fk_Localidad_Ciudad1` FOREIGN KEY (`idCiudad`) REFERENCES `ciudad` (`idCiudad`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `mascota`
--
ALTER TABLE `mascota`
  ADD CONSTRAINT `FKlbxhx5j6lsk1yh3ta9mpd8ygk` FOREIGN KEY (`Tutor_idUsuario`) REFERENCES `usuario` (`idUsuario`),
  ADD CONSTRAINT `fk_Mascota_Especie1` FOREIGN KEY (`idEspecie`) REFERENCES `especie` (`idEspecie`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Mascota_Raza1` FOREIGN KEY (`idRaza`) REFERENCES `raza` (`idRaza`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Mascota_Tutor1` FOREIGN KEY (`Tutor_idUsuario`) REFERENCES `tutor` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `mensaje_chat`
--
ALTER TABLE `mensaje_chat`
  ADD CONSTRAINT `FK6lmsyngqhj9brc50yxofopvnb` FOREIGN KEY (`idRemitente`) REFERENCES `usuario` (`idUsuario`),
  ADD CONSTRAINT `FKh6onu9cuw5kucuskr3mpuupfc` FOREIGN KEY (`idConversacion`) REFERENCES `conversacion` (`idConversacion`);

--
-- Filtros para la tabla `raza`
--
ALTER TABLE `raza`
  ADD CONSTRAINT `fk_raza_especie` FOREIGN KEY (`idEspecie`) REFERENCES `especie` (`idEspecie`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `recepcionista`
--
ALTER TABLE `recepcionista`
  ADD CONSTRAINT `fk_Recepcionista_Usuario1` FOREIGN KEY (`idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Recepcionista_Veterinaria1` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `reseña`
--
ALTER TABLE `reseña`
  ADD CONSTRAINT `FKf997r3xfnk6goppqkqr0yn928` FOREIGN KEY (`Tutor_idUsuario`) REFERENCES `usuario` (`idUsuario`),
  ADD CONSTRAINT `fk_Reseña_Calificacion1` FOREIGN KEY (`idCalificacion`) REFERENCES `calificacion` (`idCalificacion`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Reseña_Tutor1` FOREIGN KEY (`Tutor_idUsuario`) REFERENCES `tutor` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Reseña_Veterinaria1` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `servicio_veterinaria`
--
ALTER TABLE `servicio_veterinaria`
  ADD CONSTRAINT `fk_Servicio_Veterinaria_Servicio1` FOREIGN KEY (`idServicio`) REFERENCES `servicio` (`idServicio`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Servicio_Veterinaria_Veterinaria1` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `telefono`
--
ALTER TABLE `telefono`
  ADD CONSTRAINT `fk_Telefono_Usuario1` FOREIGN KEY (`idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `telefono_veterinaria`
--
ALTER TABLE `telefono_veterinaria`
  ADD CONSTRAINT `fk_Telefono_Veterinaria_Veterinaria1` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `tutor`
--
ALTER TABLE `tutor`
  ADD CONSTRAINT `fk_Tutor_Usuario1` FOREIGN KEY (`idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `ubicacion_veterinaria`
--
ALTER TABLE `ubicacion_veterinaria`
  ADD CONSTRAINT `fk_Ubicacion_Veterinaria_Barrio1` FOREIGN KEY (`idBarrio`) REFERENCES `barrio` (`idBarrio`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Ubicacion_Veterinaria_Veterinaria1` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `usuario`
--
ALTER TABLE `usuario`
  ADD CONSTRAINT `fk_Usuario_Tipo_Documento1` FOREIGN KEY (`idTipoDocumento`) REFERENCES `tipo_documento` (`idTipoDocumento`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `vacuna`
--
ALTER TABLE `vacuna`
  ADD CONSTRAINT `fk_Vacuna_Especie1` FOREIGN KEY (`idEspecie`) REFERENCES `especie` (`idEspecie`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Vacuna_Fabricante_Vacuna1` FOREIGN KEY (`idFabricanteVacuna`) REFERENCES `fabricante_vacuna` (`idFabricante_Vacuna`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Vacuna_Tipo_Vacuna1` FOREIGN KEY (`idTipoVacuna`) REFERENCES `tipo_vacuna` (`idTipoVacuna`) ON DELETE NO ACTION ON UPDATE NO ACTION;

--
-- Filtros para la tabla `veterinario`
--
ALTER TABLE `veterinario`
  ADD CONSTRAINT `fk_Veterinario_Especialidad_Veterinario1` FOREIGN KEY (`idEspecialidad`) REFERENCES `especialidad_veterinario` (`idEspecialidadVeterinario`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Veterinario_Usuario1` FOREIGN KEY (`idUsuario`) REFERENCES `usuario` (`idUsuario`) ON DELETE NO ACTION ON UPDATE NO ACTION,
  ADD CONSTRAINT `fk_Veterinario_Veterinaria1` FOREIGN KEY (`idVeterinaria`) REFERENCES `veterinaria` (`idVeterinaria`) ON DELETE NO ACTION ON UPDATE NO ACTION;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
