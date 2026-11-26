#!/usr/bin/env bash
# =====================================================================
# scripts/reset-db.sh — BORRA la base de datos de DESARROLLO y la crea de cero.
#
# ACCIÓN DESTRUCTIVA Y EXPLÍCITA. Nunca forma parte del arranque ni de las
# actualizaciones normales (para eso está scripts/migrate.sh, que no borra nada).
#
#   bash scripts/reset-db.sh                    pide confirmación escribiendo BORRAR
#   bash scripts/reset-db.sh --si               sin preguntar (para scripts)
#   bash scripts/reset-db.sh --incluir-uploads  borra también fotos y adjuntos subidos
#   bash scripts/reset-db.sh --sin-respaldo     no respalda antes de borrar
#   bash scripts/reset-db.sh --sin-levantar     borra pero no vuelve a levantar los servicios
#
# Qué hace:
#   1. Identifica el proyecto de Compose y SU volumen de MySQL (etiquetas de Compose):
#      nunca toca volúmenes de otros proyectos.
#   2. Guarda un respaldo completo en respaldos/ (enciende MySQL si estaba apagado).
#      Si no puede respaldar, se detiene sin borrar nada.
#   3. Detiene los servicios, borra el volumen de MySQL (y el de uploads si se pide).
#   4. Levanta todo de nuevo: el servicio «migrate» crea la base desde cero.
# Qué se pierde: TODOS los datos de la base (usuarios, mascotas, citas, historias...).
# Las fotos y adjuntos (volumen de uploads) se conservan salvo con --incluir-uploads.
# =====================================================================
set -Eeuo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${RAIZ}"

info()  { printf '[reset-db] %s\n' "$*"; }
error() { printf '[reset-db] ERROR: %s\n' "$*" >&2; exit 1; }

CONFIRMADO=0; UPLOADS=0; RESPALDAR=1; LEVANTAR=1
for arg in "$@"; do
    case "${arg}" in
        --si) CONFIRMADO=1 ;;
        --incluir-uploads) UPLOADS=1 ;;
        --sin-respaldo) RESPALDAR=0 ;;
        --sin-levantar) LEVANTAR=0 ;;
        -h|--help) sed -n '2,25p' "${BASH_SOURCE[0]}"; exit 0 ;;
        *) error "Opción desconocida: ${arg}" ;;
    esac
done

command -v docker >/dev/null 2>&1 || error "No se encontró «docker»."
docker compose version >/dev/null 2>&1 || error "«docker compose» no está disponible."
[[ -f compose.yaml ]] || error "No se encontró compose.yaml en ${RAIZ}."
[[ -f .env ]] || error "No existe .env: sin él no se puede volver a crear la base."

# Solo entornos de desarrollo: en producción no se usa este script.
if grep -Eq '^VETLINK_ENTORNO=produccion' .env; then
    error "VETLINK_ENTORNO=produccion en .env: este script es solo para desarrollo."
fi

# 1. Proyecto de Compose: el de los contenedores existentes o, si no hay, el nombre por defecto.
proyecto="${COMPOSE_PROJECT_NAME:-}"
if [[ -z "${proyecto}" ]]; then
    id_mysql=$(docker compose ps -a -q mysql 2>/dev/null | head -n1 || true)
    if [[ -n "${id_mysql}" ]]; then
        proyecto=$(docker inspect -f '{{ index .Config.Labels "com.docker.compose.project" }}' "${id_mysql}")
    else
        proyecto=$(basename "${RAIZ}" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9_-')
    fi
fi
volumen_de() {
    docker volume ls -q --filter "label=com.docker.compose.project=${proyecto}" \
                        --filter "label=com.docker.compose.volume=$1"
}
vol_mysql=$(volumen_de mysql_data)
vol_uploads=$(volumen_de vetlink_uploads)

info "Proyecto de Compose: ${proyecto}"
info "Volumen de MySQL que se borrará: ${vol_mysql:-<no existe>}"
if (( UPLOADS )); then
    info "Volumen de uploads que se borrará: ${vol_uploads:-<no existe>}"
else
    info "Volumen de uploads (fotos y adjuntos): se conserva."
fi

if (( ! CONFIRMADO )); then
    echo
    echo "  Se borrarán TODOS los datos de la base de desarrollo del proyecto «${proyecto}»."
    (( UPLOADS )) && echo "  También se borrarán las fotos y archivos adjuntos subidos."
    echo
    read -r -p "  Escribe BORRAR para continuar: " respuesta
    [[ "${respuesta}" == "BORRAR" ]] || error "Cancelado. No se borró nada."
fi

# 2. Respaldo. Nunca se omite en silencio: si no se puede hacer, se detiene sin borrar.
if (( ! RESPALDAR )); then
    info "Sin respaldo (--sin-respaldo)."
elif [[ -z "${vol_mysql}" ]]; then
    info "No hay volumen de MySQL: nada que respaldar."
else
    if ! docker compose exec -T mysql true >/dev/null 2>&1; then
        info "MySQL está apagado; encendiéndolo para respaldar..."
        docker compose up -d --wait mysql >/dev/null \
            || error "No se pudo encender MySQL para respaldar. No se borró nada (usa --sin-respaldo si no lo necesitas)."
    fi
    mkdir -p respaldos
    respaldo="respaldos/antes-de-reset-$(date +%Y%m%d-%H%M%S).sql"
    info "Respaldando la base en ${respaldo}..."
    docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump --single-transaction --routines -uroot "$MYSQL_DATABASE"' > "${respaldo}" \
        || error "No se pudo respaldar. No se borró nada (usa --sin-respaldo si de verdad no lo necesitas)."
    [[ -s "${respaldo}" ]] || error "El respaldo quedó vacío. No se borró nada."
    info "Respaldo guardado: ${respaldo} ($(wc -c < "${respaldo}") bytes)."
fi

# 3. Detener y borrar.
info "Deteniendo los servicios..."
docker compose down --remove-orphans
if [[ -n "${vol_mysql}" ]]; then
    info "Borrando ${vol_mysql}..."
    docker volume rm "${vol_mysql}"
fi
if (( UPLOADS )) && [[ -n "${vol_uploads}" ]]; then
    info "Borrando ${vol_uploads}..."
    docker volume rm "${vol_uploads}"
fi

# 4. Recrear.
if (( ! LEVANTAR )); then
    info "Listo (--sin-levantar). Para crear la base de cero: docker compose up -d --build"
    exit 0
fi
info "Levantando de nuevo (la base se crea desde cero con las migraciones)..."
docker compose up -d --build
info "Listo. Sigue el progreso con: docker compose logs -f migrate spring"
