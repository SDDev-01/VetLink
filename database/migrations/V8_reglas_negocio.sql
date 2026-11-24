-- =====================================================================
-- V8_reglas_negocio.sql
-- Máquina de estados de citas, disponibilidad por veterinario, cupos
-- automáticos, control de concurrencia, atención clínica, transferencias,
-- notificaciones, auditoría y reportes PDF.
--
-- Se ejecuta UNA SOLA VEZ, después de V7 (y de los datos demo si se usan).
-- En una instalación nueva lo aplica database/init/00-init.sh.
-- En una base existente: database/aplicar_V8.sh (hace respaldo antes).
--
-- No borra filas. El único cambio sobre datos existentes es:
--   * cita.estado 'AGENDADA' pasa a llamarse 'CONFIRMADA' (mismo significado:
--     cita reservada y aceptada por la clínica).
--   * Se rellenan columnas nuevas (duración, veterinaria del historial) con
--     valores derivados de datos ya existentes.
--   * Se siembra un horario APROBADO por veterinario a partir del horario de su
--     clínica y los servicios de la clínica como servicios autorizados
--     (supuesto documentado en REGLAS_NEGOCIO.md; la recepción puede ajustarlos).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Estados de cita
-- ---------------------------------------------------------------------
ALTER TABLE cita MODIFY estado ENUM('DISPONIBLE','AGENDADA','PENDIENTE','CONFIRMADA','EN_ATENCION','ATENDIDA',
                                    'CANCELADA','RECHAZADA','NO_ASISTIO','REPROGRAMADA')
    NOT NULL DEFAULT 'DISPONIBLE';
UPDATE cita SET estado = 'CONFIRMADA' WHERE estado = 'AGENDADA';
ALTER TABLE cita MODIFY estado ENUM('DISPONIBLE','PENDIENTE','CONFIRMADA','EN_ATENCION','ATENDIDA',
                                    'CANCELADA','RECHAZADA','NO_ASISTIO','REPROGRAMADA')
    NOT NULL DEFAULT 'DISPONIBLE';

-- ---------------------------------------------------------------------
-- 2. Datos de la cita
-- ---------------------------------------------------------------------
ALTER TABLE cita
    ADD COLUMN duracionMin INT NULL,
    ADD COLUMN motivo VARCHAR(500) NULL,
    ADD COLUMN observaciones TEXT NULL,
    ADD COLUMN indicacionesPrevias TEXT NULL,
    ADD COLUMN observacionesAdmin TEXT NULL,
    ADD COLUMN creadaEn DATETIME NULL,
    ADD COLUMN reservadaEn DATETIME NULL,
    ADD COLUMN expiraEn DATETIME NULL,
    ADD COLUMN inicioAtencion DATETIME NULL,
    ADD COLUMN finAtencion DATETIME NULL,
    ADD COLUMN idCitaOrigen INT NULL,
    ADD COLUMN generadaAuto TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN version INT NOT NULL DEFAULT 0;

UPDATE cita c
JOIN servicio_veterinaria sv
  ON sv.idServicio = c.Servicio_idServicio AND sv.idVeterinaria = c.Veterinaria_idVeterinaria
SET c.duracionMin = sv.duracionAprox
WHERE c.duracionMin IS NULL AND sv.duracionAprox > 0;
UPDATE cita SET duracionMin = 30 WHERE duracionMin IS NULL OR duracionMin <= 0;
ALTER TABLE cita MODIFY duracionMin INT NOT NULL DEFAULT 30;

ALTER TABLE cita
    ADD CONSTRAINT fk_cita_origen FOREIGN KEY (idCitaOrigen) REFERENCES cita (idCita),
    ADD KEY idx_cita_clinica_fecha (Veterinaria_idVeterinaria, fecha, estado);

-- Última barrera contra reservas duplicadas: un veterinario (y una mascota) no puede
-- tener dos citas ocupadas que empiecen a la misma hora. Los cupos DISPONIBLES y las
-- citas cerradas (canceladas, rechazadas...) dan NULL y no cuentan para la unicidad.
-- El solapamiento por duración se controla en la aplicación con bloqueo pesimista.
ALTER TABLE cita
    ADD COLUMN ocupaHorario TINYINT GENERATED ALWAYS AS
        (IF(estado IN ('PENDIENTE','CONFIRMADA','EN_ATENCION','ATENDIDA'), 1, NULL)) STORED,
    ADD UNIQUE KEY uk_cita_veterinario_ocupado (Veterinario_idUsuario, fecha, hora, ocupaHorario),
    ADD UNIQUE KEY uk_cita_mascota_ocupado (idMascota, fecha, hora, ocupaHorario);

