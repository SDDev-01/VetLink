-- =====================================================================
-- V8_permisos_fastapi.sql
-- Permisos mínimos del usuario de FastAPI (vetlink_fast) para los reportes PDF.
-- Se ejecuta sobre la base seleccionada (tablas sin prefijo de esquema).
-- Lo aplican database/init/00-init.sh (instalación nueva) y database/aplicar_V8.sh.
-- =====================================================================

-- Verificación de acceso (quién tiene relación clínica con la mascota).
GRANT SELECT ON cita TO 'vetlink_fast'@'%';
GRANT SELECT ON emergencia TO 'vetlink_fast'@'%';
GRANT SELECT (idUsuario, licencia, idVeterinaria) ON veterinario TO 'vetlink_fast'@'%';

-- Contenido del reporte (nunca documento, dirección, teléfono ni correo del tutor).
GRANT SELECT ON archivo_historial TO 'vetlink_fast'@'%';
GRANT SELECT ON lote_vacuna TO 'vetlink_fast'@'%';
GRANT SELECT ON ubicacion_veterinaria TO 'vetlink_fast'@'%';
GRANT SELECT ON telefono_veterinaria TO 'vetlink_fast'@'%';
GRANT SELECT ON correo_veterinaria TO 'vetlink_fast'@'%';

-- Bitácora de reportes generados: solo FastAPI escribe aquí (sin UPDATE ni DELETE).
GRANT SELECT, INSERT ON reporte_generado TO 'vetlink_fast'@'%';
