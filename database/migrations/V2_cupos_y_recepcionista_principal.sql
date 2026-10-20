-- VetLink V2: ejecutar una sola vez después de importar mydb.sql.
-- Permite crear cupos DISPONIBLE sin asignar una mascota y distingue a la
-- recepcionista principal. No borra ni transforma citas existentes.
ALTER TABLE cita MODIFY idMascota INT(10) UNSIGNED NULL;
ALTER TABLE recepcionista ADD COLUMN esPrincipal TINYINT(1) NOT NULL DEFAULT 0;

-- Marcar exactamente una principal por veterinaria. Revise el resultado antes
-- de ejecutar en producción si su operación usa otro criterio.
UPDATE recepcionista r
JOIN (
  SELECT idVeterinaria, MIN(idUsuario) AS idPrincipal
  FROM recepcionista
  GROUP BY idVeterinaria
) p ON p.idVeterinaria = r.idVeterinaria AND p.idPrincipal = r.idUsuario
SET r.esPrincipal = 1;
