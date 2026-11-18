-- =====================================================================
-- V5_correcciones.sql
-- Correcciones de CORRECCIONES_SPRING.md (tareas VL-XX).
--
-- Se ejecuta UNA SOLA VEZ, después de V4_atencion_y_archivos.sql.
-- ANTES DE APLICARLO haz un respaldo completo de la base:
--     mysqldump -u root -p mydb > mydb_respaldo_antes_de_V5.sql
--
-- Cada sentencia lleva un comentario con el número de tarea que la origina.
-- Este archivo no borra filas de datos existentes.
-- =====================================================================

-- ---------------------------------------------------------------------
-- VL-09: las clínicas nuevas se registran con estado = 0 (pendiente de verificación).
-- Para aprobar una clínica después de revisarla:
-- UPDATE veterinaria SET estado = 1 WHERE idVeterinaria = ?;
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- VL-10: calificaciones de 2 y 1 estrellas (la tabla solo traía 5, 4 y 3).
-- Verificado en database/mydb.sql: solo existen los ids 1, 2 y 3 (AUTO_INCREMENT=4).
-- Comprobación previa en tu base:  SELECT * FROM calificacion WHERE idCalificacion IN (4, 5);
-- ---------------------------------------------------------------------
INSERT INTO calificacion (idCalificacion, valor, descripcion) VALUES (4, 2, 'Malo'), (5, 1, 'Muy malo'); -- VL-10

-- ---------------------------------------------------------------------
-- VL-14: agregar el domingo (dia_semana solo tenía lunes a sábado).
-- Verificado en database/mydb.sql: ids 1..6 (AUTO_INCREMENT=7), el 7 está libre.
-- Comprobación previa en tu base:  SELECT * FROM dia_semana WHERE idDiaSemana = 7;
-- ---------------------------------------------------------------------
INSERT INTO dia_semana (idDiaSemana, diaSemana) VALUES (7, 'Domingo'); -- VL-14

-- VL-14: horarios inválidos ya guardados (apertura >= cierre, p. ej. 20:34 a 00:23).
-- La aplicación ya no los usa para calcular disponibilidad; revísalos y corrígelos a mano:
-- SELECT * FROM horario_veterinaria WHERE horaApertura >= horaCierre;

-- ---------------------------------------------------------------------
-- VL-17: la próxima cita sugerida de una historia clínica pasa a ser opcional.
-- ---------------------------------------------------------------------
ALTER TABLE historial_clinico MODIFY proximaCita DATE NULL; -- VL-17

-- ---------------------------------------------------------------------
-- VL-24: llave foránea de emergencia.idMascota y tipo igual al de mascota.idMascota.
-- Antes de agregar la llave, revisa que no haya emergencias apuntando a mascotas inexistentes
-- (en la copia de mydb usada para probar este archivo no había ninguna):
-- SELECT e.idEmergencia, e.idMascota FROM emergencia e
--   LEFT JOIN mascota m ON m.idMascota = e.idMascota
--   WHERE e.idMascota IS NOT NULL AND m.idMascota IS NULL;
-- ---------------------------------------------------------------------
ALTER TABLE emergencia MODIFY idMascota INT(10) UNSIGNED NULL; -- VL-24
ALTER TABLE emergencia ADD CONSTRAINT fk_emergencia_mascota FOREIGN KEY (idMascota) REFERENCES mascota(idMascota); -- VL-24

-- ---------------------------------------------------------------------
-- VL-24: unificar el charset. Estas 36 tablas siguen en utf8 (utf8mb3); el resto ya está en utf8mb4.
-- EJECUTAR SOLO CON RESPALDO: reescribe cada tabla completa y puede tardar.
-- ---------------------------------------------------------------------
-- ALTER TABLE `archivo_historial` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `barrio` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `calificacion` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `carnet_vacuna` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `cita` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `ciudad` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `correo` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `correo_veterinaria` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `dia_semana` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `especialidad_veterinario` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `especie` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `fabricante_vacuna` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `historial_clinico` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `historial_medicamento` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `historial_tratamiento` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `horario_veterinaria` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `localidad` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `lote_vacuna` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `mascota` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `medicamento` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `raza` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `recepcionista` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `reseña` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `servicio` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `servicio_veterinaria` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `telefono` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `telefono_veterinaria` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `tipo_documento` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `tipo_vacuna` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `tratamiento` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `tutor` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `ubicacion_veterinaria` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `usuario` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `vacuna` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `veterinaria` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24
-- ALTER TABLE `veterinario` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; -- VL-24

