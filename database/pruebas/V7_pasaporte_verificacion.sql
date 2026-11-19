-- =====================================================================
-- V7_pasaporte_verificacion.sql  (SOLO DESARROLLO)
-- Comprobaciones de V7_pasaporte.sql + V7_pasaporte_demo.sql.
-- Las consultas 1-8 solo leen. Las pruebas 9 y 10 intentan INSERT inválidos
-- que MySQL DEBE rechazar (error 3819 y 1452); van dentro de una transacción
-- con ROLLBACK por si acaso. Ejecutar con:  mysql --force mydb < este_archivo
-- (--force para que siga después de los errores esperados).
-- =====================================================================

-- 1) Ambas tablas existen
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('pasaporte', 'pasaporte_acceso');

-- Restricciones creadas (PK, UNIQUE, FK, CHECK)
SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('pasaporte', 'pasaporte_acceso')
ORDER BY TABLE_NAME, CONSTRAINT_TYPE;

-- Ninguna FK con CASCADE
SELECT CONSTRAINT_NAME, TABLE_NAME, REFERENCED_TABLE_NAME, DELETE_RULE, UPDATE_RULE
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME IN ('pasaporte', 'pasaporte_acceso');

-- 2) Cinco pasaportes DEMO
SELECT COUNT(*) AS pasaportes_demo FROM pasaporte WHERE token LIKE 'DEMO-PASAPORTE-%';

-- 3) Registros de acceso DEMO y su reparto por pasaporte
SELECT COUNT(*) AS accesos_demo FROM pasaporte_acceso WHERE user_agent LIKE '%DEMO-VetLink-acceso-%';
SELECT p.token, COUNT(a.idAcceso) AS aperturas
FROM pasaporte p LEFT JOIN pasaporte_acceso a ON a.idPasaporte = p.idPasaporte
WHERE p.token LIKE 'DEMO-PASAPORTE-%'
GROUP BY p.idPasaporte, p.token ORDER BY p.token;

-- 4) Tokens únicos (debe devolver 0 filas)
SELECT token, COUNT(*) FROM pasaporte GROUP BY token HAVING COUNT(*) > 1;

-- 5) Relaciones: mascota existe, creador existe y es el tutor de la mascota
SELECT p.idPasaporte, p.token, m.idMascota, m.nombre AS mascota,
       u.idUsuario, CONCAT(u.nombre, ' ', u.apellido) AS creador,
       (m.Tutor_idUsuario = p.idUsuarioCreador) AS creador_es_tutor
FROM pasaporte p
JOIN mascota m ON m.idMascota = p.idMascota
JOIN usuario u ON u.idUsuario = p.idUsuarioCreador
WHERE p.token LIKE 'DEMO-PASAPORTE-%'
ORDER BY p.idPasaporte;

-- Aperturas fuera del periodo válido de su pasaporte (debe devolver 0 filas)
SELECT a.idAcceso, p.token, a.consultado_en
FROM pasaporte_acceso a JOIN pasaporte p ON p.idPasaporte = a.idPasaporte
WHERE a.consultado_en < p.creado_en
   OR a.consultado_en > p.expira_en
   OR (p.revocado_en IS NOT NULL AND a.consultado_en > p.revocado_en);

-- 6, 7, 8) Estado de cada pasaporte (ACTIVO / VENCIDO / REVOCADO)
SELECT idPasaporte, token, creado_en, expira_en, revocado_en,
       CASE
         WHEN revocado_en IS NOT NULL AND revocado_en <= NOW() THEN 'REVOCADO'
         WHEN expira_en <= NOW()                              THEN 'VENCIDO'
         ELSE 'ACTIVO'
       END AS estado
FROM pasaporte
WHERE token LIKE 'DEMO-PASAPORTE-%'
ORDER BY idPasaporte;

-- 9 y 10) Restricciones que deben FALLAR
START TRANSACTION;

-- 9) Vencimiento anterior a la creación -> esperado ERROR 3819 (chk_pasaporte_expira)
INSERT INTO pasaporte (token, idMascota, idUsuarioCreador, creado_en, expira_en)
SELECT 'DEMO-PRUEBA-FECHA-INVALIDA', m.idMascota, m.Tutor_idUsuario, NOW(), NOW() - INTERVAL 1 DAY
FROM mascota m ORDER BY m.idMascota LIMIT 1;

-- 10) Mascota inexistente -> esperado ERROR 1452 (fk_pasaporte_mascota)
INSERT INTO pasaporte (token, idMascota, idUsuarioCreador, creado_en, expira_en)
SELECT 'DEMO-PRUEBA-MASCOTA-INEXISTENTE', (SELECT MAX(idMascota) + 1000 FROM mascota),
       MIN(u.idUsuario), NOW(), NOW() + INTERVAL 1 DAY
FROM usuario u;

ROLLBACK;

-- Ninguna de las filas inválidas quedó guardada (debe ser 0)
SELECT COUNT(*) AS filas_invalidas FROM pasaporte WHERE token LIKE 'DEMO-PRUEBA-%';
