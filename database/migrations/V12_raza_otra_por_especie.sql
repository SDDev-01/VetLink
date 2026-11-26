-- =====================================================================
-- V12_raza_otra_por_especie.sql
-- Agrega la raza «Otra» al catálogo de cada especie que no la tenga.
--
-- El formulario de mascotas ya no tiene buscador ni el campo «Nombre de la raza»:
-- la raza se elige solo de la lista de la especie. «Otra» cubre las razas que no
-- están en el catálogo (Pez, Tortuga, Iguana, Hurón, Serpiente y Erizo no tenían
-- ninguna raza genérica). Las razas personalizadas ya registradas se conservan.
--
-- Solo inserta filas. Es idempotente (NOT EXISTS por especie).
-- =====================================================================
SET NAMES utf8mb4;

INSERT INTO raza (nombreRaza, idEspecie, personalizada)
SELECT 'Otra', e.idEspecie, 0
  FROM especie e
 WHERE NOT EXISTS (SELECT 1 FROM raza r WHERE r.idEspecie = e.idEspecie AND r.nombreRaza = 'Otra');
