-- =====================================================================
-- V13_veterinarias_directorio.sql  (tipo demo: se aplica con RUN_DEMO_SEED=true)
-- Agrega 20 veterinarias al directorio: 12 en Bogotá y 8 en otras ciudades
-- (Medellín, Cali, Barranquilla, Bucaramanga, Cartagena y Pereira).
--
-- Por cada clínica: veterinaria, ubicación (barrio y coordenadas), teléfono, correo,
-- horario por día y servicios con precio (COP) y duración.
-- No crea personal (veterinarios ni recepcionistas): las clínicas aparecen en el
-- directorio con "Sin cupos publicados por ahora" hasta que tengan agenda.
--
-- Solo inserta filas; no modifica la estructura. Funciona sobre cualquier base del
-- proyecto porque busca los catálogos por nombre (no por ID):
--   * ciudades, localidades y barrios se crean solo si no existen;
--   * los servicios se buscan por nombre (p. ej. 'Vacunaci%'); si uno no existe, se omite.
-- Es idempotente: cada INSERT filtra con NOT EXISTS (por NIT o por clínica).
-- =====================================================================
SET NAMES utf8mb4;

CREATE TEMPORARY TABLE tmp_v13_vet (
    nit          VARCHAR(45)  NOT NULL,
    nombre       VARCHAR(50)  NOT NULL,
    descripcion  VARCHAR(400) NOT NULL,
    web          VARCHAR(50)  NULL,
    ciudad       VARCHAR(45)  NOT NULL,
    ciudad_alt   VARCHAR(45)  NOT NULL,
    localidad    VARCHAR(45)  NOT NULL,
    barrio       VARCHAR(45)  NOT NULL,
    calle        VARCHAR(45)  NOT NULL,
    numero       VARCHAR(45)  NOT NULL,
    estrato      INT          NOT NULL,
    lat          DOUBLE       NOT NULL,
    lng          DOUBLE       NOT NULL,
    telefono     VARCHAR(45)  NOT NULL,
    correo       VARCHAR(45)  NOT NULL,
    abre         TIME         NOT NULL,
    cierra       TIME         NOT NULL,
    sabado       TIME         NULL COMMENT 'cierre del sábado (abre a la misma hora); NULL = cerrado',
    domingo      TIME         NULL COMMENT 'cierre del domingo (abre 09:00, o 00:00 si es 24 horas); NULL = cerrado',
    servicios    VARCHAR(200) NOT NULL,
    factor       DOUBLE       NOT NULL COMMENT 'multiplica el precio base según la zona',
    aprobada     DATETIME     NOT NULL,
    idCiudad     INT NULL, idLocalidad INT NULL, idBarrio INT NULL, idVeterinaria INT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;

INSERT INTO tmp_v13_vet (nit, nombre, descripcion, web, ciudad, ciudad_alt, localidad, barrio, calle, numero, estrato,
                         lat, lng, telefono, correo, abre, cierra, sabado, domingo, servicios, factor, aprobada) VALUES
('901612345-8', 'Veterinaria Mascotas Usaquén', 'Medicina preventiva, vacunación y consulta general para perros y gatos en el norte de Bogotá.', 'www.mascotasusaquen.com.co',
 'Bogotá D.C.', 'Bogota', 'Usaquén', 'Santa Bárbara', 'Calle 116', '# 15-42', 5, 4.696812, -74.044731, '6016123487', 'citas@mascotasusaquen.com.co',
 '08:00', '19:00', '15:00', NULL, 'consulta,vacunacion,desparasitacion,dermatologia,laboratorio', 1.20, '2025-02-10 10:00:00'),
('900784512-3', 'Clínica Veterinaria Cedritos', 'Consulta, cirugía y ecografía con atención de lunes a sábado.', 'www.vetcedritos.com',
 'Bogotá D.C.', 'Bogota', 'Usaquén', 'Cedritos', 'Calle 140', '# 11-28', 4, 4.722514, -74.041026, '6016259014', 'contacto@vetcedritos.com',
 '08:00', '18:00', '14:00', NULL, 'consulta,vacunacion,esterilizacion,ecografia,desparasitacion', 1.10, '2024-11-05 09:30:00'),
('901347856-1', 'Centro Veterinario Teusaquillo', 'Atención integral de pequeños animales cerca de Galerías y Park Way.', NULL,
 'Bogotá D.C.', 'Bogota', 'Teusaquillo', 'Galerías', 'Calle 53', '# 24-17', 4, 4.643027, -74.076013, '6013487215', 'centroveteusaquillo@gmail.com',
 '08:00', '18:00', '13:00', NULL, 'consulta,vacunacion,desparasitacion,odontologia', 1.00, '2024-06-18 15:20:00'),
('900562318-4', 'Veterinaria Peludos de Chapinero', 'Clínica de barrio con enfoque en gatos y perros pequeños. Dermatología y odontología.', 'www.peludoschapinero.co',
 'Bogotá D.C.', 'Bogota', 'Chapinero', 'Chapinero Alto', 'Carrera 5', '# 57-30', 5, 4.646215, -74.059108, '6017024561', 'hola@peludoschapinero.co',
 '09:00', '19:00', '14:00', NULL, 'consulta,vacunacion,dermatologia,odontologia,desparasitacion', 1.20, '2025-04-22 11:10:00'),
('901225874-6', 'Clínica Veterinaria Modelia', 'Consulta general, laboratorio clínico y esterilizaciones con cita previa.', NULL,
 'Bogotá D.C.', 'Bogota', 'Fontibón', 'Modelia', 'Calle 24', '# 75-60', 4, 4.670118, -74.117025, '6014158823', 'vetmodelia@gmail.com',
 '08:00', '18:00', '13:00', NULL, 'consulta,vacunacion,laboratorio,esterilizacion', 1.00, '2024-09-02 08:45:00'),
('900918274-2', 'Hospital Veterinario Engativá 24 Horas', 'Urgencias 24 horas, hospitalización y cirugía. Atención todos los días del año.', 'www.hospitalvetengativa.com',
 'Bogotá D.C.', 'Bogota', 'Engativá', 'Minuto de Dios', 'Calle 80', '# 73A-10', 3, 4.699053, -74.088214, '6014390012', 'urgencias@hospitalvetengativa.com',
 '00:00', '23:59', '23:59', '23:59', 'consulta,vacunacion,urgencias,esterilizacion,laboratorio,ecografia', 0.95, '2023-03-15 07:00:00'),
('901458712-9', 'Veterinaria Amigos Fieles Kennedy', 'Consulta, vacunación y desparasitación a precios accesibles en Timiza.', NULL,
 'Bogotá D.C.', 'Bogota', 'Kennedy', 'Timiza', 'Carrera 72', '# 40B Sur-15', 3, 4.610342, -74.145107, '6014527733', 'amigosfieleskennedy@gmail.com',
 '07:30', '18:30', '16:00', '13:00', 'consulta,vacunacion,desparasitacion', 0.85, '2025-07-01 10:00:00'),
('900631459-7', 'Clínica Veterinaria Bosa Centro', 'Atención veterinaria para Bosa y Soacha: consulta, vacunas y urgencias diurnas.', NULL,
 'Bogotá D.C.', 'Bogota', 'Bosa', 'Bosa Centro', 'Calle 63 Sur', '# 80-25', 2, 4.619536, -74.190077, '6017756120', 'vetbosacentro@gmail.com',
 '07:00', '19:00', '17:00', '13:00', 'consulta,vacunacion,desparasitacion,urgencias', 0.80, '2025-01-14 09:00:00'),
('901774236-5', 'Veterinaria Huellas del Sur', 'Consulta general y laboratorio en Venecia, sur de Bogotá.', NULL,
 'Bogotá D.C.', 'Bogota', 'Tunjuelito', 'Venecia', 'Carrera 51', '# 48A Sur-12', 3, 4.594087, -74.139210, '6017140985', 'huellasdelsur@gmail.com',
 '08:00', '18:00', '14:00', NULL, 'consulta,vacunacion,desparasitacion,laboratorio', 0.90, '2024-12-09 14:30:00'),
('900845621-0', 'Centro Médico Veterinario Colina', 'Especialistas en dermatología, odontología y ecografía. Parqueadero propio.', 'www.cmvcolina.com.co',
 'Bogotá D.C.', 'Bogota', 'Suba', 'Colina Campestre', 'Calle 138', '# 55-31', 5, 4.730061, -74.065024, '6016437790', 'info@cmvcolina.com.co',
 '08:00', '19:00', '15:00', NULL, 'consulta,vacunacion,dermatologia,odontologia,ecografia,laboratorio', 1.25, '2024-03-27 16:00:00'),
('901093847-2', 'Veterinaria Patas y Bigotes', 'Consulta y vacunación de perros, gatos y conejos en Barrios Unidos.', NULL,
 'Bogotá D.C.', 'Bogota', 'Barrios Unidos', 'Doce de Octubre', 'Carrera 50', '# 73-14', 4, 4.668214, -74.075118, '6013106642', 'patasybigotes.vet@gmail.com',
 '08:30', '18:30', '14:00', NULL, 'consulta,vacunacion,desparasitacion,dermatologia', 1.00, '2025-05-19 12:00:00'),
('900376512-8', 'Clínica Felina La Candelaria', 'Clínica exclusiva para gatos: consulta, vacunación, odontología y laboratorio.', 'www.clinicafelinacandelaria.co',
 'Bogotá D.C.', 'Bogota', 'La Candelaria', 'La Concordia', 'Calle 12C', '# 2-41', 3, 4.598231, -74.070112, '6012846011', 'gatos@clinicafelinacandelaria.co',
 '09:00', '18:00', '13:00', NULL, 'consulta,vacunacion,odontologia,laboratorio', 1.05, '2025-08-11 10:30:00'),
('901534278-3', 'Clínica Veterinaria Laureles', 'Medicina interna, cirugía y ecografía en el corazón de Laureles.', 'www.vetlaureles.com',
 'Medellín', 'Medellin', 'Laureles-Estadio', 'Laureles', 'Circular 1', '# 70-25', 5, 6.245107, -75.596021, '6044128730', 'citas@vetlaureles.com',
 '08:00', '19:00', '14:00', NULL, 'consulta,vacunacion,esterilizacion,ecografia,laboratorio', 1.15, '2024-05-06 09:00:00'),
('900712653-1', 'Veterinaria Belén Mascotas', 'Consulta, vacunación y urgencias diurnas para el occidente de Medellín.', NULL,
 'Medellín', 'Medellin', 'Belén', 'La Mota', 'Calle 1 Sur', '# 76-40', 4, 6.199208, -75.602215, '6043415507', 'belenmascotas@gmail.com',
 '07:30', '19:00', '16:00', '13:00', 'consulta,vacunacion,desparasitacion,urgencias', 0.95, '2025-03-03 08:15:00'),
('901287465-4', 'Veterinaria Granada Cali', 'Atención de perros y gatos con dermatología y laboratorio propio.', 'www.vetgranadacali.com',
 'Cali', 'Cali', 'Comuna 2', 'Granada', 'Avenida 9 Norte', '# 15-40', 5, 3.461027, -76.531014, '6026617840', 'contacto@vetgranadacali.com',
 '08:00', '18:30', '13:00', NULL, 'consulta,vacunacion,dermatologia,laboratorio,desparasitacion', 1.05, '2024-10-21 11:00:00'),
('900957341-6', 'Centro Veterinario Ciudad Jardín', 'Urgencias 24 horas, cirugía y hospitalización en el sur de Cali.', 'www.cvciudadjardin.com',
 'Cali', 'Cali', 'Comuna 22', 'Ciudad Jardín', 'Carrera 105', '# 14-50', 6, 3.360214, -76.534012, '6023315098', 'urgencias@cvciudadjardin.com',
 '00:00', '23:59', '23:59', '23:59', 'consulta,vacunacion,urgencias,esterilizacion,ecografia', 1.20, '2023-08-30 07:30:00'),
('901648923-0', 'Clínica Veterinaria El Prado', 'Consulta, vacunación y esterilizaciones en El Prado, Barranquilla.', NULL,
 'Barranquilla', 'Barranquilla', 'Norte-Centro Histórico', 'El Prado', 'Carrera 54', '# 64-120', 5, 10.993018, -74.799107, '6053602214', 'vetelprado@gmail.com',
 '08:00', '18:00', '13:00', NULL, 'consulta,vacunacion,esterilizacion,desparasitacion', 1.00, '2025-06-09 10:00:00'),
('900423687-5', 'Veterinaria Cabecera', 'Consulta general, odontología y laboratorio clínico en Cabecera del Llano.', 'www.veterinariacabecera.co',
 'Bucaramanga', 'Bucaramanga', 'Cabecera del Llano', 'Cabecera del Llano', 'Carrera 33', '# 48-60', 5, 7.119045, -73.110028, '6076435511', 'info@veterinariacabecera.co',
 '08:00', '18:00', '13:00', NULL, 'consulta,vacunacion,odontologia,laboratorio', 1.00, '2024-07-15 09:40:00'),
('901872034-7', 'Veterinaria Bocagrande', 'Consulta, vacunación y certificados para viaje de mascotas en Bocagrande.', NULL,
 'Cartagena', 'Cartagena de Indias', 'Histórica y del Caribe Norte', 'Bocagrande', 'Carrera 3', '# 8-129', 6, 10.399021, -75.554017, '6056654120', 'vetbocagrande@gmail.com',
 '08:00', '19:00', '15:00', NULL, 'consulta,vacunacion,desparasitacion,dermatologia', 1.20, '2025-09-01 08:00:00'),
('900689154-2', 'Clínica Veterinaria Circunvalar', 'Medicina general, ecografía y cirugía en Pereira.', 'www.vetcircunvalar.com',
 'Pereira', 'Pereira', 'Comuna Centro', 'Circunvalar', 'Avenida Circunvalar', '# 12-40', 5, 4.810112, -75.690208, '6063354782', 'citas@vetcircunvalar.com',
 '08:00', '18:30', '14:00', NULL, 'consulta,vacunacion,ecografia,esterilizacion', 1.00, '2024-04-08 13:00:00');

-- 1. Ciudades, localidades y barrios que falten (búsqueda por nombre; general_ci no distingue tildes).
INSERT INTO ciudad (nombre)
SELECT DISTINCT t.ciudad FROM tmp_v13_vet t
 WHERE NOT EXISTS (SELECT 1 FROM ciudad c WHERE c.nombre IN (t.ciudad, t.ciudad_alt));
UPDATE tmp_v13_vet t
   SET t.idCiudad = (SELECT MIN(c.idCiudad) FROM ciudad c WHERE c.nombre IN (t.ciudad, t.ciudad_alt));

INSERT INTO localidad (nombre, idCiudad)
SELECT DISTINCT t.localidad, t.idCiudad FROM tmp_v13_vet t
 WHERE NOT EXISTS (SELECT 1 FROM localidad l WHERE l.nombre = t.localidad AND l.idCiudad = t.idCiudad);
UPDATE tmp_v13_vet t
   SET t.idLocalidad = (SELECT MIN(l.idLocalidad) FROM localidad l WHERE l.nombre = t.localidad AND l.idCiudad = t.idCiudad);

INSERT INTO barrio (nombre, idLocalidad)
SELECT DISTINCT t.barrio, t.idLocalidad FROM tmp_v13_vet t
 WHERE NOT EXISTS (SELECT 1 FROM barrio b WHERE b.nombre = t.barrio AND b.idLocalidad = t.idLocalidad);
UPDATE tmp_v13_vet t
   SET t.idBarrio = (SELECT MIN(b.idBarrio) FROM barrio b WHERE b.nombre = t.barrio AND b.idLocalidad = t.idLocalidad);

-- 2. Veterinarias (NIT y nombre son únicos: si ya existe alguna de las dos, no se inserta).
INSERT INTO veterinaria (nit, nombre, logo, descripcion, paginaWeb, estado, aprobadaEn, aprobacionAutomatica)
SELECT t.nit, t.nombre, 'default-logo.png', t.descripcion, t.web, 1, t.aprobada, 0
  FROM tmp_v13_vet t
 WHERE NOT EXISTS (SELECT 1 FROM veterinaria v WHERE v.nit = t.nit OR v.nombre = t.nombre);
-- Solo se completan las clínicas de esta migración (identificadas por NIT y nombre).
UPDATE tmp_v13_vet t
   SET t.idVeterinaria = (SELECT v.idVeterinaria FROM veterinaria v WHERE v.nit = t.nit AND v.nombre = t.nombre);

-- 3. Ubicación, teléfono y correo (uno por clínica).
INSERT INTO ubicacion_veterinaria (calle, numero, estrato, latitud, longitud, idVeterinaria, idBarrio)
SELECT t.calle, t.numero, t.estrato, t.lat, t.lng, t.idVeterinaria, t.idBarrio
  FROM tmp_v13_vet t
 WHERE t.idVeterinaria IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM ubicacion_veterinaria u WHERE u.idVeterinaria = t.idVeterinaria);

