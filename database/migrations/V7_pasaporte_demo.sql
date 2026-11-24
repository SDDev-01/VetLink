-- =====================================================================
-- V7_pasaporte_demo.sql  (OPCIONAL — SOLO DESARROLLO/DEMO, NO PRODUCCIÓN)
-- Datos de prueba para las tablas creadas en V7_pasaporte.sql.
--
-- - Solo hace INSERT en `pasaporte` y `pasaporte_acceso`. No modifica, no borra
--   y no toca mascotas, usuarios, tutores ni datos clínicos.
-- - Los tokens 'DEMO-...' son FICTICIOS y predecibles: no sirven como tokens seguros.
-- - Se puede ejecutar varias veces: cada INSERT comprueba que la fila no exista.
-- - Las fechas son relativas al momento de ejecución, así siempre quedan
--   3 pasaportes activos, 1 vencido y 1 revocado (en la primera ejecución).
-- - No usa IDs fijos: toma de la base una mascota activa cuyo tutor sea un usuario
--   activo (mascota.Tutor_idUsuario -> tutor.idUsuario -> usuario.idUsuario).
--   Si no hay ninguna pareja válida, no inserta nada y lo informa al final.
-- =====================================================================
SET NAMES utf8mb4;

SET @ahora := NOW();

-- 1) Pareja válida mascota / propietario (tutor) tomada de los datos reales.
SET @idMascota := NULL, @idTutor := NULL;
SELECT m.idMascota, m.Tutor_idUsuario
  INTO @idMascota, @idTutor
FROM mascota m
JOIN tutor   t ON t.idUsuario = m.Tutor_idUsuario
JOIN usuario u ON u.idUsuario = t.idUsuario
WHERE m.estado = 1
  AND u.estado = 1
ORDER BY m.idMascota
LIMIT 1;

-- 2) Cinco pasaportes DEMO. idUsuarioCreador = tutor de la mascota.
--    Si @idMascota es NULL (no hay pareja válida) el SELECT no devuelve filas.
INSERT INTO pasaporte (token, idMascota, idUsuarioCreador, creado_en, expira_en, revocado_en)
SELECT d.token, @idMascota, @idTutor, d.creado_en, d.expira_en, d.revocado_en
FROM (
  -- Pasaporte 1: activo, 24 horas
  SELECT 'DEMO-PASAPORTE-0001-ACTIVO-24H' AS token,
         @ahora - INTERVAL 2 HOUR AS creado_en,
         @ahora - INTERVAL 2 HOUR + INTERVAL 24 HOUR AS expira_en,
         CAST(NULL AS DATETIME) AS revocado_en
  UNION ALL
  -- Pasaporte 2: activo, 7 días
  SELECT 'DEMO-PASAPORTE-0002-ACTIVO-7D',
         @ahora - INTERVAL 1 DAY,
         @ahora - INTERVAL 1 DAY + INTERVAL 7 DAY,
         NULL
  UNION ALL
  -- Pasaporte 3: activo, 30 días
  SELECT 'DEMO-PASAPORTE-0003-ACTIVO-30D',
         @ahora - INTERVAL 3 DAY,
         @ahora - INTERVAL 3 DAY + INTERVAL 30 DAY,
         NULL
  UNION ALL
  -- Pasaporte 4: vencido (duró 7 días y venció hace 3)
  SELECT 'DEMO-PASAPORTE-0004-VENCIDO',
         @ahora - INTERVAL 10 DAY,
         @ahora - INTERVAL 3 DAY,
         NULL
  UNION ALL
  -- Pasaporte 5: revocado hace 2 días (su vencimiento original era a 30 días)
  SELECT 'DEMO-PASAPORTE-0005-REVOCADO',
         @ahora - INTERVAL 5 DAY,
         @ahora - INTERVAL 5 DAY + INTERVAL 30 DAY,
         @ahora - INTERVAL 2 DAY
) AS d
WHERE @idMascota IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM pasaporte p WHERE p.token = d.token);

-- 3) Aperturas DEMO (IPs de documentación RFC 5737 / RFC 3849).
--    Cada apertura cae entre la creación y el vencimiento/revocación de su pasaporte.
--    Se evita duplicar comparando pasaporte + user_agent (único por fila DEMO).
INSERT INTO pasaporte_acceso (idPasaporte, consultado_en, ip, user_agent)
SELECT p.idPasaporte, a.consultado_en, a.ip, a.user_agent
FROM (
  -- P1 (creado hace 2 h): abierto hace 1 h
  SELECT 'DEMO-PASAPORTE-0001-ACTIVO-24H' AS token,
         @ahora - INTERVAL 1 HOUR AS consultado_en,
         '192.0.2.10' AS ip,
         'Mozilla/5.0 (Linux; Android 14) DEMO-VetLink-acceso-01' AS user_agent
  UNION ALL
  -- P2 (creado hace 1 día): abierto hace 20 h y hace 2 h
  SELECT 'DEMO-PASAPORTE-0002-ACTIVO-7D', @ahora - INTERVAL 20 HOUR,
         '198.51.100.23', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) DEMO-VetLink-acceso-02'
  UNION ALL
  SELECT 'DEMO-PASAPORTE-0002-ACTIVO-7D', @ahora - INTERVAL 2 HOUR,
         '2001:db8::25', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) DEMO-VetLink-acceso-03'
  UNION ALL
  -- P3 (creado hace 3 días): abierto hace 2 días
  SELECT 'DEMO-PASAPORTE-0003-ACTIVO-30D', @ahora - INTERVAL 2 DAY,
         '203.0.113.7', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) DEMO-VetLink-acceso-04'
  UNION ALL
  -- P4 (vigente de hace 10 a hace 3 días): abierto hace 8 días, antes de vencer
  SELECT 'DEMO-PASAPORTE-0004-VENCIDO', @ahora - INTERVAL 8 DAY,
         '192.0.2.44', 'Mozilla/5.0 (Linux; Android 13) DEMO-VetLink-acceso-05'
  UNION ALL
  -- P5 (creado hace 5 días, revocado hace 2): abierto hace 4 días, antes de revocarse
  SELECT 'DEMO-PASAPORTE-0005-REVOCADO', @ahora - INTERVAL 4 DAY,
         '198.51.100.88', 'Mozilla/5.0 (X11; Linux x86_64) DEMO-VetLink-acceso-06'
) AS a
JOIN pasaporte p ON p.token = a.token
WHERE NOT EXISTS (
  SELECT 1 FROM pasaporte_acceso x
  WHERE x.idPasaporte = p.idPasaporte AND x.user_agent = a.user_agent
);

-- 4) Resultado.
SELECT CASE
         WHEN @idMascota IS NULL
           THEN 'SIN DATOS: no hay una mascota activa con un tutor/usuario activo. No se insertó nada.'
         ELSE CONCAT('OK: mascota ', @idMascota, ' / tutor ', @idTutor,
                     ' | pasaportes DEMO: ', (SELECT COUNT(*) FROM pasaporte WHERE token LIKE 'DEMO-PASAPORTE-%'),
                     ' | aperturas DEMO: ', (SELECT COUNT(*) FROM pasaporte_acceso WHERE user_agent LIKE '%DEMO-VetLink-acceso-%'))
       END AS resultado_demo;
