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

