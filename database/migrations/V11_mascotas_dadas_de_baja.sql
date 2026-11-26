-- =====================================================================
-- V11_mascotas_dadas_de_baja.sql
-- Cierra lo que quedó abierto en mascotas dadas de baja (mascota.estado = 0).
--
-- Causa: versiones anteriores daban de baja una mascota (borrado lógico) sin
-- cancelar sus reservas. El seed de mydb.sql trae así a «Max» (idMascota 1): dada de
-- baja pero con la cita 277 PENDIENTE para el 2026-11-27. El dueño no veía a Max en
-- «Mis mascotas» pero sí en «Mis citas» y en el calendario del panel.
-- La aplicación ya no muestra al dueño mascotas dadas de baja en ninguna vista
-- (AccesoPacienteService.esDuenioVigente) y ninguna ruta da de baja mascotas; esta
-- migración corrige los datos que ya existen.
--
-- No borra filas. Es idempotente (cada sentencia filtra lo que aún está abierto).
-- 1. Reservas futuras (PENDIENTE / CONFIRMADA) -> CANCELADA, con su historial de estado.
--    El cupo no se vuelve a ofrecer automáticamente; recepción puede publicarlo de nuevo.
-- 2. Transferencias PENDIENTE -> CANCELADA.
-- 3. Pasaportes vigentes -> revocados (el dueño ya no puede gestionarlos).
-- =====================================================================
SET NAMES utf8mb4;

-- 1. Reservas futuras de mascotas dadas de baja.
CREATE TEMPORARY TABLE tmp_v11_citas AS
SELECT c.idCita, c.estado
  FROM cita c
  JOIN mascota m ON m.idMascota = c.idMascota
 WHERE m.estado = 0
   AND c.estado IN ('PENDIENTE', 'CONFIRMADA')
   AND TIMESTAMP(c.fecha, c.hora) > NOW();

INSERT INTO cita_historial_estado (idCita, estadoAnterior, estadoNuevo, idUsuario, comentario, fecha)
SELECT idCita, estado, 'CANCELADA', NULL,
       'Cancelada por el sistema: la mascota está dada de baja (migración V11)', NOW()
  FROM tmp_v11_citas;

UPDATE cita c
  JOIN tmp_v11_citas t ON t.idCita = c.idCita
   SET c.estado = 'CANCELADA', c.expiraEn = NULL;

DROP TEMPORARY TABLE tmp_v11_citas;

-- 2. Transferencias pendientes.
UPDATE transferencia_mascota t
  JOIN mascota m ON m.idMascota = t.idMascota
   SET t.estado = 'CANCELADA', t.resueltaEn = NOW()
 WHERE m.estado = 0 AND t.estado = 'PENDIENTE';

-- 3. Pasaportes vigentes.
UPDATE pasaporte p
  JOIN mascota m ON m.idMascota = p.idMascota
   SET p.revocado_en = NOW()
 WHERE m.estado = 0 AND p.revocado_en IS NULL AND p.expira_en > NOW();
