-- =====================================================================
-- V7_pasaporte.sql
-- Pasaporte de salud veterinario (P4): tablas `pasaporte` y `pasaporte_acceso`.
--
-- Se ejecuta UNA SOLA VEZ, después de V5_correcciones.sql (V6 es opcional y no influye).
-- Solo crea tablas nuevas: no modifica tablas existentes ni toca datos.
-- No incluye datos de prueba; esos están en V7_pasaporte_demo.sql (opcional, no producción).
--
-- Tipos tomados de database/mydb.sql:
--   mascota.idMascota  INT(10) UNSIGNED  (PK)
--   usuario.idUsuario  INT(11)           (PK)
--   La mascota pertenece a un tutor por mascota.Tutor_idUsuario -> tutor.idUsuario -> usuario.idUsuario.
--
-- Ninguna llave foránea usa ON DELETE CASCADE: en MySQL el valor por omisión es
-- RESTRICT/NO ACTION, así que no se puede borrar una mascota, un usuario o un
-- pasaporte mientras tenga registros que dependan de él.
-- =====================================================================

-- ---------------------------------------------------------------------
-- pasaporte: enlace temporal y revocable a la información de salud de una mascota.
--   token        valor del enlace/QR. ascii_bin => comparación exacta (distingue
--                mayúsculas), necesario para tokens aleatorios tipo base64url.
--                Cabe un token aleatorio (p. ej. 43 caracteres) o un hash SHA-256 en hex (64).
--   revocado_en  NULL mientras el pasaporte no se haya revocado.
--   Pasaporte vigente:  revocado_en IS NULL AND expira_en > NOW()
-- ---------------------------------------------------------------------
CREATE TABLE pasaporte (
  idPasaporte      INT UNSIGNED NOT NULL AUTO_INCREMENT,
  token            VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
  idMascota        INT(10) UNSIGNED NOT NULL,
  idUsuarioCreador INT(11) NOT NULL,
  creado_en        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expira_en        DATETIME NOT NULL,
  revocado_en      DATETIME NULL DEFAULT NULL,
  CONSTRAINT pk_pasaporte PRIMARY KEY (idPasaporte),
  CONSTRAINT uk_pasaporte_token UNIQUE KEY (token),
  KEY idx_pasaporte_mascota (idMascota),
  KEY idx_pasaporte_creador (idUsuarioCreador),
  KEY idx_pasaporte_expira (expira_en),
  CONSTRAINT fk_pasaporte_mascota FOREIGN KEY (idMascota) REFERENCES mascota (idMascota),
  CONSTRAINT fk_pasaporte_usuario FOREIGN KEY (idUsuarioCreador) REFERENCES usuario (idUsuario),
  CONSTRAINT chk_pasaporte_expira CHECK (expira_en > creado_en),
  CONSTRAINT chk_pasaporte_revocado CHECK (revocado_en IS NULL OR revocado_en >= creado_en)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ---------------------------------------------------------------------
-- pasaporte_acceso: bitácora de cada apertura de un pasaporte.
--   ip          texto; VARCHAR(45) admite IPv4 e IPv6 (incluido IPv4 mapeado en IPv6).
--   user_agent  se recorta a 512 caracteres al guardarlo; NULL si el cliente no lo envía.
-- ---------------------------------------------------------------------
CREATE TABLE pasaporte_acceso (
  idAcceso      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  idPasaporte   INT UNSIGNED NOT NULL,
  consultado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ip            VARCHAR(45) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  user_agent    VARCHAR(512) NULL DEFAULT NULL,
  CONSTRAINT pk_pasaporte_acceso PRIMARY KEY (idAcceso),
  KEY idx_acceso_pasaporte_fecha (idPasaporte, consultado_en),
  CONSTRAINT fk_acceso_pasaporte FOREIGN KEY (idPasaporte) REFERENCES pasaporte (idPasaporte)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
