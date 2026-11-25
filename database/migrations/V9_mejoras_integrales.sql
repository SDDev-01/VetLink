-- =====================================================================
-- V9_mejoras_integrales.sql
-- Catálogo ampliado de razas (con razas personalizadas), trazabilidad de
-- emergencias (solicitante + registro clínico que la atiende).
--
-- NO borra filas ni columnas. Es idempotente: se puede ejecutar más de una
-- vez (cada cambio de estructura comprueba primero si ya existe).
-- En una instalación nueva lo aplica database/init/00-init.sh.
-- En una base existente: bash database/aplicar_V9.sh (hace respaldo antes).
-- =====================================================================

DELIMITER //
DROP PROCEDURE IF EXISTS vl_v9 //
CREATE PROCEDURE vl_v9()
BEGIN
    -- -----------------------------------------------------------------
    -- 1. Razas: el nombre deja de ser único global y pasa a ser único
    --    por especie (p. ej. «Mestizo» puede existir en varias especies).
    -- -----------------------------------------------------------------
    IF EXISTS (SELECT 1 FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'raza' AND INDEX_NAME = 'nombreRaza_UNIQUE') THEN
        ALTER TABLE raza DROP INDEX nombreRaza_UNIQUE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'raza' AND INDEX_NAME = 'uk_raza_nombre_especie') THEN
        ALTER TABLE raza ADD UNIQUE KEY uk_raza_nombre_especie (nombreRaza, idEspecie);
    END IF;
    -- Razas registradas por los dueños con la opción «Otra raza».
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'raza' AND COLUMN_NAME = 'personalizada') THEN
        ALTER TABLE raza ADD COLUMN personalizada TINYINT(1) NOT NULL DEFAULT 0;
    END IF;

    -- -----------------------------------------------------------------
    -- 2. Emergencias: quién la solicitó (se conserva aunque la mascota
    --    se transfiera después) y cuándo se atendió.
    -- -----------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emergencia' AND COLUMN_NAME = 'idSolicitante') THEN
        ALTER TABLE emergencia ADD COLUMN idSolicitante INT NULL,
            ADD CONSTRAINT fk_emergencia_solicitante FOREIGN KEY (idSolicitante) REFERENCES usuario (idUsuario);
        -- Las emergencias existentes se atribuyen al dueño actual de la mascota (único dato disponible).
        UPDATE emergencia e JOIN mascota m ON m.idMascota = e.idMascota
           SET e.idSolicitante = m.Tutor_idUsuario
         WHERE e.idSolicitante IS NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emergencia' AND COLUMN_NAME = 'atendidaEn') THEN
        ALTER TABLE emergencia ADD COLUMN atendidaEn DATETIME NULL;
    END IF;

    -- -----------------------------------------------------------------
    -- 3. Historia clínica ligada a la emergencia que la originó.
    --    UNIQUE: una emergencia se cierra con un único registro clínico
    --    (evita duplicados por reintentos o envíos simultáneos).
    -- -----------------------------------------------------------------
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historial_clinico' AND COLUMN_NAME = 'idEmergencia') THEN
        ALTER TABLE historial_clinico ADD COLUMN idEmergencia INT NULL,
            ADD UNIQUE KEY uk_historial_emergencia (idEmergencia),
            ADD CONSTRAINT fk_historial_emergencia FOREIGN KEY (idEmergencia) REFERENCES emergencia (idEmergencia);
    END IF;
END //
DELIMITER ;

CALL vl_v9();
DROP PROCEDURE vl_v9;

