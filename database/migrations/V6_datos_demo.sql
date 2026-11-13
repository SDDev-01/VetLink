-- =====================================================================
-- V6_datos_demo.sql  (OPCIONAL, separado de V5)
-- Deja los datos de prueba presentables para una demo (VL-27).
-- Solo INSERT/UPDATE; no borra filas. Ejecutar después de V5 y con respaldo.
-- Las comparaciones usan BINARY para tocar solo el texto exacto (la intercalación
-- general_ci no distingue mayúsculas ni tildes).
-- =====================================================================
SET NAMES utf8mb4;

-- 1) El usuario 4 (Carlos Rodriguez) no tiene rol y no puede iniciar sesión: se le da una fila en tutor.
--    (Es un INSERT porque la fila no existe; el NOT EXISTS evita duplicarla si se ejecuta dos veces.)
INSERT INTO tutor (idUsuario, direccion)
SELECT 4, 'Calle 45 # 12-30' FROM DUAL
WHERE EXISTS (SELECT 1 FROM usuario WHERE idUsuario = 4)
  AND NOT EXISTS (SELECT 1 FROM tutor WHERE idUsuario = 4);

-- 2) Ortografía y textos inapropiados en emergencias, historiales y descripciones de clínicas.

-- historial_clinico.motivoConsulta
UPDATE `historial_clinico` SET `motivoConsulta` = 'Caída de pelo' WHERE BINARY `motivoConsulta` = 'Caida de pelo'; -- 10 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Chequeo prequirúrgico' WHERE BINARY `motivoConsulta` = 'Chequeo prequirurgico'; -- 9 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Control de vacunación' WHERE BINARY `motivoConsulta` = 'Control de vacunacion'; -- 6 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Control geriátrico' WHERE BINARY `motivoConsulta` = 'Control geriatrico'; -- 5 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Deshidratación' WHERE BINARY `motivoConsulta` = 'Deshidratacion'; -- 9 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Diarrea' WHERE BINARY `motivoConsulta` = 'diarrea'; -- 1 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Infección de oído' WHERE BINARY `motivoConsulta` = 'Infeccion de oido'; -- 8 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Inflamación en pata' WHERE BINARY `motivoConsulta` = 'Inflamacion en pata'; -- 5 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Pérdida de apetito' WHERE BINARY `motivoConsulta` = 'Perdida de apetito'; -- 13 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Picazón en la piel' WHERE BINARY `motivoConsulta` = 'Picazon en la piel'; -- 8 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Reacción alérgica leve' WHERE BINARY `motivoConsulta` = 'Reaccion alergica leve'; -- 6 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Revisión dental' WHERE BINARY `motivoConsulta` = 'Revision dental'; -- 7 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Revisión posquirúrgica' WHERE BINARY `motivoConsulta` = 'Revision post quirurgica'; -- 16 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Sangrado en encías' WHERE BINARY `motivoConsulta` = 'Sangrado en encias'; -- 3 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Secreción nasal' WHERE BINARY `motivoConsulta` = 'Secrecion nasal'; -- 5 fila(s)
UPDATE `historial_clinico` SET `motivoConsulta` = 'Vómito' WHERE BINARY `motivoConsulta` = 'Vomito'; -- 3 fila(s)

-- historial_clinico.diagnostico
UPDATE `historial_clinico` SET `diagnostico` = 'Dermatitis alérgica' WHERE BINARY `diagnostico` = 'Dermatitis alergica'; -- 14 fila(s)
UPDATE `historial_clinico` SET `diagnostico` = 'Diarrea aguda' WHERE BINARY `diagnostico` = 'diarrea'; -- 1 fila(s)
UPDATE `historial_clinico` SET `diagnostico` = 'Estrés digestivo' WHERE BINARY `diagnostico` = 'Estres digestivo'; -- 8 fila(s)
UPDATE `historial_clinico` SET `diagnostico` = 'Infección respiratoria' WHERE BINARY `diagnostico` = 'Infeccion respiratoria'; -- 16 fila(s)
UPDATE `historial_clinico` SET `diagnostico` = 'Infección urinaria' WHERE BINARY `diagnostico` = 'Infeccion urinaria'; -- 12 fila(s)
UPDATE `historial_clinico` SET `diagnostico` = 'Reacción alérgica a picadura' WHERE BINARY `diagnostico` = 'Reaccion alergica a picadura'; -- 9 fila(s)
UPDATE `historial_clinico` SET `diagnostico` = 'Síndrome vestibular leve' WHERE BINARY `diagnostico` = 'Sindrome vestibular leve'; -- 8 fila(s)