INSERT INTO telefono_veterinaria (idVeterinaria, numeroTelefono)
SELECT t.idVeterinaria, t.telefono FROM tmp_v13_vet t
 WHERE t.idVeterinaria IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM telefono_veterinaria x WHERE x.idVeterinaria = t.idVeterinaria);

INSERT INTO correo_veterinaria (idVeterinaria, correo)
SELECT t.idVeterinaria, t.correo FROM tmp_v13_vet t
 WHERE t.idVeterinaria IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM correo_veterinaria x WHERE x.idVeterinaria = t.idVeterinaria);

-- 4. Horario por día (1 = lunes ... 7 = domingo).
CREATE TEMPORARY TABLE tmp_v13_dia (d INT NOT NULL PRIMARY KEY);
INSERT INTO tmp_v13_dia (d) VALUES (1), (2), (3), (4), (5), (6), (7);

INSERT INTO horario_veterinaria (idDiaSemana, horaApertura, horaCierre, estado, idVeterinaria)
SELECT d.d,
       CASE WHEN d.d = 7 AND t.abre <> '00:00:00' THEN '09:00:00' ELSE t.abre END,
       CASE WHEN d.d <= 5 THEN t.cierra WHEN d.d = 6 THEN t.sabado ELSE t.domingo END,
       1, t.idVeterinaria
  FROM tmp_v13_vet t
  JOIN tmp_v13_dia d ON d.d <= 5 OR (d.d = 6 AND t.sabado IS NOT NULL) OR (d.d = 7 AND t.domingo IS NOT NULL)
 WHERE t.idVeterinaria IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM horario_veterinaria h WHERE h.idVeterinaria = t.idVeterinaria);

