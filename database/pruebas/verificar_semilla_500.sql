-- =====================================================================
-- database/pruebas/verificar_semilla_500.sql
-- Comprobaciones de la semilla de pruebas (database/pruebas/semilla_500.py). Solo lectura.
-- Lo ejecuta scripts/semilla-pruebas.sh; el conteo por tabla (exactamente 500) lo hace el script
-- de shell porque recorre todas las tablas de la base.
--
-- Cada fila es «comprobación | esperado | obtenido». La última columna dice OK o REVISAR.
-- =====================================================================
SET SESSION information_schema_stats_expiry = 0;

SELECT comprobacion, esperado, obtenido, IF(esperado = obtenido, 'OK', 'REVISAR') AS resultado FROM (
SELECT 'dueños de prueba (1..10@gmail.com) con rol TUTOR' AS comprobacion, 10 AS esperado, COUNT(*) AS obtenido
  FROM correo c JOIN tutor t ON t.idUsuario = c.idUsuario JOIN usuario u ON u.idUsuario = c.idUsuario
 WHERE u.estado = 1 AND c.correo REGEXP '^([1-9]|10)@gmail\\.com$'
UNION ALL
SELECT 'veterinarios de prueba (100..110@gmail.com) con rol VETERINARIO', 11, COUNT(*)
  FROM correo c JOIN veterinario v ON v.idUsuario = c.idUsuario JOIN usuario u ON u.idUsuario = c.idUsuario
 WHERE u.estado = 1 AND v.idVeterinaria IS NOT NULL AND c.correo REGEXP '^1(0[0-9]|10)@gmail\\.com$'
UNION ALL
SELECT 'recepcionistas de prueba (1000..1010@gmail.com) con rol RECEPCIONISTA', 11, COUNT(*)
  FROM correo c JOIN recepcionista r ON r.idUsuario = c.idUsuario JOIN usuario u ON u.idUsuario = c.idUsuario
 WHERE u.estado = 1 AND c.correo REGEXP '^10(0[0-9]|10)@gmail\\.com$'
UNION ALL
SELECT 'usuarios con más de un rol', 0, COUNT(*)
  FROM usuario u
 WHERE (EXISTS (SELECT 1 FROM tutor t WHERE t.idUsuario = u.idUsuario))
     + (EXISTS (SELECT 1 FROM veterinario v WHERE v.idUsuario = u.idUsuario))
     + (EXISTS (SELECT 1 FROM recepcionista r WHERE r.idUsuario = u.idUsuario)) <> 1
UNION ALL
SELECT 'contraseñas que no son hash BCrypt', 0, COUNT(*)
  FROM usuario WHERE password NOT REGEXP '^\\$2[aby]\\$10\\$.{53}$'
UNION ALL
SELECT 'mascotas con estado distinto de 1', 0, COUNT(*) FROM mascota WHERE estado <> 1
UNION ALL
SELECT 'mascotas en total (todas con estado = 1)', (SELECT COUNT(*) FROM mascota), COUNT(*) FROM mascota WHERE estado = 1
UNION ALL
SELECT 'cada clínica con una recepcionista principal', (SELECT COUNT(*) FROM veterinaria), COUNT(*)
  FROM (SELECT idVeterinaria FROM recepcionista WHERE esPrincipal = 1 GROUP BY idVeterinaria HAVING COUNT(*) = 1) p
UNION ALL
SELECT 'especies sin raza «Otra» (regla V12)', 0, COUNT(*)
  FROM especie e WHERE NOT EXISTS (SELECT 1 FROM raza r WHERE r.idEspecie = e.idEspecie AND r.nombreRaza = 'Otra')
UNION ALL
SELECT 'mascotas cuya raza no es de su especie', 0, COUNT(*)
  FROM mascota m JOIN raza r ON r.idRaza = m.idRaza WHERE r.idEspecie <> m.idEspecie
UNION ALL
SELECT 'citas cuyo veterinario no es de la clínica de la cita', 0, COUNT(*)
  FROM cita c JOIN veterinario v ON v.idUsuario = c.Veterinario_idUsuario
 WHERE v.idVeterinaria <> c.Veterinaria_idVeterinaria
UNION ALL
SELECT 'citas con un servicio que la clínica no ofrece', 0, COUNT(*)
  FROM cita c LEFT JOIN servicio_veterinaria sv
    ON sv.idServicio = c.Servicio_idServicio AND sv.idVeterinaria = c.Veterinaria_idVeterinaria
 WHERE sv.idServicio IS NULL
UNION ALL
SELECT 'citas ATENDIDAS clínicas sin historia clínica', 0, COUNT(*)
  FROM cita c JOIN servicio s ON s.idServicio = c.Servicio_idServicio
 WHERE c.estado = 'ATENDIDA' AND s.tipoRegistro = 'CLINICO'
   AND NOT EXISTS (SELECT 1 FROM historial_clinico h WHERE h.idCita = c.idCita)
UNION ALL
SELECT 'citas ATENDIDAS de vacunación sin carné', 0, COUNT(*)
  FROM cita c JOIN servicio s ON s.idServicio = c.Servicio_idServicio
 WHERE c.estado = 'ATENDIDA' AND s.tipoRegistro = 'VACUNACION'
   AND NOT EXISTS (SELECT 1 FROM carnet_vacuna v WHERE v.idCita = c.idCita)
UNION ALL
SELECT 'historias de una mascota distinta a la de su cita', 0, COUNT(*)
  FROM historial_clinico h JOIN cita c ON c.idCita = h.idCita WHERE c.idMascota <> h.idMascota
UNION ALL
SELECT 'historias cuyo veterinario no atendió la cita', 0, COUNT(*)
  FROM historial_clinico h JOIN cita c ON c.idCita = h.idCita WHERE c.Veterinario_idUsuario <> h.Veterinario_idUsuario
UNION ALL
SELECT 'vacunas aplicadas a otra especie', 0, COUNT(*)
  FROM carnet_vacuna cv JOIN vacuna v ON v.idVacuna = cv.idVacuna JOIN mascota m ON m.idMascota = cv.idMascota
 WHERE v.idEspecie <> m.idEspecie
UNION ALL
SELECT 'vacunas con lote vencido al aplicarse', 0, COUNT(*)
  FROM carnet_vacuna cv JOIN lote_vacuna l ON l.idLote = cv.idLote WHERE l.fechaVencimiento < cv.fechaAplicacion
UNION ALL
SELECT 'atenciones con fin antes del inicio', 0, COUNT(*)
  FROM cita WHERE finAtencion IS NOT NULL AND finAtencion <= inicioAtencion
UNION ALL
SELECT 'citas ATENDIDAS en fecha futura', 0, COUNT(*)
  FROM cita WHERE estado IN ('ATENDIDA', 'NO_ASISTIO') AND TIMESTAMP(fecha, hora) > NOW()
UNION ALL
SELECT 'reservas (PENDIENTE/CONFIRMADA) en horas pasadas', 0, COUNT(*)
  FROM cita WHERE estado IN ('PENDIENTE', 'CONFIRMADA') AND TIMESTAMP(fecha, hora) < NOW()
UNION ALL
SELECT 'reservas PENDIENTES ya vencidas', 0, COUNT(*)
  FROM cita WHERE estado = 'PENDIENTE' AND (expiraEn IS NULL OR expiraEn < NOW())
UNION ALL
SELECT 'franjas aprobadas del horizonte sin cubrir (la agenda automática crearía cupos)', 0, COUNT(*)
  FROM horario_veterinario h
  JOIN (SELECT CURDATE() + INTERVAL n DAY AS f
          FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
                UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14) d) dias
    ON WEEKDAY(dias.f) + 1 = h.diaSemana
 WHERE h.estado = 'APROBADO'
   AND (SELECT COALESCE(SUM(c.duracionMin), 0) FROM cita c
         WHERE c.Veterinario_idUsuario = h.idVeterinario AND c.fecha = dias.f
           AND c.estado IN ('DISPONIBLE', 'PENDIENTE', 'CONFIRMADA', 'EN_ATENCION', 'ATENDIDA')
           AND c.hora >= h.horaInicio AND c.hora < h.horaFin) < TIME_TO_SEC(TIMEDIFF(h.horaFin, h.horaInicio)) / 60
