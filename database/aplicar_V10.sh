#!/usr/bin/env bash
# =====================================================================
# Aplica la migración V10 a una base de VetLink YA EXISTENTE (sin borrar datos).
# Uso, desde la carpeta del proyecto y con los contenedores encendidos:
#     bash database/aplicar_V10.sh
# 1) Hace un respaldo completo en ./respaldos/ (fuera del contenedor).
# 2) Ejecuta V10_reprogramacion_desbloqueo_aprobacion.sql (idempotente: se puede repetir).
# Si algo falla se detiene; el respaldo permite restaurar con:
#     docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"' < respaldos/<archivo>.sql
# =====================================================================
set -Eeuo pipefail

cd "$(dirname "$0")/.."
mkdir -p respaldos
marca=$(date +%Y%m%d-%H%M%S)
respaldo="respaldos/vetlink-antes-de-V10-${marca}.sql"

echo "[1/2] Respaldando la base en ${respaldo}..."
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump --single-transaction --routines -uroot "$MYSQL_DATABASE"' > "${respaldo}"
test -s "${respaldo}"

echo "[2/2] Aplicando V10_reprogramacion_desbloqueo_aprobacion.sql..."
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"' < database/migrations/V10_reprogramacion_desbloqueo_aprobacion.sql

echo "Listo. Reconstruye los servicios: docker compose up -d --build spring fastapi nginx"
