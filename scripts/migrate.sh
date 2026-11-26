#!/usr/bin/env bash
# =====================================================================
# scripts/migrate.sh — crea o actualiza la base de datos de VetLink.
#
# Se puede ejecutar todas las veces que se quiera: solo aplica lo pendiente.
#
#   bash scripts/migrate.sh             aplica las migraciones pendientes
#   bash scripts/migrate.sh --estado    muestra aplicadas y pendientes, sin cambiar nada
#   bash scripts/migrate.sh --sin-respaldo   no hace el respaldo previo
#
# Dos modos:
#   * Desde el equipo (Git Bash, Linux, macOS): lanza el servicio «migrate» de Compose,
#     que corre este mismo script dentro de un contenedor con el cliente de MySQL.
#     No hace falta tener MySQL instalado.
#   * Dentro del contenedor (VETLINK_MIGRATE_EN_CONTENEDOR=1): hace el trabajo. Es lo que
#     ocurre automáticamente en cada «docker compose up», antes de arrancar Spring y FastAPI.
#
# Qué hace, en orden:
#   1. Valida variables de entorno y espera a que MySQL responda.
#   2. Toma un bloqueo en la base (dos ejecuciones simultáneas no se pisan).
#   3. Lee database/migraciones.txt y compara con el historial (tabla vetlink_migracion).
#      Una base creada antes de existir el historial se «adopta»: se registra lo que ya
#      tenía (consultas marcador) sin volver a ejecutarlo.
#   4. Respalda la base en respaldos/ si hay cambios pendientes y ya tenía datos.
#   5. Aplica cada migración pendiente en orden y la registra. Si una falla, se detiene
#      con código de salida 1 y Spring no arranca.
#   6. Crea o actualiza los usuarios vetlink_spring y vetlink_fast y sus permisos.
#   7. Ejecuta las comprobaciones de database/verificar_consistencia.sql.
#
# Códigos de salida: 0 = base lista; 1 = error (el mensaje dice cuál y qué hacer).
# =====================================================================
set -Eeuo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

info()  { printf '[migrate] %s\n' "$*"; }
aviso() { printf '[migrate] AVISO: %s\n' "$*" >&2; }
error() { printf '[migrate] ERROR: %s\n' "$*" >&2; exit 1; }

# ---------------------------------------------------------------------
# Modo equipo: delega en el servicio «migrate» de Compose.
# ---------------------------------------------------------------------
if [[ "${VETLINK_MIGRATE_EN_CONTENEDOR:-}" != "1" ]]; then
    cd "${RAIZ}"
    command -v docker >/dev/null 2>&1 || error "No se encontró «docker». Instala Docker Desktop o Docker Engine con el plugin Compose."
    docker compose version >/dev/null 2>&1 || error "«docker compose» no está disponible."
    [[ -f .env ]] || error "No existe el archivo .env en ${RAIZ}. Créalo como indica el README (sección «Variables de entorno»)."
    for var in MYSQL_DATABASE MYSQL_ROOT_PASSWORD MYSQL_SPRING_PASSWORD MYSQL_FAST_PASSWORD; do
        # Solo se comprueba que exista y no esté vacía; nunca se muestra su valor.
        grep -Eq "^${var}=.+" .env || error "Falta ${var} en .env (o está vacía)."
    done
    info "Ejecutando las migraciones en el contenedor «migrate» (inicia MySQL si hace falta)..."
    # -T: sin terminal interactiva (evita errores de TTY en Git Bash).
    exec docker compose run --rm -T migrate "$@"
fi

# ---------------------------------------------------------------------
# Modo contenedor.
# ---------------------------------------------------------------------
SOLO_ESTADO=0
RESPALDAR=1
for arg in "$@"; do
    case "${arg}" in
        --estado) SOLO_ESTADO=1 ;;
        --sin-respaldo) RESPALDAR=0 ;;
        -h|--help) sed -n '2,32p' "${BASH_SOURCE[0]}"; exit 0 ;;
        *) error "Opción desconocida: ${arg} (usa --estado, --sin-respaldo o --help)" ;;
    esac