UNION ALL
SELECT 'citas CONFIRMADAS de las próximas 24 h sin recordatorio', 0, COUNT(*)
  FROM cita c JOIN mascota m ON m.idMascota = c.idMascota
 WHERE c.estado = 'CONFIRMADA' AND TIMESTAMP(c.fecha, c.hora) BETWEEN NOW() AND NOW() + INTERVAL 24 HOUR
   AND NOT EXISTS (SELECT 1 FROM notificacion n WHERE n.idUsuario = m.Tutor_idUsuario
                    AND n.claveUnica = CONCAT('cita-', c.idCita, '-recordatorio'))
UNION ALL
SELECT 'transferencias pendientes repetidas por mascota', 0, COUNT(*)
  FROM (SELECT idMascota FROM transferencia_mascota WHERE estado = 'PENDIENTE' GROUP BY idMascota HAVING COUNT(*) > 1) t
UNION ALL
SELECT 'conversaciones sin mensajes', 0, COUNT(*)
  FROM conversacion cv WHERE NOT EXISTS (SELECT 1 FROM mensaje_chat m WHERE m.idConversacion = cv.idConversacion)
UNION ALL
SELECT 'pasaportes de una mascota que no es del creador', 0, COUNT(*)
  FROM pasaporte p JOIN mascota m ON m.idMascota = p.idMascota WHERE m.Tutor_idUsuario <> p.idUsuarioCreador
UNION ALL
SELECT 'reportes registrados de usuarios inexistentes', 0, COUNT(*)
  FROM reporte_generado r LEFT JOIN usuario u ON u.idUsuario = r.idUsuario WHERE u.idUsuario IS NULL
UNION ALL
SELECT 'auditoría de usuarios inexistentes', 0, COUNT(*)
  FROM auditoria a LEFT JOIN usuario u ON u.idUsuario = a.idUsuario WHERE a.idUsuario IS NOT NULL AND u.idUsuario IS NULL
) comprobaciones;

-- Ubicaciones: participación de Bogotá (direcciones de dueños + sedes de las clínicas).
SELECT ubicacion, COUNT(*) AS registros, ROUND(100 * COUNT(*) / SUM(COUNT(*)) OVER (), 1) AS porcentaje FROM (
    SELECT IF(direccion LIKE '%Bogotá%', 'Bogotá D.C.', TRIM(SUBSTRING_INDEX(direccion, ',', -1))) AS ubicacion FROM tutor
    UNION ALL
    SELECT ci.nombre FROM ubicacion_veterinaria u
      JOIN barrio b ON b.idBarrio = u.idBarrio JOIN localidad l ON l.idLocalidad = b.idLocalidad
      JOIN ciudad ci ON ci.idCiudad = l.idCiudad
) x GROUP BY ubicacion ORDER BY registros DESC;

-- Citas por estado (para los reportes y calendarios).
SELECT estado, COUNT(*) AS citas,
       SUM(TIMESTAMP(fecha, hora) < NOW()) AS pasadas, SUM(TIMESTAMP(fecha, hora) >= NOW()) AS futuras
  FROM cita GROUP BY estado ORDER BY citas DESC;