CREATE TABLE cita_historial_estado (
    idHistorialEstado INT AUTO_INCREMENT PRIMARY KEY,
    idCita INT NOT NULL,
    estadoAnterior VARCHAR(20) NULL,
    estadoNuevo VARCHAR(20) NOT NULL,
    idUsuario INT NULL,
    comentario VARCHAR(500) NULL,
    fecha DATETIME NOT NULL,
    KEY idx_historial_estado_cita (idCita, fecha),
    CONSTRAINT fk_historial_estado_cita FOREIGN KEY (idCita) REFERENCES cita (idCita),
    CONSTRAINT fk_historial_estado_usuario FOREIGN KEY (idUsuario) REFERENCES usuario (idUsuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- 3. Tipo de registro que exige cada servicio al finalizar la atención
-- ---------------------------------------------------------------------
ALTER TABLE servicio
    ADD COLUMN tipoRegistro VARCHAR(20) NOT NULL DEFAULT 'CLINICO',
    ADD COLUMN esEmergencia TINYINT(1) NOT NULL DEFAULT 0;
UPDATE servicio SET tipoRegistro = 'VACUNACION' WHERE nombreServicio LIKE 'Vacunaci%';
UPDATE servicio SET tipoRegistro = 'NINGUNO'
 WHERE nombreServicio LIKE 'Peluquer%' OR nombreServicio LIKE 'Hotel%' OR nombreServicio LIKE 'Guarder%'
    OR nombreServicio LIKE 'Venta de%';
UPDATE servicio SET esEmergencia = 1 WHERE nombreServicio LIKE 'Urgencia%' OR nombreServicio LIKE 'Emergencia%';

-- ---------------------------------------------------------------------
-- 4. Historia clínica estructurada y ligada a la cita
-- ---------------------------------------------------------------------
ALTER TABLE historial_clinico
    ADD COLUMN idCita INT NULL,
    ADD COLUMN idVeterinaria INT NULL,
    ADD COLUMN tipoRegistro VARCHAR(30) NULL,
    ADD COLUMN antecedentes TEXT NULL,
    ADD COLUMN temperatura DOUBLE NULL,
    ADD COLUMN frecuenciaCardiaca INT NULL,
    ADD COLUMN frecuenciaRespiratoria INT NULL,
    ADD COLUMN pesoKg DOUBLE NULL,
    ADD COLUMN evaluacionClinica TEXT NULL,
    ADD COLUMN hallazgos TEXT NULL,
    ADD COLUMN tratamiento TEXT NULL,
    ADD COLUMN medicamentosIndicados TEXT NULL,
    ADD COLUMN recomendaciones TEXT NULL,
    ADD UNIQUE KEY uk_historial_cita (idCita),
    ADD KEY idx_historial_veterinaria (idVeterinaria),
    ADD CONSTRAINT fk_historial_cita FOREIGN KEY (idCita) REFERENCES cita (idCita),
    ADD CONSTRAINT fk_historial_veterinaria FOREIGN KEY (idVeterinaria) REFERENCES veterinaria (idVeterinaria);

-- Registros antiguos: la clínica responsable es la del veterinario que los firmó.
UPDATE historial_clinico h
JOIN veterinario v ON v.idUsuario = h.Veterinario_idUsuario
SET h.idVeterinaria = v.idVeterinaria
WHERE h.idVeterinaria IS NULL;

ALTER TABLE carnet_vacuna
    ADD COLUMN idCita INT NULL,
    ADD COLUMN idVeterinaria INT NULL,
    ADD KEY idx_carnet_cita (idCita),
    ADD CONSTRAINT fk_carnet_cita FOREIGN KEY (idCita) REFERENCES cita (idCita),
    ADD CONSTRAINT fk_carnet_veterinaria FOREIGN KEY (idVeterinaria) REFERENCES veterinaria (idVeterinaria);

UPDATE carnet_vacuna c
JOIN veterinario v ON v.idUsuario = c.idUsuario
SET c.idVeterinaria = v.idVeterinaria
WHERE c.idVeterinaria IS NULL;

ALTER TABLE archivo_historial
    ADD COLUMN tamanoBytes BIGINT NULL,
    ADD COLUMN subidoEn DATETIME NULL,
    ADD COLUMN idUsuario INT NULL,
    ADD CONSTRAINT fk_archivo_usuario FOREIGN KEY (idUsuario) REFERENCES usuario (idUsuario);

-- ---------------------------------------------------------------------
-- 5. Disponibilidad de veterinarios (con aprobación de recepción)
-- ---------------------------------------------------------------------
CREATE TABLE solicitud_disponibilidad (
    idSolicitud INT AUTO_INCREMENT PRIMARY KEY,
    idVeterinario INT NOT NULL,
    idVeterinaria INT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    descripcion VARCHAR(500) NULL,
    creadaEn DATETIME NOT NULL,
    revisadaPor INT NULL,
    revisadaEn DATETIME NULL,
    observacionRevision VARCHAR(500) NULL,
    KEY idx_solicitud_clinica_estado (idVeterinaria, estado),
    CONSTRAINT fk_solicitud_veterinario FOREIGN KEY (idVeterinario) REFERENCES veterinario (idUsuario),
    CONSTRAINT fk_solicitud_veterinaria FOREIGN KEY (idVeterinaria) REFERENCES veterinaria (idVeterinaria),
    CONSTRAINT fk_solicitud_revisor FOREIGN KEY (revisadaPor) REFERENCES usuario (idUsuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- diaSemana: 1 = lunes ... 7 = domingo (ISO-8601, igual que java.time.DayOfWeek).
CREATE TABLE horario_veterinario (
    idHorarioVeterinario INT AUTO_INCREMENT PRIMARY KEY,
    idVeterinario INT NOT NULL,
    diaSemana INT NOT NULL,
    horaInicio TIME NOT NULL,
    horaFin TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    idSolicitud INT NULL,
    creadoEn DATETIME NOT NULL,
    KEY idx_horario_vet_estado (idVeterinario, estado),
    CONSTRAINT chk_horario_vet_dia CHECK (diaSemana BETWEEN 1 AND 7),
    CONSTRAINT chk_horario_vet_rango CHECK (horaInicio < horaFin),
    CONSTRAINT fk_horario_vet_veterinario FOREIGN KEY (idVeterinario) REFERENCES veterinario (idUsuario),
    CONSTRAINT fk_horario_vet_solicitud FOREIGN KEY (idSolicitud) REFERENCES solicitud_disponibilidad (idSolicitud)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE ausencia_veterinario (
    idAusencia INT AUTO_INCREMENT PRIMARY KEY,
    idVeterinario INT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    inicio DATETIME NOT NULL,
    fin DATETIME NOT NULL,
    motivo VARCHAR(500) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    idSolicitud INT NULL,
    creadoEn DATETIME NOT NULL,
    KEY idx_ausencia_vet (idVeterinario, estado, inicio),
    CONSTRAINT chk_ausencia_rango CHECK (inicio < fin),
    CONSTRAINT fk_ausencia_veterinario FOREIGN KEY (idVeterinario) REFERENCES veterinario (idUsuario),
    CONSTRAINT fk_ausencia_solicitud FOREIGN KEY (idSolicitud) REFERENCES solicitud_disponibilidad (idSolicitud)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE veterinario_servicio (
    idVeterinarioServicio INT AUTO_INCREMENT PRIMARY KEY,
    idVeterinario INT NOT NULL,
    idServicio INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    idSolicitud INT NULL,
    creadoEn DATETIME NOT NULL,
    KEY idx_vet_servicio (idVeterinario, estado),
    CONSTRAINT fk_vet_servicio_veterinario FOREIGN KEY (idVeterinario) REFERENCES veterinario (idUsuario),
    CONSTRAINT fk_vet_servicio_servicio FOREIGN KEY (idServicio) REFERENCES servicio (idServicio),
    CONSTRAINT fk_vet_servicio_solicitud FOREIGN KEY (idSolicitud) REFERENCES solicitud_disponibilidad (idSolicitud)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Siembra inicial (supuesto): el horario válido de la clínica es el horario aprobado de
-- cada uno de sus veterinarios, y todos los servicios de la clínica quedan autorizados.
INSERT INTO horario_veterinario (idVeterinario, diaSemana, horaInicio, horaFin, estado, creadoEn)
SELECT v.idUsuario, h.idDiaSemana, h.horaApertura, h.horaCierre, 'APROBADO', NOW()
FROM veterinario v
JOIN horario_veterinaria h ON h.idVeterinaria = v.idVeterinaria
WHERE h.estado = 1 AND h.horaApertura < h.horaCierre AND h.idDiaSemana BETWEEN 1 AND 7;

INSERT INTO veterinario_servicio (idVeterinario, idServicio, estado, creadoEn)
SELECT v.idUsuario, sv.idServicio, 'APROBADO', NOW()
FROM veterinario v
JOIN servicio_veterinaria sv ON sv.idVeterinaria = v.idVeterinaria;

-- ---------------------------------------------------------------------
-- 6. Mascotas: situación, eventos y transferencias
-- ---------------------------------------------------------------------
ALTER TABLE mascota ADD COLUMN situacion VARCHAR(20) NOT NULL DEFAULT 'ACTIVA';

CREATE TABLE mascota_evento (
    idEvento INT AUTO_INCREMENT PRIMARY KEY,
    idMascota INT(10) UNSIGNED NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    detalle VARCHAR(500) NULL,
    valorAnterior VARCHAR(60) NULL,
    valorNuevo VARCHAR(60) NULL,
    idUsuario INT NULL,
    fecha DATETIME NOT NULL,
    KEY idx_evento_mascota (idMascota, fecha),
    CONSTRAINT fk_evento_mascota FOREIGN KEY (idMascota) REFERENCES mascota (idMascota),
    CONSTRAINT fk_evento_usuario FOREIGN KEY (idUsuario) REFERENCES usuario (idUsuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE transferencia_mascota (
    idTransferencia INT AUTO_INCREMENT PRIMARY KEY,
    idMascota INT(10) UNSIGNED NOT NULL,
    idTutorOrigen INT NOT NULL,
    idTutorDestino INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    mensaje VARCHAR(300) NULL,
    creadaEn DATETIME NOT NULL,
    resueltaEn DATETIME NULL,
    -- Una sola transferencia PENDIENTE por mascota (evita transferencias simultáneas).
    pendiente TINYINT GENERATED ALWAYS AS (IF(estado = 'PENDIENTE', 1, NULL)) STORED,
    UNIQUE KEY uk_transferencia_pendiente (idMascota, pendiente),
    KEY idx_transferencia_destino (idTutorDestino, estado),
    CONSTRAINT chk_transferencia_distinto CHECK (idTutorOrigen <> idTutorDestino),
    CONSTRAINT fk_transferencia_mascota FOREIGN KEY (idMascota) REFERENCES mascota (idMascota),
    CONSTRAINT fk_transferencia_origen FOREIGN KEY (idTutorOrigen) REFERENCES tutor (idUsuario),
    CONSTRAINT fk_transferencia_destino FOREIGN KEY (idTutorDestino) REFERENCES tutor (idUsuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- 7. Chat: no leídos y orden por último mensaje
-- ---------------------------------------------------------------------
ALTER TABLE mensaje_chat ADD COLUMN leidoEn DATETIME(6) NULL;
ALTER TABLE conversacion
    ADD COLUMN ultimoMensajeEn DATETIME(6) NULL,
    ADD COLUMN idVeterinaria INT NULL,
    ADD CONSTRAINT fk_conversacion_veterinaria FOREIGN KEY (idVeterinaria) REFERENCES veterinaria (idVeterinaria);
UPDATE conversacion c
SET c.ultimoMensajeEn = (SELECT MAX(m.enviadoEn) FROM mensaje_chat m WHERE m.idConversacion = c.idConversacion);

-- ---------------------------------------------------------------------
-- 8. Notificaciones persistentes (claveUnica evita duplicados)
-- ---------------------------------------------------------------------
CREATE TABLE notificacion (
    idNotificacion BIGINT AUTO_INCREMENT PRIMARY KEY,
    idUsuario INT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    enlace VARCHAR(300) NULL,
    claveUnica VARCHAR(150) NULL,
    leida TINYINT(1) NOT NULL DEFAULT 0,
    creadaEn DATETIME NOT NULL,
    UNIQUE KEY uk_notificacion_clave (idUsuario, claveUnica),
    KEY idx_notificacion_usuario (idUsuario, leida, creadaEn),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (idUsuario) REFERENCES usuario (idUsuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- 9. Auditoría de acciones sensibles
-- ---------------------------------------------------------------------
CREATE TABLE auditoria (
    idAuditoria BIGINT AUTO_INCREMENT PRIMARY KEY,
    idUsuario INT NULL,
    accion VARCHAR(60) NOT NULL,
    entidad VARCHAR(40) NOT NULL,
    idEntidad VARCHAR(40) NULL,
    detalle VARCHAR(1000) NULL,
    ip VARCHAR(45) NULL,
    fecha DATETIME NOT NULL,
    KEY idx_auditoria_entidad (entidad, idEntidad),
    KEY idx_auditoria_usuario (idUsuario, fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- 10. Reportes PDF generados por FastAPI (bitácora)
-- ---------------------------------------------------------------------
CREATE TABLE reporte_generado (
    idReporte BIGINT AUTO_INCREMENT PRIMARY KEY,
    idUsuario INT NOT NULL,
    rol VARCHAR(20) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    idMascota INT(10) UNSIGNED NULL,
    numeroHistorial INT NULL,
    parametros VARCHAR(300) NULL,
    ip VARCHAR(45) NULL,
    generadoEn DATETIME NOT NULL,
    KEY idx_reporte_usuario (idUsuario, generadoEn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