done

DIR_DB="${VETLINK_DATABASE_DIR:-${RAIZ}/database}"
MANIFIESTO="${DIR_DB}/migraciones.txt"
DIR_RESPALDOS="${VETLINK_RESPALDOS_DIR:-${RAIZ}/respaldos}"
DB_HOST="${DB_HOST:-mysql}"
DB_PORT="${DB_PORT:-3306}"
ESPERA_MAX="${MIGRATE_TIMEOUT:-180}"

info "1/7 Comprobando requisitos y variables de entorno..."
for cmd in mysql mysqladmin mysqldump sha256sum; do
    command -v "${cmd}" >/dev/null 2>&1 || error "Falta el comando «${cmd}» en el contenedor."
done
[[ -f "${MANIFIESTO}" ]] || error "No se encontró ${MANIFIESTO}."
: "${MYSQL_DATABASE:?MYSQL_DATABASE no está definida}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD no está definida}"
: "${MYSQL_SPRING_PASSWORD:?MYSQL_SPRING_PASSWORD no está definida}"
: "${MYSQL_FAST_PASSWORD:?MYSQL_FAST_PASSWORD no está definida}"
# Estos valores se interpolan en sentencias SQL: se validan antes.
[[ "${MYSQL_DATABASE}" =~ ^[A-Za-z0-9_]+$ ]] || error "MYSQL_DATABASE solo admite letras, números y _."
[[ "${MYSQL_SPRING_PASSWORD}" =~ ^[a-fA-F0-9]{64}$ && "${MYSQL_FAST_PASSWORD}" =~ ^[a-fA-F0-9]{64}$ ]] \
    || error "MYSQL_SPRING_PASSWORD y MYSQL_FAST_PASSWORD deben tener 64 caracteres hexadecimales (openssl rand -hex 32)."
[[ "${DB_PORT}" =~ ^[0-9]+$ ]] || error "DB_PORT no es un número."

# La contraseña viaja por variable de entorno, no en los argumentos del proceso.
export MYSQL_PWD="${MYSQL_ROOT_PASSWORD}"
servidor() { mysql --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -uroot --default-character-set=utf8mb4 "$@"; }
base()     { servidor "${MYSQL_DATABASE}" "$@"; }
# </dev/null: dentro de los bucles «while read» el cliente no debe consumir la entrada.
valor()    { base --batch --skip-column-names -e "$1" </dev/null; }

info "2/7 Esperando a MySQL en ${DB_HOST}:${DB_PORT} (máximo ${ESPERA_MAX} s)..."
inicio=${SECONDS}
until mysqladmin --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -uroot ping --silent >/dev/null 2>&1 \
      && servidor -e "SELECT 1" >/dev/null 2>&1; do
    (( SECONDS - inicio >= ESPERA_MAX )) && error "MySQL no respondió en ${ESPERA_MAX} s. Revisa: docker compose logs mysql"
    sleep 2
done
servidor -e "CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\`"
info "MySQL disponible."

# ¿Qué tipo de base es? (antes de crear las tablas del historial)
tenia_historial=$(valor "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vetlink_migracion'")
tenia_tablas=$(valor "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME NOT LIKE 'vetlink\\_migracion%'")
ADOPTAR=0
if [[ "${tenia_historial}" == "0" && "${tenia_tablas}" != "0" ]]; then
    ADOPTAR=1
fi

if (( SOLO_ESTADO )) && [[ "${tenia_historial}" == "0" ]]; then
    : # En modo estado no se crea nada; se calcula igual qué está pendiente.
else
    base <<'SQL'
