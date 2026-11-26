-- =====================================================================
-- permisos_fastapi.sql
-- Permisos mínimos de vetlink_fast (FastAPI) para el pasaporte de salud.
-- Lo aplica scripts/migrate.sh en cada ejecución (GRANT es idempotente),
-- seguido de migrations/V8_permisos_fastapi.sql (reportes PDF).
--
-- FastAPI escribe solo en sus tablas (sin DELETE) y lee únicamente lo que
-- muestra el pasaporte. Columnas leídas: docs/contrato-documentos.md, sección 5.
-- GRANT falla si una tabla no existe, así que también sirve de comprobación.
-- Si FastAPI necesita leer una tabla o columna nueva, se agrega aquí.
-- =====================================================================
GRANT SELECT, INSERT, UPDATE ON pasaporte TO 'vetlink_fast'@'%';
GRANT SELECT, INSERT, UPDATE ON pasaporte_acceso TO 'vetlink_fast'@'%';

GRANT SELECT ON mascota TO 'vetlink_fast'@'%';
GRANT SELECT ON especie TO 'vetlink_fast'@'%';
GRANT SELECT ON raza TO 'vetlink_fast'@'%';
GRANT SELECT ON carnet_vacuna TO 'vetlink_fast'@'%';
GRANT SELECT ON vacuna TO 'vetlink_fast'@'%';
GRANT SELECT ON historial_clinico TO 'vetlink_fast'@'%';
GRANT SELECT ON historial_medicamento TO 'vetlink_fast'@'%';
GRANT SELECT ON medicamento TO 'vetlink_fast'@'%';
GRANT SELECT ON veterinaria TO 'vetlink_fast'@'%';

-- Solo el nombre del veterinario; idUsuario hace falta para unirlo con historial_clinico.
GRANT SELECT (idUsuario, nombre, apellido) ON usuario TO 'vetlink_fast'@'%';
