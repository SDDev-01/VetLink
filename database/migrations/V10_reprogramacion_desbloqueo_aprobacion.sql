-- =====================================================================
-- V10_reprogramacion_desbloqueo_aprobacion.sql
-- 1. cita: respuesta del dueño a una reprogramación propuesta por la clínica.
-- 2. ausencia_veterinario: quién y cuándo desbloqueó un bloqueo de agenda.
-- 3. veterinaria: fecha y tipo de aprobación (aprobación automática al registrarse).
--
-- Solo AGREGA columnas: no borra filas, columnas ni tablas. Es idempotente
-- (cada cambio comprueba antes si ya existe), así que se puede repetir.
-- Instalación nueva: la aplica database/init/00-init.sh.
-- Base existente: bash database/aplicar_V10.sh (hace un respaldo antes).
-- =====================================================================

DELIMITER //
DROP PROCEDURE IF EXISTS vl_v10 //
CREATE PROCEDURE vl_v10()
BEGIN
    -- -----------------------------------------------------------------
    -- 1. Cita reprogramada por la clínica: el dueño confirma, modifica o cancela.
    --    NULL = la cita no requiere respuesta del dueño.
    -- -----------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cita' AND COLUMN_NAME = 'respuestaDuenio') THEN
        ALTER TABLE cita ADD COLUMN respuestaDuenio VARCHAR(20) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cita' AND COLUMN_NAME = 'respuestaDuenioEn') THEN
        ALTER TABLE cita ADD COLUMN respuestaDuenioEn DATETIME NULL;
    END IF;

    -- -----------------------------------------------------------------
    -- 2. Desbloqueo de agenda: trazabilidad de quién lo hizo y cuándo.
    -- -----------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ausencia_veterinario' AND COLUMN_NAME = 'desbloqueadaPor') THEN
        ALTER TABLE ausencia_veterinario ADD COLUMN desbloqueadaPor INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ausencia_veterinario' AND COLUMN_NAME = 'desbloqueadaEn') THEN
        ALTER TABLE ausencia_veterinario ADD COLUMN desbloqueadaEn DATETIME NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ausencia_veterinario'
                     AND CONSTRAINT_NAME = 'fk_ausencia_desbloqueada_por') THEN
        ALTER TABLE ausencia_veterinario
            ADD CONSTRAINT fk_ausencia_desbloqueada_por FOREIGN KEY (desbloqueadaPor) REFERENCES usuario (idUsuario);
    END IF;

    -- -----------------------------------------------------------------
    -- 3. Aprobación de veterinarias. Las existentes conservan su estado;
    --    a las ya activas no se les inventa una fecha de aprobación.
    -- -----------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'veterinaria' AND COLUMN_NAME = 'aprobadaEn') THEN
        ALTER TABLE veterinaria ADD COLUMN aprobadaEn DATETIME NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'veterinaria' AND COLUMN_NAME = 'aprobacionAutomatica') THEN
        ALTER TABLE veterinaria ADD COLUMN aprobacionAutomatica TINYINT(1) NOT NULL DEFAULT 0;
    END IF;
END //
DELIMITER ;

CALL vl_v10();
DROP PROCEDURE IF EXISTS vl_v10;