-- historial_clinico.observaciones
UPDATE `historial_clinico` SET `observaciones` = 'Buena evolución clínica' WHERE BINARY `observaciones` = 'Buena evolucion clinica'; -- 9 fila(s)
UPDATE `historial_clinico` SET `observaciones` = 'Mejoría notable respecto a la consulta anterior' WHERE BINARY `observaciones` = 'Mejoria notable respecto a consulta anterior'; -- 24 fila(s)
UPDATE `historial_clinico` SET `observaciones` = 'Evitar que consuma desechos o alimentos en mal estado' WHERE BINARY `observaciones` = 'no coma basura'; -- 1 fila(s)
UPDATE `historial_clinico` SET `observaciones` = 'Se sugiere realizar exámenes de laboratorio de control' WHERE BINARY `observaciones` = 'Se sugiere realizar examenes de laboratorio de control'; -- 21 fila(s)

-- emergencia.motivo
UPDATE `emergencia` SET `motivo` = 'Paro respiratorio' WHERE BINARY `motivo` = 'morir'; -- 1 fila(s)
UPDATE `emergencia` SET `motivo` = 'Dificultad respiratoria' WHERE BINARY `motivo` = 'dificultad respiratoria'; -- 1 fila(s)
UPDATE `emergencia` SET `motivo` = 'Reacción alérgica grave' WHERE BINARY `motivo` = 'Reaccion alergica grave'; -- 2 fila(s)
UPDATE `emergencia` SET `motivo` = 'Vómito severo' WHERE BINARY `motivo` = 'Vomito severo'; -- 1 fila(s)

-- emergencia.detalle
UPDATE `emergencia` SET `detalle` = 'No respira; se inició reanimación' WHERE BINARY `detalle` = 'no rspira'; -- 1 fila(s)
UPDATE `emergencia` SET `detalle` = 'Presenta vómito' WHERE BINARY `detalle` = 'tienen vomito'; -- 1 fila(s)
UPDATE `emergencia` SET `detalle` = 'Llegó en estado crítico' WHERE BINARY `detalle` = 'Llego en estado critico'; -- 5 fila(s)
UPDATE `emergencia` SET `detalle` = 'Necesitó hospitalización' WHERE BINARY `detalle` = 'Necesito hospitalizacion'; -- 3 fila(s)
UPDATE `emergencia` SET `detalle` = 'Requirió atención inmediata' WHERE BINARY `detalle` = 'Requirio atencion inmediata'; -- 4 fila(s)
UPDATE `emergencia` SET `detalle` = 'Se trasladó a la sala de urgencias' WHERE BINARY `detalle` = 'Se traslado a la sala de urgencias'; -- 4 fila(s)

-- veterinaria.descripcion
UPDATE `veterinaria` SET `descripcion` = 'La mejor veterinaria de Bogotá' WHERE BINARY `descripcion` = 'La mejor veterinaria de Bogota'; -- 1 fila(s)
UPDATE `veterinaria` SET `descripcion` = 'Clínica veterinaria para perros y gatos' WHERE BINARY `descripcion` = '---'; -- 1 fila(s)
UPDATE `veterinaria` SET `descripcion` = 'Clínica veterinaria de barrio con atención personalizada' WHERE BINARY `descripcion` = 'clinica de prueba de don jose'; -- 1 fila(s)
UPDATE `veterinaria` SET `descripcion` = 'Atención veterinaria general para perros y gatos' WHERE BINARY `descripcion` = 'vet de prueba '; -- 1 fila(s)
UPDATE `veterinaria` SET `descripcion` = 'Atención veterinaria de confianza' WHERE BINARY `descripcion` = 'Atencion veterinaria de confianza'; -- 1 fila(s)
UPDATE `veterinaria` SET `descripcion` = 'Clínica veterinaria integral 24 horas' WHERE BINARY `descripcion` = 'Clinica veterinaria integral 24 horas'; -- 1 fila(s)
UPDATE `veterinaria` SET `descripcion` = 'Cuidado veterinario con más de 10 años de experiencia' WHERE BINARY `descripcion` = 'Cuidado veterinario con mas de 10 anios de experiencia'; -- 1 fila(s)
UPDATE `veterinaria` SET `descripcion` = 'Servicio veterinario rápido y confiable' WHERE BINARY `descripcion` = 'Servicio veterinario rapido y confiable'; -- 1 fila(s)

-- 3) Nombres de clínicas que conviene cambiar antes de una demo (los elige el equipo):
-- UPDATE veterinaria SET nombre = 'Nombre elegido por el equipo' WHERE idVeterinaria = 1;  -- hoy se llama 'VetLink', igual que la plataforma
-- UPDATE veterinaria SET nombre = 'Nombre elegido por el equipo' WHERE idVeterinaria = 2;  -- 'Vetlink2'
-- UPDATE veterinaria SET nombre = 'Nombre elegido por el equipo' WHERE idVeterinaria = 5;  -- 'clinica don jose'
-- UPDATE veterinaria SET nombre = 'Nombre elegido por el equipo' WHERE idVeterinaria = 6;  -- 'vetlolita'