-- ---------------------------------------------------------------------
-- 4. Catálogo de razas. Solo agrega las que faltan (no toca ni renombra
--    las existentes, que pueden estar en uso por mascotas registradas).
-- ---------------------------------------------------------------------
INSERT INTO raza (nombreRaza, idEspecie)
SELECT v.nombre, v.idEspecie
FROM (
    -- Perro (1)
    SELECT 'Akita Inu' AS nombre, 1 AS idEspecie UNION ALL SELECT 'Alaskan Malamute', 1 UNION ALL
    SELECT 'American Bully', 1 UNION ALL SELECT 'American Staffordshire Terrier', 1 UNION ALL
    SELECT 'Basset Hound', 1 UNION ALL SELECT 'Bichón Frisé', 1 UNION ALL SELECT 'Bichón Maltés', 1 UNION ALL
    SELECT 'Bloodhound', 1 UNION ALL SELECT 'Boston Terrier', 1 UNION ALL SELECT 'Boyero de Berna', 1 UNION ALL
    SELECT 'Bull Terrier', 1 UNION ALL SELECT 'Cane Corso', 1 UNION ALL SELECT 'Cavalier King Charles', 1 UNION ALL
    SELECT 'Chow Chow', 1 UNION ALL SELECT 'Cocker Spaniel', 1 UNION ALL SELECT 'Collie', 1 UNION ALL
    SELECT 'Corgi Galés', 1 UNION ALL SELECT 'Criollo', 1 UNION ALL SELECT 'Dachshund (Salchicha)', 1 UNION ALL
    SELECT 'Dogo Argentino', 1 UNION ALL SELECT 'Fila Brasileño', 1 UNION ALL SELECT 'Galgo', 1 UNION ALL
    SELECT 'Gran Danés', 1 UNION ALL SELECT 'Jack Russell Terrier', 1 UNION ALL SELECT 'Lhasa Apso', 1 UNION ALL
    SELECT 'Mastín Napolitano', 1 UNION ALL SELECT 'Pastor Australiano', 1 UNION ALL SELECT 'Pastor Belga Malinois', 1 UNION ALL
    SELECT 'Pekinés', 1 UNION ALL SELECT 'Pinscher Miniatura', 1 UNION ALL SELECT 'Pitbull', 1 UNION ALL
    SELECT 'Pomerania', 1 UNION ALL SELECT 'Poodle (Caniche)', 1 UNION ALL SELECT 'San Bernardo', 1 UNION ALL
    SELECT 'Samoyedo', 1 UNION ALL SELECT 'Schnauzer', 1 UNION ALL SELECT 'Setter Irlandés', 1 UNION ALL
    SELECT 'Shar Pei', 1 UNION ALL SELECT 'Shiba Inu', 1 UNION ALL SELECT 'Terranova', 1 UNION ALL
    SELECT 'Weimaraner', 1 UNION ALL SELECT 'West Highland White Terrier', 1 UNION ALL SELECT 'Whippet', 1 UNION ALL
    SELECT 'Yorkshire Terrier', 1 UNION ALL
    -- Gato (2)
    SELECT 'Abisinio', 2 UNION ALL SELECT 'American Shorthair', 2 UNION ALL SELECT 'Bobtail Japonés', 2 UNION ALL
    SELECT 'Bombay', 2 UNION ALL SELECT 'Burmés', 2 UNION ALL SELECT 'Cornish Rex', 2 UNION ALL
    SELECT 'Criollo (Común Europeo)', 2 UNION ALL SELECT 'Devon Rex', 2 UNION ALL SELECT 'Exótico de Pelo Corto', 2 UNION ALL
    SELECT 'Himalayo', 2 UNION ALL SELECT 'Manx', 2 UNION ALL SELECT 'Munchkin', 2 UNION ALL
    SELECT 'Noruego del Bosque', 2 UNION ALL SELECT 'Oriental', 2 UNION ALL SELECT 'Savannah', 2 UNION ALL
    SELECT 'Scottish Fold', 2 UNION ALL SELECT 'Siamés', 2 UNION ALL SELECT 'Siberiano', 2 UNION ALL
    SELECT 'Somalí', 2 UNION ALL SELECT 'Van Turco', 2 UNION ALL
    -- Ave (3)
    SELECT 'Cacatúa Ninfa', 3 UNION ALL SELECT 'Diamante Mandarín', 3 UNION ALL SELECT 'Gallina', 3 UNION ALL
    SELECT 'Jilguero', 3 UNION ALL SELECT 'Loro Gris Africano', 3 UNION ALL SELECT 'Paloma', 3 UNION ALL
    SELECT 'Pato', 3 UNION ALL SELECT 'Perico Australiano', 3 UNION ALL SELECT 'Periquito Monje', 3 UNION ALL
    SELECT 'Tucán', 3 UNION ALL SELECT 'Mestizo', 3 UNION ALL
    -- Conejo (4)
    SELECT 'Angora Inglés', 4 UNION ALL SELECT 'Californiano', 4 UNION ALL SELECT 'Enano Holandés', 4 UNION ALL
    SELECT 'Hotot Enano', 4 UNION ALL SELECT 'Lop Inglés', 4 UNION ALL SELECT 'Neozelandés', 4 UNION ALL
    SELECT 'Rex', 4 UNION ALL SELECT 'Mestizo', 4 UNION ALL
    -- Hámster (5)
    SELECT 'Campbell', 5 UNION ALL SELECT 'Dorado', 5 UNION ALL SELECT 'Panda', 5 UNION ALL
    SELECT 'Mestizo', 5 UNION ALL
    -- Cobayo (6)
    SELECT 'Coronet', 6 UNION ALL SELECT 'Cuy Criollo', 6 UNION ALL SELECT 'Rex', 6 UNION ALL
    SELECT 'Skinny', 6 UNION ALL SELECT 'Teddy', 6 UNION ALL SELECT 'Texel', 6 UNION ALL SELECT 'Mestizo', 6 UNION ALL
    -- Pez (7)
    SELECT 'Carpa Koi', 7 UNION ALL SELECT 'Disco', 7 UNION ALL SELECT 'Escalar', 7 UNION ALL
    SELECT 'Oscar', 7 UNION ALL SELECT 'Platy', 7 UNION ALL SELECT 'Pez Payaso', 7 UNION ALL
    SELECT 'Corydora', 7 UNION ALL SELECT 'Telescopio', 7 UNION ALL
    -- Tortuga (8)
    SELECT 'Tortuga Almizclera', 8 UNION ALL SELECT 'Tortuga Charapa', 8 UNION ALL SELECT 'Tortuga de Tierra Rusa', 8 UNION ALL
    SELECT 'Tortuga Hicotea', 8 UNION ALL SELECT 'Tortuga Mapa', 8 UNION ALL SELECT 'Tortuga Sulcata', 8 UNION ALL
    -- Iguana (9)
    SELECT 'Iguana del Desierto', 9 UNION ALL SELECT 'Iguana Rinoceronte', 9 UNION ALL SELECT 'Iguana Roja', 9 UNION ALL
    -- Hurón (10)
    SELECT 'Hurón Albino', 10 UNION ALL SELECT 'Hurón Angora', 10 UNION ALL SELECT 'Hurón Canela', 10 UNION ALL
    SELECT 'Hurón Panda', 10 UNION ALL SELECT 'Hurón Sable', 10 UNION ALL
    -- Serpiente (11)
    SELECT 'Boa Arcoíris', 11 UNION ALL SELECT 'Pitón Real', 11 UNION ALL SELECT 'Pitón Reticulada', 11 UNION ALL
    SELECT 'Serpiente Rey', 11 UNION ALL SELECT 'Serpiente de Leche', 11 UNION ALL
    -- Erizo (12)
    SELECT 'Erizo Argelino', 12 UNION ALL SELECT 'Erizo Egipcio', 12 UNION ALL SELECT 'Erizo Pigmeo', 12
) v
JOIN especie e ON e.idEspecie = v.idEspecie
WHERE NOT EXISTS (SELECT 1 FROM raza r WHERE r.idEspecie = v.idEspecie AND LOWER(r.nombreRaza) = LOWER(v.nombre));