-- 5. Servicios con precio (COP, redondeado a miles) y duración en minutos.
CREATE TEMPORARY TABLE tmp_v13_servicio (
    clave       VARCHAR(30) NOT NULL PRIMARY KEY,
    patron      VARCHAR(45) NOT NULL,
    precio      DOUBLE      NOT NULL,
    duracion    INT         NOT NULL,
    idServicio  INT NULL
) DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci;
INSERT INTO tmp_v13_servicio (clave, patron, precio, duracion) VALUES
('consulta', 'Consulta General%', 60000, 30),
('vacunacion', 'Vacunaci%', 70000, 30),
('desparasitacion', 'Desparasitaci%', 42000, 30),
('urgencias', 'Urgencia%', 160000, 60),
('esterilizacion', 'Esteriliz%', 430000, 90),
('laboratorio', 'Laboratorio%', 100000, 30),
('ecografia', 'Ecograf%', 140000, 30),
('odontologia', 'Odontolog%', 300000, 60),
('dermatologia', 'Dermatolog%', 90000, 30);
UPDATE tmp_v13_servicio s
   SET s.idServicio = (SELECT MIN(x.idServicio) FROM servicio x WHERE x.nombreServicio LIKE s.patron);

INSERT INTO servicio_veterinaria (idServicio, idVeterinaria, precio, duracionAprox)
SELECT s.idServicio, t.idVeterinaria, ROUND(s.precio * t.factor, -3), s.duracion
  FROM tmp_v13_vet t
  JOIN tmp_v13_servicio s ON FIND_IN_SET(s.clave, t.servicios) > 0
 WHERE t.idVeterinaria IS NOT NULL AND s.idServicio IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM servicio_veterinaria sv
                    WHERE sv.idServicio = s.idServicio AND sv.idVeterinaria = t.idVeterinaria);

DROP TEMPORARY TABLE tmp_v13_servicio;
DROP TEMPORARY TABLE tmp_v13_dia;
DROP TEMPORARY TABLE tmp_v13_vet;
