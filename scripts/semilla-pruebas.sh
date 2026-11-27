#!/usr/bin/env bash
# =====================================================================
# scripts/semilla-pruebas.sh — carga los datos de prueba de VetLink en la base de DESARROLLO:
# exactamente 500 registros entre todas las tablas funcionales, con 32 cuentas de prueba.
#
# ACCIÓN DESTRUCTIVA: vacía TODAS las tablas funcionales (TRUNCATE: los ID vuelven a 1).
# No toca la estructura (tablas, columnas, índices, restricciones) ni el historial de
# migraciones (vetlink_migracion*). Solo para desarrollo.
#
#   bash scripts/semilla-pruebas.sh              vacía las tablas y carga la semilla (pide confirmación)
#   bash scripts/semilla-pruebas.sh --reset      antes, recrea la base desde cero con scripts/reset-db.sh
#                                                (respalda, borra el volumen de MySQL y aplica las migraciones)
#   bash scripts/semilla-pruebas.sh --si         sin preguntar
#   bash scripts/semilla-pruebas.sh --verificar  solo ejecuta las comprobaciones (no cambia nada)
#
# Pasos:
#   1. (--reset) reset-db.sh --si --sin-levantar, luego MySQL + servicio «migrate» (esquema completo).
#   2. Detiene Spring y FastAPI (sus tareas programadas no deben escribir durante la carga).
#   3. TRUNCATE de todas las tablas funcionales y carga de database/pruebas/semilla_500.py
#      (se ejecuta en un contenedor python:3.12-slim; no hace falta Python en el equipo).
#   4. Escribe los PDF de archivo_historial en el volumen de uploads.
#   5. Levanta todos los servicios y ejecuta las comprobaciones (conteo exacto de 500, roles,
#      estados de mascotas, integridad, ID desde 1, AUTO_INCREMENT sincronizado).
#
# Cuentas (contraseña 123456V): dueños 1..10@gmail.com, veterinarios 100..110@gmail.com,
# recepcionistas 1000..1010@gmail.com.
# =====================================================================
set -Eeuo pipefail
export MSYS_NO_PATHCONV=1   # Git Bash: no convertir rutas como /u en rutas de Windows

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${RAIZ}"

info()  { printf '[semilla] %s\n' "$*"; }
error() { printf '[semilla] ERROR: %s\n' "$*" >&2; exit 1; }

RESET=0; CONFIRMADO=0; SOLO_VERIFICAR=0
for arg in "$@"; do
    case "${arg}" in
        --reset) RESET=1 ;;
        --si) CONFIRMADO=1 ;;
        --verificar) SOLO_VERIFICAR=1 ;;
        -h|--help) sed -n '2,29p' "${BASH_SOURCE[0]}"; exit 0 ;;
        *) error "Opción desconocida: ${arg}" ;;
    esac
done

command -v docker >/dev/null 2>&1 || error "No se encontró «docker»."
docker compose version >/dev/null 2>&1 || error "«docker compose» no está disponible."
[[ -f .env ]] || error "No existe .env (ver README)."
if grep -Eq '^VETLINK_ENTORNO=produccion' .env; then
    error "VETLINK_ENTORNO=produccion en .env: este script es solo para desarrollo."
fi

IMAGEN_PYTHON="python:3.12-slim"
GENERADOR="database/pruebas/semilla_500.py"
VERIFICACION="database/pruebas/verificar_semilla_500.sql"

