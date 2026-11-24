#!/usr/bin/env bash
# =====================================================================
# Aplica la migración V8 a una base de VetLink YA EXISTENTE (sin borrar datos).
# Uso, desde la carpeta del proyecto y con los contenedores encendidos:
#     bash database/aplicar_V8.sh
# 1) Hace un respaldo completo en ./respaldos/ (fuera del contenedor).
# 2) Ejecuta V8_reglas_negocio.sql y los permisos de FastAPI.
# 3) Da a Spring permisos DML sobre las tablas nuevas.
# Si algo falla se detiene; el respaldo permite restaurar con:
#     docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"' < respaldos/<archivo>.sql
# =====================================================================
set -Eeuo pipefail

cd "$(dirname "$0")/.."
mkdir -p respaldos
marca=$(date +%Y%m%d-%H%M%S)
respaldo="respaldos/vetlink-antes-de-V8-${marca}.sql"

echo "[1/4] Respaldando la base en ${respaldo}..."
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump --single-transaction --routines -uroot "$MYSQL_DATABASE"' > "${respaldo}"
test -s "${respaldo}"

echo "[2/4] ¿Ya está aplicada V8?"
if docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -N -uroot "$MYSQL_DATABASE" -e "SHOW TABLES LIKE '"'"'cita_historial_estado'"'"'"' | grep -q cita_historial_estado; then
    echo "V8 ya estaba aplicada; solo se refrescan permisos."
else
    echo "Aplicando V8_reglas_negocio.sql..."
    docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"' < database/migrations/V8_reglas_negocio.sql
fi

echo "[3/4] Permisos de FastAPI (reportes)..."
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot "$MYSQL_DATABASE"' < database/migrations/V8_permisos_fastapi.sql

echo "[4/4] Permisos de Spring sobre las tablas nuevas..."
for t in cita_historial_estado solicitud_disponibilidad horario_veterinario ausencia_veterinario veterinario_servicio \
         mascota_evento transferencia_mascota notificacion auditoria; do
    docker compose exec -T mysql sh -c "MYSQL_PWD=\"\$MYSQL_ROOT_PASSWORD\" mysql -uroot \"\$MYSQL_DATABASE\" -e \"GRANT SELECT, INSERT, UPDATE, DELETE ON \\\`\$MYSQL_DATABASE\\\`.\\\`${t}\\\` TO 'vetlink_spring'@'%';\""
done

echo "Listo. Reinicia los servicios: docker compose up -d --build spring fastapi nginx"