CREATE TABLE IF NOT EXISTS vetlink_migracion (
    id          VARCHAR(100) NOT NULL,
    archivo     VARCHAR(255) NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    checksum    CHAR(64)     NOT NULL,
    origen      VARCHAR(20)  NOT NULL COMMENT 'EJECUTADA o ADOPTADA (ya estaba en una base anterior al historial)',
    aplicada_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duracion_ms INT          NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Historial de migraciones (scripts/migrate.sh)';
CREATE TABLE IF NOT EXISTS vetlink_migracion_bloqueo (
    id          TINYINT      NOT NULL,
    propietario VARCHAR(150) NOT NULL,
    desde       DATETIME     NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Evita dos ejecuciones simultáneas de scripts/migrate.sh';
SQL
fi

# ---------------------------------------------------------------------
# Bloqueo: una fila con clave primaria fija. Un bloqueo de más de 30 minutos
# se considera abandonado (proceso interrumpido) y se libera.
# ---------------------------------------------------------------------
PROPIETARIO="${HOSTNAME:-migrate}-$$-$(date +%s)"
BLOQUEADO=0
liberar_bloqueo() {
    if (( BLOQUEADO )); then
        base -e "DELETE FROM vetlink_migracion_bloqueo WHERE id = 1 AND propietario = '${PROPIETARIO}'" >/dev/null 2>&1 || true
        BLOQUEADO=0
    fi
}
trap liberar_bloqueo EXIT
trap 'error "Interrumpido."' INT TERM

if (( ! SOLO_ESTADO )); then
    info "3/7 Tomando el bloqueo de migraciones..."
    inicio=${SECONDS}
    until base -e "DELETE FROM vetlink_migracion_bloqueo WHERE desde < NOW() - INTERVAL 30 MINUTE;
                   INSERT INTO vetlink_migracion_bloqueo (id, propietario, desde) VALUES (1, '${PROPIETARIO}', NOW());" >/dev/null 2>&1; do
        (( SECONDS - inicio >= ESPERA_MAX )) && error "Otra ejecución de migraciones tiene el bloqueo desde hace más de ${ESPERA_MAX} s. Si no hay ninguna en curso, espera 30 minutos o borra la fila de vetlink_migracion_bloqueo."
        info "Otra ejecución está migrando; esperando..."
        sleep 3
    done
    BLOQUEADO=1
    # Con el bloqueo tomado, se vuelve a mirar si había historial (otra ejecución pudo crearlo).
    if [[ "$(valor "SELECT COUNT(*) FROM vetlink_migracion")" != "0" ]]; then ADOPTAR=0; fi
fi

# ---------------------------------------------------------------------
# Plan: qué está aplicado, qué se adopta y qué falta.
# ---------------------------------------------------------------------
declare -A APLICADA=() CHECKSUM=()
if [[ "$(valor "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'vetlink_migracion'")" != "0" ]]; then
    while IFS=$'\t' read -r id suma; do
        [[ -n "${id}" ]] && APLICADA["${id}"]=1 && CHECKSUM["${id}"]="${suma}"
    done < <(valor "SELECT id, checksum FROM vetlink_migracion")
fi

recortar() { local s="$1"; s="${s#"${s%%[![:space:]]*}"}"; s="${s%"${s##*[![:space:]]}"}"; printf '%s' "${s}"; }

PENDIENTES=()        # "id|archivo|tipo"
ADOPTADAS=()
IDS_VISTOS=" "
info "Estado de las migraciones (database/migraciones.txt):"
while IFS='|' read -r c_id c_archivo c_tipo c_marcador; do
    c_id=$(recortar "${c_id}")
    [[ -z "${c_id}" || "${c_id}" == \#* ]] && continue
    c_archivo=$(recortar "${c_archivo}"); c_tipo=$(recortar "${c_tipo}"); c_marcador=$(recortar "${c_marcador}")
    [[ "${IDS_VISTOS}" == *" ${c_id} "* ]] && error "El id ${c_id} está repetido en migraciones.txt."
    IDS_VISTOS+="${c_id} "
    ruta="${DIR_DB}/${c_archivo}"
    [[ -f "${ruta}" ]] || error "No existe ${c_archivo} (declarado en migraciones.txt como ${c_id})."
    [[ "${c_tipo}" == "esquema" || "${c_tipo}" == "demo" ]] || error "Tipo desconocido «${c_tipo}» en ${c_id}."
    suma=$(sha256sum "${ruta}" | cut -d' ' -f1)

    if [[ -n "${APLICADA[${c_id}]:-}" ]]; then
        if [[ "${CHECKSUM[${c_id}]}" != "${suma}" ]]; then
            aviso "${c_id}: el archivo cambió después de aplicarse. No se vuelve a ejecutar; si el cambio es de esquema, crea una migración nueva."
        fi
        printf '           [aplicada ] %s\n' "${c_id}"
        continue
    fi
    if [[ "${c_tipo}" == "demo" && "${RUN_DEMO_SEED:-false}" != "true" ]]; then
        printf '           [omitida  ] %s (datos demo; RUN_DEMO_SEED no es true)\n' "${c_id}"
        continue
    fi
    if (( ADOPTAR )) && [[ -n "${c_marcador}" ]] && [[ "$(valor "${c_marcador}" 2>/dev/null || echo 0)" == "1" ]]; then
        printf '           [adoptada ] %s (ya estaba en la base)\n' "${c_id}"
        ADOPTADAS+=("${c_id}|${c_archivo}|${c_tipo}|${suma}")
        continue
    fi
    printf '           [pendiente] %s\n' "${c_id}"
    PENDIENTES+=("${c_id}|${c_archivo}|${c_tipo}")
done < "${MANIFIESTO}"

if (( SOLO_ESTADO )); then
    info "${#PENDIENTES[@]} pendiente(s). Modo --estado: no se cambió nada."
    exit 0
fi

if (( ADOPTAR )); then
    info "Base creada antes del historial: se registran ${#ADOPTADAS[@]} migraciones que ya tenía."
    for fila in "${ADOPTADAS[@]}"; do
        IFS='|' read -r a_id a_archivo a_tipo a_suma <<<"${fila}"
        base -e "INSERT INTO vetlink_migracion (id, archivo, tipo, checksum, origen) VALUES ('${a_id}', '${a_archivo}', '${a_tipo}', '${a_suma}', 'ADOPTADA')"
    done
fi

# ---------------------------------------------------------------------
# Respaldo previo (solo si hay algo que aplicar y la base ya tenía datos).
# ---------------------------------------------------------------------
if (( ${#PENDIENTES[@]} > 0 )) && [[ "${tenia_tablas}" != "0" ]]; then
    if (( RESPALDAR )) && mkdir -p "${DIR_RESPALDOS}" 2>/dev/null && [[ -w "${DIR_RESPALDOS}" ]]; then
        respaldo="${DIR_RESPALDOS}/${MYSQL_DATABASE}-antes-de-migrar-$(date +%Y%m%d-%H%M%S).sql"
        info "4/7 Respaldando la base en respaldos/$(basename "${respaldo}")..."
        mysqldump --protocol=TCP -h "${DB_HOST}" -P "${DB_PORT}" -uroot --single-transaction --routines \
            --default-character-set=utf8mb4 "${MYSQL_DATABASE}" > "${respaldo}" \
            || error "No se pudo hacer el respaldo; no se aplicó ninguna migración."
        [[ -s "${respaldo}" ]] || error "El respaldo quedó vacío; no se aplicó ninguna migración."
    else
        aviso "4/7 Sin respaldo previo (--sin-respaldo o carpeta de respaldos no disponible)."
    fi
else
    info "4/7 Respaldo no necesario."
fi

# ---------------------------------------------------------------------
# Aplicar pendientes.
# ---------------------------------------------------------------------
if (( ${#PENDIENTES[@]} == 0 )); then
    info "5/7 No hay migraciones pendientes."
else
    info "5/7 Aplicando ${#PENDIENTES[@]} migración(es)..."
fi
for fila in "${PENDIENTES[@]}"; do
    IFS='|' read -r p_id p_archivo p_tipo <<<"${fila}"
    ruta="${DIR_DB}/${p_archivo}"
    suma=$(sha256sum "${ruta}" | cut -d' ' -f1)
    info "    -> ${p_id} (${p_archivo})"
    t0=$(date +%s%3N)
    if ! base < "${ruta}"; then
        error "Falló ${p_id} (${p_archivo}). No se registró y las siguientes no se aplicaron.
         MySQL no revierte los cambios de estructura (DDL): revisa el mensaje anterior.
         Base de desarrollo: bash scripts/reset-db.sh la recrea desde cero.
         Base con datos: restaura el respaldo de respaldos/ (ver README, «Restaurar un respaldo»)."
    fi
    t1=$(date +%s%3N)
    base -e "INSERT INTO vetlink_migracion (id, archivo, tipo, checksum, origen, duracion_ms)
             VALUES ('${p_id}', '${p_archivo}', '${p_tipo}', '${suma}', 'EJECUTADA', $(( t1 - t0 )))"
done

# ---------------------------------------------------------------------
# Usuarios de aplicación y permisos mínimos (idempotente; se repite siempre
# para que las tablas nuevas también queden con permisos).
# ---------------------------------------------------------------------
info "6/7 Usuarios de aplicación y permisos..."
servidor -e "
    CREATE USER IF NOT EXISTS 'vetlink_spring'@'%' IDENTIFIED BY '${MYSQL_SPRING_PASSWORD}';
    CREATE USER IF NOT EXISTS 'vetlink_fast'@'%' IDENTIFIED BY '${MYSQL_FAST_PASSWORD}';
    ALTER USER 'vetlink_spring'@'%' IDENTIFIED BY '${MYSQL_SPRING_PASSWORD}';
    ALTER USER 'vetlink_fast'@'%' IDENTIFIED BY '${MYSQL_FAST_PASSWORD}';"

# Spring: DML sobre las tablas principales. Excluidas las de FastAPI y las del historial.
grants_spring=""
while IFS= read -r tabla; do
    [[ -z "${tabla}" ]] && continue
    grants_spring+="GRANT SELECT, INSERT, UPDATE, DELETE ON \`${MYSQL_DATABASE}\`.\`${tabla}\` TO 'vetlink_spring'@'%';"$'\n'
done < <(valor "SELECT TABLE_NAME FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
                  AND TABLE_NAME NOT IN ('pasaporte', 'pasaporte_acceso', 'reporte_generado')
                  AND TABLE_NAME NOT LIKE 'vetlink\\_migracion%'
                ORDER BY TABLE_NAME")
[[ -n "${grants_spring}" ]] || error "No hay tablas para dar permisos a vetlink_spring."
base <<<"${grants_spring}"

# FastAPI: pasaporte (permisos_fastapi.sql) y reportes PDF (V8_permisos_fastapi.sql).
base < "${DIR_DB}/permisos_fastapi.sql" || error "Falló database/permisos_fastapi.sql."
base < "${DIR_DB}/migrations/V8_permisos_fastapi.sql" || error "Falló migrations/V8_permisos_fastapi.sql."

# ---------------------------------------------------------------------
# Comprobaciones de consistencia (solo lectura; avisan, no detienen).
# ---------------------------------------------------------------------
info "7/7 Comprobaciones de consistencia:"
problemas=0
while IFS=$'\t' read -r nombre cantidad; do
    [[ -z "${nombre}" ]] && continue
    if [[ "${nombre}" == "(info)"* || "${cantidad}" == "0" ]]; then
        printf '           %-70s %s\n' "${nombre}" "${cantidad}"
    else
        printf '           %-70s %s  <-- revisar\n' "${nombre}" "${cantidad}"
        problemas=$(( problemas + 1 ))
    fi
done < <(base --batch --skip-column-names < "${DIR_DB}/verificar_consistencia.sql")
(( problemas == 0 )) || aviso "${problemas} comprobación(es) con resultados distintos de 0 (ver database/verificar_consistencia.sql)."

aplicadas=$(valor "SELECT COUNT(*) FROM vetlink_migracion")
info "Base «${MYSQL_DATABASE}» lista: ${aplicadas} migraciones registradas, ${#PENDIENTES[@]} aplicadas en esta ejecución."