# Cliente MySQL como root dentro del contenedor (la contraseña no sale del contenedor).
mysql_root() { docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 "$MYSQL_DATABASE" "$@"' mysql "$@"; }
valor() { mysql_root --batch --skip-column-names -e "$1"; }

tablas_funcionales() {
    valor "SELECT TABLE_NAME FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
              AND TABLE_NAME NOT LIKE 'vetlink\\_migracion%' ORDER BY TABLE_NAME" | tr -d '\r'
}

verificar() {
    info "Conteo exacto por tabla (todas las tablas funcionales):"
    local consulta="" t
    while IFS= read -r t; do
        [[ -z "${t}" ]] && continue
        [[ -n "${consulta}" ]] && consulta+=" UNION ALL "
        consulta+="SELECT '${t}' AS tabla, COUNT(*) AS registros FROM \`${t}\`"
    done < <(tablas_funcionales)
    mysql_root -t -e "SELECT tabla, registros FROM (${consulta}) x ORDER BY tabla;
                      SELECT COUNT(*) AS tablas, SUM(registros) AS total_registros,
                             IF(SUM(registros) = 500, 'OK: exactamente 500', 'REVISAR: no son 500') AS resultado
                        FROM (${consulta}) x;"

    info "ID autoincrementales: empiezan en 1 y AUTO_INCREMENT = máximo + 1:"
    local ids=""
    while IFS=$'\t' read -r t col ai; do
        [[ -z "${t}" ]] && continue
        [[ -n "${ids}" ]] && ids+=" UNION ALL "
        ids+="SELECT '${t}' AS tabla, MIN(\`${col}\`) AS id_min, MAX(\`${col}\`) AS id_max, COUNT(*) AS filas, ${ai} AS auto_increment FROM \`${t}\`"
    done < <(mysql_root --batch --skip-column-names -e "SET SESSION information_schema_stats_expiry = 0;
            SELECT c.TABLE_NAME, c.COLUMN_NAME, COALESCE(t.AUTO_INCREMENT, 1)
              FROM information_schema.COLUMNS c JOIN information_schema.TABLES t
                ON t.TABLE_SCHEMA = c.TABLE_SCHEMA AND t.TABLE_NAME = c.TABLE_NAME
             WHERE c.TABLE_SCHEMA = DATABASE() AND c.EXTRA LIKE '%auto_increment%'
               AND c.TABLE_NAME NOT LIKE 'vetlink\\_migracion%' ORDER BY c.TABLE_NAME" | tr -d '\r')
    mysql_root -t -e "SELECT COUNT(*) AS tablas_autoincrementales,
                             SUM(filas > 0 AND id_min <> 1) AS no_empiezan_en_1,
                             SUM(filas > 0 AND id_max <> filas) AS con_huecos,
                             SUM(auto_increment <> COALESCE(id_max, 0) + 1) AS auto_increment_desfasado
                        FROM (${ids}) x;"

    info "Comprobaciones de roles, mascotas, integridad y agenda:"
    mysql_root -t < "${VERIFICACION}"
    info "Comprobaciones generales del proyecto (database/verificar_consistencia.sql):"
    mysql_root -t < database/verificar_consistencia.sql
}

if (( SOLO_VERIFICAR )); then
    verificar
    exit 0
fi

if (( ! CONFIRMADO )); then
    echo
    echo "  Se borrarán TODOS los datos de la base de desarrollo y se cargarán 500 registros de prueba."
    (( RESET )) && echo "  Con --reset también se borra y recrea el volumen de MySQL (con respaldo previo)."
    echo
    read -r -p "  Escribe BORRAR para continuar: " respuesta
    [[ "${respuesta}" == "BORRAR" ]] || error "Cancelado. No se borró nada."
fi

# 1. Base desde cero (opcional).
if (( RESET )); then
    info "1/5 Recreando la base desde cero con scripts/reset-db.sh..."
    bash scripts/reset-db.sh --si --sin-levantar
    docker compose up -d --wait mysql
    info "Aplicando el esquema (servicio «migrate»)..."
    docker compose run --rm -T migrate
else
    info "1/5 Sin --reset: se reutiliza la estructura actual."
    docker compose up -d --wait mysql
fi
[[ "$(valor "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mascota'" | tr -d '\r')" == "1" ]] \
    || error "La base no tiene el esquema de VetLink. Ejecuta: bash scripts/migrate.sh"

# 2. Sin escrituras de la aplicación durante la carga.
info "2/5 Deteniendo Spring y FastAPI..."
docker compose stop spring fastapi >/dev/null 2>&1 || true

# 3. Vaciar y cargar.
info "3/5 Generando la semilla y cargándola..."
docker image inspect "${IMAGEN_PYTHON}" >/dev/null 2>&1 || docker pull -q "${IMAGEN_PYTHON}" >/dev/null
docker run --rm -i -e TZ=America/Bogota "${IMAGEN_PYTHON}" python - --resumen < "${GENERADOR}"
semilla_sql="$(mktemp)"
trap 'rm -f "${semilla_sql}"' EXIT
docker run --rm -i -e TZ=America/Bogota "${IMAGEN_PYTHON}" python - < "${GENERADOR}" > "${semilla_sql}"
{
    echo "SET FOREIGN_KEY_CHECKS = 0;"
    while IFS= read -r t; do
        [[ -n "${t}" ]] && echo "TRUNCATE TABLE \`${t}\`;"
    done < <(tablas_funcionales)
    echo "SET FOREIGN_KEY_CHECKS = 1;"
    cat "${semilla_sql}"
} | mysql_root
info "Semilla cargada."

# 4. Adjuntos (PDF) en el volumen de uploads, en las rutas que guarda archivo_historial.
info "4/5 Escribiendo los PDF adjuntos en el volumen de uploads..."
docker compose up --no-start spring >/dev/null 2>&1 || true
proyecto="${COMPOSE_PROJECT_NAME:-$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "$(docker compose ps -a -q mysql | head -n1)")}"
vol_uploads=$(docker volume ls -q --filter "label=com.docker.compose.project=${proyecto}" \
                                  --filter "label=com.docker.compose.volume=vetlink_uploads")
if [[ -n "${vol_uploads}" ]]; then
    docker run --rm -i -v "${vol_uploads}:/u" "${IMAGEN_PYTHON}" python - --adjuntos /u < "${GENERADOR}"
else
    printf '[semilla] AVISO: no se encontró el volumen de uploads; los PDF adjuntos no se escribieron.\n' >&2
fi

# 5. Levantar y verificar.
info "5/5 Levantando los servicios..."
docker compose up -d
verificar
info "Listo. Spring tarda ~1 minuto en arrancar: docker compose logs -f spring"
