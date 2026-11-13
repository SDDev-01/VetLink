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

