-- =====================================================================
-- verificar_consistencia.sql
-- Comprobaciones de integridad de los datos. Solo lectura: no modifica nada.
-- Lo ejecuta scripts/migrate.sh al terminar; también se puede correr a mano:
--   docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -t "$MYSQL_DATABASE"' < database/verificar_consistencia.sql
--
-- Cada fila es «comprobación | cantidad». Lo esperado es 0 en todas, salvo las
-- marcadas como (info), que solo describen los datos.
-- =====================================================================
SELECT 'mascotas cuyo tutor no existe' AS comprobacion, COUNT(*) AS cantidad
  FROM mascota m LEFT JOIN tutor t ON t.idUsuario = m.Tutor_idUsuario WHERE t.idUsuario IS NULL
UNION ALL
SELECT 'citas con mascota inexistente', COUNT(*)
  FROM cita c LEFT JOIN mascota m ON m.idMascota = c.idMascota WHERE c.idMascota IS NOT NULL AND m.idMascota IS NULL
UNION ALL
SELECT 'historias con mascota inexistente', COUNT(*)
  FROM historial_clinico h LEFT JOIN mascota m ON m.idMascota = h.idMascota WHERE m.idMascota IS NULL
UNION ALL
SELECT 'vacunas con mascota inexistente', COUNT(*)
  FROM carnet_vacuna v LEFT JOIN mascota m ON m.idMascota = v.idMascota WHERE m.idMascota IS NULL
UNION ALL
SELECT 'emergencias con mascota inexistente', COUNT(*)
  FROM emergencia e LEFT JOIN mascota m ON m.idMascota = e.idMascota WHERE e.idMascota IS NOT NULL AND m.idMascota IS NULL
UNION ALL
SELECT 'citas reservadas sin mascota', COUNT(*)
  FROM cita c WHERE c.idMascota IS NULL AND c.estado NOT IN ('DISPONIBLE', 'CANCELADA', 'RECHAZADA', 'REPROGRAMADA')
UNION ALL
SELECT 'mascotas duplicadas (mismo tutor, nombre, especie y nacimiento)', COUNT(*)
  FROM (SELECT 1 FROM mascota GROUP BY Tutor_idUsuario, nombre, idEspecie, fechaNacimiento HAVING COUNT(*) > 1) d
UNION ALL
SELECT 'mascotas dadas de baja con citas futuras vigentes', COUNT(*)
  FROM cita c JOIN mascota m ON m.idMascota = c.idMascota
 WHERE m.estado = 0 AND c.estado IN ('PENDIENTE', 'CONFIRMADA', 'EN_ATENCION') AND c.fecha >= CURDATE()
UNION ALL
SELECT 'pasaportes vigentes de mascotas dadas de baja', COUNT(*)
  FROM pasaporte p JOIN mascota m ON m.idMascota = p.idMascota
 WHERE m.estado = 0 AND p.revocado_en IS NULL AND p.expira_en > NOW()
UNION ALL
SELECT 'transferencias pendientes de mascotas dadas de baja', COUNT(*)
  FROM transferencia_mascota t JOIN mascota m ON m.idMascota = t.idMascota
 WHERE m.estado = 0 AND t.estado = 'PENDIENTE'
UNION ALL
SELECT '(info) usuarios sin rol (no pueden iniciar sesión)', COUNT(*)
  FROM usuario u
 WHERE NOT EXISTS (SELECT 1 FROM tutor t WHERE t.idUsuario = u.idUsuario)
   AND NOT EXISTS (SELECT 1 FROM veterinario v WHERE v.idUsuario = u.idUsuario)
   AND NOT EXISTS (SELECT 1 FROM recepcionista r WHERE r.idUsuario = u.idUsuario)
UNION ALL
SELECT '(info) mascotas dadas de baja (estado 0, ocultas al dueño)', COUNT(*)
  FROM mascota WHERE estado = 0
UNION ALL
SELECT '(info) citas pasadas que siguen PENDIENTE o CONFIRMADA', COUNT(*)
  FROM cita WHERE idMascota IS NOT NULL AND estado IN ('PENDIENTE', 'CONFIRMADA') AND fecha < CURDATE();
